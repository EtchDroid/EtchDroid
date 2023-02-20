/*
 * (C) Copyright 2014-2019 magnusja <github@mgns.tech>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.depau.etchdroid.libaums_wrapper

import android.content.Context
import android.hardware.usb.*
import android.util.Log
import me.jahnen.libaums.core.UsbMassStorageDevice
import me.jahnen.libaums.core.driver.BlockDeviceDriver
import me.jahnen.libaums.core.driver.BlockDeviceDriverFactory
import me.jahnen.libaums.core.driver.scsi.commands.sense.MediaNotInserted
import me.jahnen.libaums.core.partition.Partition
import me.jahnen.libaums.core.partition.PartitionTable
import me.jahnen.libaums.core.partition.PartitionTableFactory
import me.jahnen.libaums.core.usb.UsbCommunication
import me.jahnen.libaums.core.usb.UsbCommunicationFactory
import java.io.IOException
import java.util.*

/**
 * Modified version of UsbMassStorageDevice from Libaums for EtchDroid.
 *
 * ---
 *
 * Class representing a connected USB mass storage device. You can enumerate
 * through all connected mass storage devices via
 * [.getMassStorageDevices]. This method only returns supported
 * devices or if no device is connected an empty array.
 *
 *
 * After choosing a device you have to get the permission for the underlying
 * [android.hardware.usb.UsbDevice]. The underlying
 * [android.hardware.usb.UsbDevice] can be accessed via
 * [.getUsbDevice].
 *
 *
 * After that you need to call [.setupDevice]. This will initialize the
 * mass storage device and read the partitions (
 * [com.github.mjdev.libaums.partition.Partition]).
 *
 *
 * The supported partitions can then be accessed via [.getPartitions]
 * and you can begin to read directories and files.
 *
 * @author mjahnen
 */
class EtchDroidUsbMassStorageDevice
/**
 * Construct a new [com.github.mjdev.libaums.UsbMassStorageDevice].
 * The given parameters have to actually be a mass storage device, this is
 * not checked in the constructor!
 *
 * @param usbManager
 * @param usbDevice
 * @param usbInterface
 * @param inEndpoint
 * @param outEndpoint
 */
