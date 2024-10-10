package eu.depau.etchdroid.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ArrowDownward
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import eu.depau.etchdroid.AppSettings
import eu.depau.etchdroid.Intents
import eu.depau.etchdroid.R
import eu.depau.etchdroid.getProgressUpdateIntent
import eu.depau.etchdroid.getStartJobIntent
import eu.depau.etchdroid.massstorage.PreviewUsbDevice
import eu.depau.etchdroid.massstorage.UsbMassStorageDeviceDescriptor
import eu.depau.etchdroid.service.WorkerService
import eu.depau.etchdroid.ui.composables.MainView
import eu.depau.etchdroid.ui.utils.rememberPorkedAroundSheetState
import eu.depau.etchdroid.utils.broadcastReceiver
import eu.depau.etchdroid.utils.ktexts.getFileName
import eu.depau.etchdroid.utils.ktexts.getFilePath
import eu.depau.etchdroid.utils.ktexts.getFileSize
import eu.depau.etchdroid.utils.ktexts.registerExportedReceiver
import eu.depau.etchdroid.utils.ktexts.safeParcelableExtra
import eu.depau.etchdroid.utils.ktexts.startForegroundServiceCompat
import eu.depau.etchdroid.utils.ktexts.toHRSize
import eu.depau.etchdroid.utils.ktexts.toast
import eu.depau.etchdroid.utils.ktexts.usbDevice
import kotlin.random.Random

private const val TAG = "ConfirmOperationActivit"

class ConfirmOperationActivity : ComponentActivity() {
    private val mViewModel: ConfirmOperationActivityViewModel by viewModels()
    private lateinit var mSettings: AppSettings
    private lateinit var mUsbPermissionIntent: PendingIntent


    private val mUsbDevicesReceiver = broadcastReceiver { intent ->
        val usbDevice: UsbDevice? = if (intent.usbDevice == null) {
            Log.w(TAG, "Received USB broadcast without device, using selected device: $intent")
            mViewModel.state.value.selectedDevice?.usbDevice
        } else {
            intent.usbDevice
        }
        if (usbDevice == null) {
            Log.w(
                TAG,
                "Received USB broadcast without device and no selected device, ignoring: $intent"
            )
            return@broadcastReceiver
        }

        Log.d(TAG, "Received USB broadcast: ${intent.action}, device: ${intent.usbDevice}")

        when (intent.action) {
            Intents.USB_PERMISSION -> {
                // Since we're using an immutable PendingIntent as recommended by the latest API
                // we won't receive a USB device or grant status; we need to check back with the
                // USB manager
                val usbManager = getSystemService(USB_SERVICE) as UsbManager
                val granted = usbManager.hasPermission(usbDevice)

                Log.d(TAG, "USB permission granted: $granted")
                mViewModel.setPermission(granted)

                if (!granted) {
                    toast(
                        getString(
                            R.string.permission_denied_for_usb_device, usbDevice.deviceName
                        )
                    )
                }
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                if (usbDevice == mViewModel.state.value.selectedDevice?.usbDevice) {
                    toast(getString(R.string.usb_device_was_unplugged))
                    finish()
                }
            }

            else -> {
                Log.w(TAG, "Received unknown broadcast: ${intent.action}")
            }
        }
    }

    private fun registerUsbReceiver() {
        val usbPermissionFilter = IntentFilter(Intents.USB_PERMISSION)
        val usbDetachedFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)

