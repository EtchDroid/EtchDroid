package eu.depau.etchdroid.ui

import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import eu.depau.etchdroid.AppSettings
import eu.depau.etchdroid.Intents
import eu.depau.etchdroid.JobStatusInfo
import eu.depau.etchdroid.SettingChangeListener
import eu.depau.etchdroid.ThemeMode
import eu.depau.etchdroid.massstorage.EtchDroidUsbMassStorageDevice.Companion.massStorageDevices
import eu.depau.etchdroid.massstorage.UsbMassStorageDeviceDescriptor
import eu.depau.etchdroid.utils.exception.base.EtchDroidException
import eu.depau.etchdroid.utils.exception.base.RecoverableException
import eu.depau.etchdroid.utils.ktexts.safeParcelableExtra
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


interface IThemeState {
    val dynamicColors: Boolean
    val themeMode: ThemeMode
}

interface IViewModel<T> {
    val state: StateFlow<T>
}

interface ThemeViewModel<T> : IViewModel<T> where T : IThemeState {
    val darkMode: State<Boolean>
        @Composable get() {
            val stateValue by state.collectAsState()
            val systemInDarkMode = isSystemInDarkTheme()
            return remember(stateValue.themeMode) {
                derivedStateOf {
                    when (stateValue.themeMode) {
                        ThemeMode.SYSTEM -> systemInDarkMode
                        ThemeMode.DARK -> true
                        ThemeMode.LIGHT -> false
                    }
                }
            }
        }
}

data class MainActivityState(
    override val dynamicColors: Boolean = false,
    override val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showWindowsAlertForUri: Uri? = null,
    val openedImage: Uri? = null,
    val massStorageDevices: Set<UsbMassStorageDeviceDescriptor> = emptySet(),
    val selectedDevice: UsbMassStorageDeviceDescriptor? = null,
    val hasUsbPermission: Boolean = false,
) : IThemeState {
    companion object {
        val Empty: MainActivityState
            get() = MainActivityState()
    }
}

class MainActivityViewModel : ViewModel(), SettingChangeListener,
    ThemeViewModel<MainActivityState> {
    private val _state = MutableStateFlow(MainActivityState.Empty)
    override val state: StateFlow<MainActivityState> = _state.asStateFlow()

    override fun refreshSettings(settings: AppSettings) {
        _state.update {
            it.copy(
                dynamicColors = settings.dynamicColors, themeMode = settings.themeMode
            )
        }
    }

    fun setState(state: MainActivityState) {
        _state.update { state }
    }

    fun setShowWindowsAlertUri(uri: Uri?) {
        _state.update {
            it.copy(showWindowsAlertForUri = uri)
        }
    }

    fun setOpenedImage(uri: Uri?) {
        _state.update {
            it.copy(openedImage = uri)
        }
    }

    fun selectDevice(context: Context, device: UsbMassStorageDeviceDescriptor?) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val hasPermission = device?.usbDevice?.let { usbManager.hasPermission(it) } ?: false
        _state.update {
            val new = if (it.selectedDevice == device) it
            else it.copy(selectedDevice = device, hasUsbPermission = hasPermission)
            // println("selectDevice: $new")
            new
        }
    }

    fun usbDeviceAttached(device: UsbDevice) {
        _state.update {
            val new = it.copy(
                massStorageDevices = it.massStorageDevices + device.massStorageDevices
            )
            // println("usbDeviceAttached: $new")
            new
        }
    }

    fun usbDeviceDetached(device: UsbDevice) {
        _state.update { state ->
            val newUsbDevices = state.massStorageDevices.filter { it.usbDevice != device }.toSet()
            val new = if (state.selectedDevice?.usbDevice == device) state.copy(
                massStorageDevices = newUsbDevices, selectedDevice = newUsbDevices.firstOrNull(),
                hasUsbPermission = false
            )
            else state.copy(massStorageDevices = newUsbDevices)
            // println("usbDeviceDetached: $new")
            new
        }
    }

    fun usbPermissionGranted(device: UsbDevice) {
        _state.update {
            val new = if (it.selectedDevice?.usbDevice == device) {
                println("Permission granted for selected device")
                it.copy(hasUsbPermission = true)
            } else if (device.massStorageDevices.size == 1) {
                it.copy(
                    selectedDevice = device.massStorageDevices.first(), hasUsbPermission = true
                )
            } else {
                it.copy(hasUsbPermission = false)
            }
            // println("usbPermissionGranted: $new")
            new
        }
    }

    fun replaceUsbDevices(devices: Collection<UsbDevice>) {
        _state.update { oldState ->
            val newDevices = devices.flatMap { it.massStorageDevices }.toSet()
            val new = if (oldState.selectedDevice?.usbDevice in devices) {
                oldState.copy(massStorageDevices = newDevices)
            } else {
                oldState.copy(
                    massStorageDevices = newDevices, selectedDevice = newDevices.firstOrNull()
                )
            }
            // println("replaceUsbDevices: $new")
            new
        }
    }

    override fun toString(): String {
        return "MainActivityViewModel(state=${_state.value})"
    }
}