private constructor(
    private val usbManager: UsbManager,
    /**
     * This returns the [android.hardware.usb.UsbDevice] which can be used
     * to request permission for communication.
     *
     * @return Underlying [android.hardware.usb.UsbDevice] used for
     * communication.
     */
    val usbDevice: UsbDevice,
    private val usbInterface: UsbInterface,
    private val inEndpoint: UsbEndpoint,
    private val outEndpoint: UsbEndpoint
) {
    private lateinit var usbCommunication: UsbCommunication
    lateinit var blockDevices: Map<Int, BlockDeviceDriver>

    /**
     * Returns the block device interface for this device.
     *
     * Only use this if you know what you are doing, for a interacting (listing/reading/writing files)
     * with a pen drive this is usually not needed
     *
     * @return The BlockDeviceDriver implementation
     */
    val partitions = ArrayList<Partition>()

    // TODO this is never used, should we only allow one init() call?
    private var inited = false
    private var partitionsInited = false

    /**
     * Initializes the mass storage device and determines different things like
     * for example the MBR or the file systems for the different partitions.
     *
     * @throws IOException
     * If reading from the physical device fails.
     * @throws IllegalStateException
     * If permission to communicate with the underlying
     * [UsbDevice] is missing.
     * @see .getUsbDevice
     */
    @Throws(IOException::class)
    fun init() {
        if (inited) {
            throw java.lang.IllegalStateException("Mass storage device already initialized")
        }

        if (usbManager.hasPermission(usbDevice))
            setupDevice()
        else
            throw IllegalStateException("Missing permission to access usb device: $usbDevice")

        inited = true
    }

    /**
     * Sets the device up. Claims interface and initiates the device connection.
     * Chooses the right[UsbCommunication]
     * depending on the Android version (
     * [com.github.mjdev.libaums.usb.HoneyCombMr1Communication]
     * or (
     * [com.github.mjdev.libaums.usb.JellyBeanMr2Communication]
     * ).
     *
     *
     * Initializes the [.blockDevice] and reads the partitions.
     *
     * @throws IOException
     * If reading from the physical device fails.
     * @see .init
     */
    @Throws(IOException::class)
    private fun setupDevice() {
        usbCommunication = UsbCommunicationFactory
            .createUsbCommunication(usbManager, usbDevice, usbInterface, outEndpoint, inEndpoint)
        val maxLun = ByteArray(1)
        usbCommunication.controlTransfer(161, 254, 0, usbInterface.id, maxLun, 1)

        Log.i(TAG, "MAX LUN " + maxLun[0].toInt())

        val mutableBlockDevices = mutableMapOf<Int, BlockDeviceDriver>()

        for (lun in 0..maxLun[0]) {
            val blockDevice =
                BlockDeviceDriverFactory.createBlockDevice(usbCommunication, lun = lun.toByte())
            try {
                blockDevice.init()
                mutableBlockDevices[lun] = blockDevice
            } catch (e: MediaNotInserted) {
                // This LUN does not have media inserted. Ignore it.
                continue
            }
        }

        blockDevices = mutableBlockDevices.toMap()
    }

    fun initPartitions() {
        if (!inited)
            throw java.lang.IllegalStateException("Block devices not initialized")

        if (partitionsInited)
            return
        partitionsInited = true

        blockDevices.forEach {
            val blockDevice = it.value

            val partitionTable = PartitionTableFactory.createPartitionTable(blockDevice)
            val partitions = initPartitions(partitionTable, blockDevice)

            this.partitions.addAll(partitions)
        }
    }

    /**
     * Fills [.partitions] with the information received by the
     * [.partitionTable].
     *
     * @throws IOException
     * If reading from the [.blockDevice] fails.
     */
    @Throws(IOException::class)
    private fun initPartitions(
        partitionTable: PartitionTable,
        blockDevice: BlockDeviceDriver
    ) = partitionTable.partitionTableEntries.mapNotNull {
        Partition.createPartition(it, blockDevice)
    }

    /**
     * Releases the [android.hardware.usb.UsbInterface] and closes the
     * [android.hardware.usb.UsbDeviceConnection]. After calling this
     * method no further communication is possible. That means you can not read
     * or write from or to the partitions returned by [.getPartitions].
     */
    fun close() {
        usbCommunication.close()
        inited = false
    }

    companion object {

        private val TAG = UsbMassStorageDevice::class.java.simpleName

        /**
         * subclass 6 means that the usb mass storage device implements the SCSI
         * transparent command set
         */
        private const val INTERFACE_SUBCLASS = 6

        /**
         * protocol 80 means the communication happens only via bulk transfers
         */
        private const val INTERFACE_PROTOCOL = 80


        @JvmStatic
        fun UsbDevice.getMassStorageDevices(context: Context): List<EtchDroidUsbMassStorageDevice> {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

            return (0 until interfaceCount)
                .map { i ->
                    val usbInterface = getInterface(i)
                    Log.i(TAG, "found usb interface: $usbInterface")
                    usbInterface
                }
                .filter { usbInterface ->
                    // we currently only support SCSI transparent command set with
                    // bulk transfers only!
                    !(usbInterface.interfaceClass != UsbConstants.USB_CLASS_MASS_STORAGE
                            || usbInterface.interfaceSubclass != INTERFACE_SUBCLASS
                            || usbInterface.interfaceProtocol != INTERFACE_PROTOCOL)
                }
                .map { usbInterface ->
                    // Every mass storage device has exactly two endpoints
                    // One IN and one OUT endpoint
                    val endpointCount = usbInterface.endpointCount
                    if (endpointCount != 2) {
                        Log.w(TAG, "inteface endpoint count != 2")
                    }

                    var outEndpoint: UsbEndpoint? = null
                    var inEndpoint: UsbEndpoint? = null

                    for (j in 0 until endpointCount) {
                        val endpoint = usbInterface.getEndpoint(j)
                        Log.i(TAG, "found usb endpoint: $endpoint")
                        if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (endpoint.direction == UsbConstants.USB_DIR_OUT) {
                                outEndpoint = endpoint
                            } else {
                                inEndpoint = endpoint
                            }
                        }
                    }

                    if (outEndpoint == null || inEndpoint == null) {
                        Log.e(TAG, "Not all needed endpoints found!")
                        return@map null
                    }

                    return@map EtchDroidUsbMassStorageDevice(
                        usbManager, this, usbInterface, inEndpoint, outEndpoint
                    )
                }
                .filterNotNull()
        }

        /**
         * This method iterates through all connected USB devices and searches for
         * mass storage devices.
         *
         * @param context
         * Context to get the [UsbManager]
         * @return An array of suitable mass storage devices or an empty array if
         * none could be found.
         */
        @JvmStatic
        fun getMassStorageDevices(context: Context): Array<EtchDroidUsbMassStorageDevice> {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

            return usbManager.deviceList
                .map {
                    val device = it.value
                    Log.i(TAG, "found usb device: $it")
                    device.getMassStorageDevices(context)
                }
                .flatten()
                .toTypedArray()
        }
    }
}