        registerExportedReceiver(mUsbDevicesReceiver, usbPermissionFilter)
        registerExportedReceiver(mUsbDevicesReceiver, usbDetachedFilter)
    }

    private fun unregisterUsbReceiver() {
        unregisterReceiver(mUsbDevicesReceiver)
    }

    override fun onStart() {
        super.onStart()
        registerUsbReceiver()
    }

    override fun onStop() {
        super.onStop()
        unregisterUsbReceiver()
    }

    private fun writeImage(uri: Uri, device: UsbMassStorageDeviceDescriptor) {
        val jobId = Random.nextInt()
        val intent =
            getStartJobIntent(uri, device, jobId, 0, false, this, WorkerService::class.java)
        Log.d(TAG, "Starting service with intent: $intent")
        startForegroundServiceCompat(intent)
        startActivity(
            getProgressUpdateIntent(
                uri, device, jobId, 0f, 0, 0, false, this, ProgressActivity::class.java
            )
        )
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val openedImage = intent.safeParcelableExtra<Uri>("sourceUri") ?: run {
            Log.e(TAG, "No source image URI provided")
            toast(getString(R.string.no_image_uri_provided))
            finish()
            return
        }
        val selectedDevice =
            intent.safeParcelableExtra<UsbMassStorageDeviceDescriptor>("destDevice") ?: run {
                Log.e(TAG, "No destination device selected")
                toast(getString(R.string.no_destination_device_selected))
                finish()
                return
            }
        mViewModel.init(openedImage, selectedDevice)

        // Use an immutable PendingIntent as recommended by the latest API
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        mUsbPermissionIntent = PendingIntent.getBroadcast(
            this, 0, Intent().apply {
                action = Intents.USB_PERMISSION
            }, pendingIntentFlags
        )

        mSettings = AppSettings(this).apply {
            addListener(mViewModel)
            mViewModel.refreshSettings(this)
        }

        setContent {
            MainView(mViewModel) {
                val uiState by mViewModel.state.collectAsState()
                var showLayFlatDialog by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(uiState.selectedDevice) {
                    if (uiState.selectedDevice == null) {
                        toast(getString(R.string.usb_device_was_unplugged))
                        finish()
                    }
                }

                ConfirmationView(mViewModel, onConfirm = {
                    showLayFlatDialog = true
                }, onCancel = {
                    finish()
                }, askUsbPermission = {
                    val usbManager = getSystemService(USB_SERVICE) as UsbManager
                    usbManager.requestPermission(
                        uiState.selectedDevice!!.usbDevice, mUsbPermissionIntent
                    )
                })

                LaunchedEffect(uiState.hasUsbPermission, showLayFlatDialog) {
                    println("hasUsbPermission: ${uiState.hasUsbPermission}")
                }

                if (uiState.hasUsbPermission && showLayFlatDialog) {
                    LayFlatOnTableBottomSheet(
                        onReady = {
                            writeImage(
                                uiState.openedImage!!, uiState.selectedDevice!!
                            )
                        },
                        onDismissRequest = { showLayFlatDialog = false },
                    )
                }
            }
        }

    }
}

@Composable
fun ConfirmationViewLayout(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    sourceCard: @Composable (Modifier) -> Unit,
    destinationCard: @Composable (Modifier) -> Unit,
    warningCard: @Composable () -> Unit,
    cancelButton: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    content: @Composable () -> Unit = {},
) {
    ConstraintLayout(modifier = modifier) {
        val (titleRef, cardsRef, warningCardRef, cancelButtonRef, confirmButtonRef) = createRefs()

        Box(Modifier.constrainAs(titleRef) {
            top.linkTo(parent.top, 24.dp)
            start.linkTo(parent.start, 16.dp)
            end.linkTo(parent.end, 16.dp)
        }) {
            title()
        }

        Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .constrainAs(cardsRef) {
                        top.linkTo(titleRef.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(warningCardRef.top)
                    }, verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            sourceCard(Modifier.fillMaxWidth())

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                        modifier = Modifier.size(48.dp), imageVector = Icons.TwoTone.ArrowDownward,
                        contentDescription = null
                )
            }

            destinationCard(Modifier.fillMaxWidth())
        }

        Box(modifier = Modifier.constrainAs(warningCardRef) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            bottom.linkTo(confirmButtonRef.top, 16.dp)
        }) {
            warningCard()
        }

        Box(modifier = Modifier.constrainAs(cancelButtonRef) {
            bottom.linkTo(parent.bottom, 16.dp)
            end.linkTo(confirmButtonRef.start, 8.dp)
        }) {
            cancelButton()
        }

        Box(modifier = Modifier.constrainAs(confirmButtonRef) {
            bottom.linkTo(parent.bottom, 16.dp)
            end.linkTo(parent.end, 16.dp)
        }) {
            confirmButton()
        }

        content()
    }
}