enum class JobState {
    IN_PROGRESS, SUCCESS, FATAL_ERROR, RECOVERABLE_ERROR
}

data class ProgressActivityState(
    override val dynamicColors: Boolean = false,
    override val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val jobState: JobState = JobState.IN_PROGRESS,
    val percent: Int = -1,
    val speed: Float = 0f,
    val processedBytes: Long = 0,
    val totalBytes: Long = 0,
    val jobId: Int = -1,
    val isVerifying: Boolean = false,
    val sourceUri: Uri? = null,
    val destDevice: UsbMassStorageDeviceDescriptor? = null,
    val exception: EtchDroidException? = null,
    val recoverableError: Boolean = false,
    val notificationsPermission: Boolean = false,
    val showNotificationsBanner: Boolean = true,
) : IThemeState {
    companion object {
        val Empty: ProgressActivityState
            get() = ProgressActivityState()
    }
}

class ProgressActivityViewModel : ViewModel(), SettingChangeListener,
    ThemeViewModel<ProgressActivityState> {
    private val _state = MutableStateFlow(ProgressActivityState.Empty)
    override val state: StateFlow<ProgressActivityState> = _state.asStateFlow()

    override fun refreshSettings(settings: AppSettings) {
        _state.update {
            it.copy(
                dynamicColors = settings.dynamicColors, themeMode = settings.themeMode,
                showNotificationsBanner = settings.showNotificationsBanner
            )
        }
    }

    fun setState(state: ProgressActivityState) {
        _state.update { state }
    }

    fun updateFromIntent(intent: Intent) {
        val sourceUri = intent.safeParcelableExtra<Uri>("sourceUri")!!
        val status = intent.safeParcelableExtra<JobStatusInfo>("status")!!

        when (intent.action) {
            Intents.JOB_PROGRESS -> {
                _state.update {
                    it.copy(
                        jobState = JobState.IN_PROGRESS,
                        jobId = status.jobId,
                        isVerifying = status.isVerifying,
                        percent = status.percent,
                        speed = status.speed,
                        processedBytes = status.processedBytes,
                        totalBytes = status.totalBytes,
                        sourceUri = sourceUri,
                        destDevice = status.destDevice,
                        exception = null,
                    )
                }
            }

            Intents.FINISHED -> {
                _state.update {
                    it.copy(
                        jobState = JobState.SUCCESS,
                        exception = null,
                        sourceUri = sourceUri,
                        destDevice = status.destDevice,
                        totalBytes = status.totalBytes,
                        percent = 100,
                    )
                }
            }

            Intents.ERROR -> {
                _state.update {
                    it.copy(
                        jobState = if (status.exception is RecoverableException) JobState.RECOVERABLE_ERROR else JobState.FATAL_ERROR,
                        jobId = status.jobId,
                        exception = status.exception,
                        sourceUri = sourceUri,
                        destDevice = status.destDevice,
                        processedBytes = status.processedBytes,
                        totalBytes = status.totalBytes,
                        percent = status.percent,
                    )
                }
            }
        }
    }

    fun setNotificationsPermission(permission: Boolean) {
        _state.update {
            it.copy(notificationsPermission = permission)
        }
    }
}