@Composable
fun ConfirmationView(
    viewModel: ConfirmOperationActivityViewModel,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    askUsbPermission: () -> Unit,
) {
    val uiState by viewModel.state.collectAsState()

    ConfirmationViewLayout(
            modifier = Modifier.fillMaxSize(),
            title = {
                Text(
                        text = stringResource(R.string.confirm_operation),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                )
            },
            sourceCard = { modifier ->
                Card {
                    Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(modifier),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                                modifier = Modifier
                                    .size(128.dp)
                                    .padding(16.dp, 16.dp, 0.dp, 16.dp),
                                imageVector = ImageVector.vectorResource(
                                        id = R.drawable.ic_disk_image_large
                                ), contentDescription = stringResource(R.string.disk_image)
                        )
                        Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(
                                        8.dp,
                                        Alignment.CenterVertically
                                )
                        ) {
                            val sourceUri = uiState.openedImage
                            val context = LocalContext.current
                            val sourceFileName by remember {
                                derivedStateOf {
                                    sourceUri?.getFileName(context) ?: sourceUri?.getFilePath(
                                            context
                                    )
                                        ?.split("/")
                                        ?.last() ?: context.getString(R.string.unknown_filename)
                                }
                            }
                            val sourceFileSize by remember {
                                derivedStateOf {
                                    try {
                                        sourceUri!!.getFileSize(context).toHRSize()
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Failed to get file size", e)
                                        "Unknown file size"
                                    }
                                }
                            }

                            Text(
                                    text = stringResource(R.string.image_to_write),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 8.dp)
                            )

                            Column {
                                Text(
                                        text = sourceFileName,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                        text = sourceFileSize,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontStyle = FontStyle.Italic,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            destinationCard = { modifier ->
                Card {
                    Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(modifier),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                                modifier = Modifier
                                    .size(128.dp)
                                    .padding(16.dp, 16.dp, 0.dp, 16.dp),
                                imageVector = ImageVector.vectorResource(
                                        id = R.drawable.ic_usb_stick_large
                                ), contentDescription = stringResource(R.string.usb_drive)
                        )
                        Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(
                                        8.dp,
                                        Alignment.CenterVertically
                                )
                        ) {
                            Text(
                                    text = stringResource(R.string.destination_usb_device),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                            )
                            Column {
                                Text(
                                        text = uiState.selectedDevice?.name ?: stringResource(
                                                R.string.unknown_device
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                        text = uiState.selectedDevice?.vidpid ?: "Unknown VID:PID",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontStyle = FontStyle.Italic, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                    onClick = askUsbPermission,
                                    enabled = !uiState.hasUsbPermission,
                                    contentPadding = if (!uiState.hasUsbPermission) PaddingValues(
                                            24.dp, 8.dp
                                    )
                                    else PaddingValues(24.dp, 8.dp, 16.dp, 8.dp),
                            ) {
                                Text(text = stringResource(R.string.grant_access))
                                if (uiState.hasUsbPermission) {
                                    Icon(
                                            modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp),
                                            imageVector = Icons.TwoTone.Check,
                                            contentDescription = stringResource(R.string.permission_granted)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            warningCard = {
                Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                ) {
                    Row(
                            modifier = Modifier
                                .padding(16.dp, 16.dp, 16.dp, 16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                                modifier = Modifier.size(48.dp),
                                imageVector = Icons.TwoTone.Warning,
                                contentDescription = null
                        )
                        Column {
                            Text(
                                    text = stringResource(R.string.be_careful),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                    text = stringResource(
                                            R.string.writing_the_image_will_erase
                                    ), style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            },
            cancelButton = {
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(
                        onClick = onConfirm,
                        enabled = uiState.selectedDevice != null && uiState.hasUsbPermission
                ) {
                    Text(text = stringResource(R.string.write_image))
                }
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayFlatOnTableBottomSheet(
    onReady: () -> Unit,
    onDismissRequest: () -> Unit,
    useGravitySensor: Boolean = true,
) {
    val context = LocalContext.current
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.animated_check))
    val sensorManager =
        remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember(sensorManager) { sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) }
    val sheetState = rememberPorkedAroundSheetState(onDismissRequest, skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        if (sensor == null || !useGravitySensor) {
            Text(
                text = stringResource(R.string.lay_your_device_flat_no_accel),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp, 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                var hideSheet by remember { mutableStateOf(false) }
                LaunchedEffect(hideSheet) {
                    if (hideSheet) {
                        sheetState.hide()
                        onReady()
                    }
                }
                Button(onClick = { hideSheet = true }) {
                    Text(text = stringResource(R.string.continue_))
                }
            }
        } else {
            val readings = 10
            var hasBeenFlatOnTable by rememberSaveable { mutableStateOf(false) }
            var gravityCircularBuffer by remember { mutableStateOf(emptyList<Float>()) }

            val sensorListener = remember {
                object : SensorEventListener {
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

                    override fun onSensorChanged(event: SensorEvent) {
                        gravityCircularBuffer = gravityCircularBuffer + event.values[2]
                        if (gravityCircularBuffer.size > readings) {
                            gravityCircularBuffer = gravityCircularBuffer.drop(1)
                        }
                    }
                }
            }
            val movingAverage = remember(gravityCircularBuffer) {
                if (gravityCircularBuffer.size >= readings) gravityCircularBuffer.average()
                    .toFloat()
                else 0f
            }

            val isFlatOnTable by remember(movingAverage) {
                derivedStateOf {
                    movingAverage > 9.7f
                }
            }
            LaunchedEffect(isFlatOnTable) {
                if (isFlatOnTable) {
                    hasBeenFlatOnTable = true
                }
            }

            DisposableEffect(sensor, sensorListener, sensorManager) {
                println("Registering sensor listener")
                sensorManager.registerListener(
                    sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL
                )
                onDispose {
                    println("Unregistering sensor listener")
                    sensorManager.unregisterListener(sensorListener)
                }
            }

            Text(
                text = stringResource(R.string.lay_your_device_flat),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                if (!hasBeenFlatOnTable) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(128.dp)
                            .padding(15.dp),
                        strokeWidth = 5.dp,
                    )
                } else {
                    val progress by animateLottieCompositionAsState(
                        composition, isPlaying = hasBeenFlatOnTable
                    )
                    val lottieDynamicProperties = rememberLottieDynamicProperties(
                        rememberLottieDynamicProperty(
                            property = LottieProperty.COLOR_FILTER, value = PorterDuffColorFilter(
                                MaterialTheme.colorScheme.primary.toArgb(), PorterDuff.Mode.SRC_ATOP
                            ), keyPath = arrayOf("**")
                        )
                    )
                    LottieAnimation(
                        composition, progress = { progress }, modifier = Modifier.size(128.dp),
                        dynamicProperties = lottieDynamicProperties
                    )
                    LaunchedEffect(progress) {
                        if (progress == 1f) {
                            sheetState.hide()
                            onReady()
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp, 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                var hideSheet by remember { mutableStateOf(false) }
                LaunchedEffect(hideSheet) {
                    if (hideSheet) {
                        sheetState.hide()
                        onReady()
                    }
                }
                OutlinedButton(onClick = { hideSheet = true }) {
                    Text(text = stringResource(R.string.skip))
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun ConfirmationViewPreview() {
    val viewModel = remember {
        ConfirmOperationActivityViewModel().apply {
            setState(
                state.value.copy(
                    selectedDevice = UsbMassStorageDeviceDescriptor(
                        previewUsbDevice = PreviewUsbDevice(
                            name = "Kingston DataTraveler 3.0",
                            vidpid = "dead:beef",
                        )
                    ),
                    openedImage = Uri.parse(
                        "file:///storage/emulated/0/Download/etchdroid-test-image-very-long-name-lol-rip-ive-never-seen-a-filename-this-long-its-absolutely-crazy.img"
                    ),
                )
            )
        }
    }
    var showLayFlatSheet by rememberSaveable { mutableStateOf(false) }

    MainView(viewModel) {
        ConfirmationView(viewModel, onCancel = {
            viewModel.setState(
                viewModel.state.value.copy(
                    hasUsbPermission = false
                )
            )
        }, onConfirm = {
            showLayFlatSheet = true
        }, askUsbPermission = {
            viewModel.setState(
                viewModel.state.value.copy(
                    hasUsbPermission = true
                )
            )
        })

        if (showLayFlatSheet) {
            LayFlatOnTableBottomSheet(
                onReady = { showLayFlatSheet = false },
                onDismissRequest = { showLayFlatSheet = false }, useGravitySensor = true
            )
        }
    }
}
