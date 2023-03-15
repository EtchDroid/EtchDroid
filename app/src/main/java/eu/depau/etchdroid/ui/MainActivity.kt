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
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpCenter
import androidx.compose.material.icons.twotone.ArrowDownward
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.Clear
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavHostController
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.*
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import eu.depau.etchdroid.AppSettings
import eu.depau.etchdroid.BuildConfig
import eu.depau.etchdroid.Intents.USB_PERMISSION
import eu.depau.etchdroid.R
import eu.depau.etchdroid.ThemeMode
import eu.depau.etchdroid.getProgressUpdateIntent
import eu.depau.etchdroid.getStartJobIntent
import eu.depau.etchdroid.massstorage.PreviewUsbDevice
import eu.depau.etchdroid.massstorage.UsbMassStorageDeviceDescriptor
import eu.depau.etchdroid.service.WorkerService
import eu.depau.etchdroid.ui.composables.MainView
import eu.depau.etchdroid.ui.composables.coloredShadow
import eu.depau.etchdroid.ui.theme.notSupportedRed
import eu.depau.etchdroid.ui.theme.partiallySupportedYellow
import eu.depau.etchdroid.ui.theme.supportedGreen
import eu.depau.etchdroid.ui.utils.rememberPorkedAroundSheetState
import eu.depau.etchdroid.utils.broadcastReceiver
import eu.depau.etchdroid.utils.ktexts.*
import eu.depau.etchdroid.utils.reviews.WriteReviewHelper
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val TAG = "MainActivity"


class MainActivity : ComponentActivity() {
    private val mViewModel: MainActivityViewModel by viewModels()
    private lateinit var mSettings: AppSettings
    private lateinit var mUsbPermissionIntent: PendingIntent
    private lateinit var mNavController: NavHostController

    private val mFilePickerActivity: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { result ->
            openUri(result ?: return@registerForActivityResult)
        }

    private fun ActivityResultLauncher<Array<String>>.launch() = launch(arrayOf("*/*"))

    private val mUsbDevicesReceiver = broadcastReceiver { intent ->
        if (intent.usbDevice == null) {
            Log.w(TAG, "Received USB broadcast without device")
            return@broadcastReceiver
        }
        Log.d(TAG, "Received USB broadcast: ${intent.action}, device: ${intent.usbDevice}")

        when (intent.action) {
            USB_PERMISSION -> {
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (granted) mViewModel.usbPermissionGranted(intent.usbDevice!!)
                else toast(
                    getString(
                        R.string.permission_denied_for_usb_device, intent.usbDevice!!.deviceName
                    )
                )
            }

            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                mViewModel.usbDeviceAttached(intent.usbDevice!!)
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                mViewModel.usbDeviceDetached(intent.usbDevice!!)
            }

            else -> {
                Log.w(TAG, "Received unknown broadcast: ${intent.action}")
            }
        }
    }

    private fun registerUsbReceiver() {
        val usbPermissionFilter = IntentFilter(USB_PERMISSION)
        val usbAttachedFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        val usbDetachedFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)

        registerReceiver(mUsbDevicesReceiver, usbPermissionFilter)
        registerReceiver(mUsbDevicesReceiver, usbAttachedFilter)
        registerReceiver(mUsbDevicesReceiver, usbDetachedFilter)

        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        mViewModel.replaceUsbDevices(usbManager.deviceList.values)
    }

    private fun unregisterUsbReceiver() {
        unregisterReceiver(mUsbDevicesReceiver)
    }

    private fun openUri(uri: Uri, userSaysYolo: Boolean = false) {
        if (!userSaysYolo) {
            try {
                // Check if it's a Windows image and alert the user; fail-open
                (uri.getFileName(this) ?: uri.getFilePath(this)
                    ?.split("/")
                    ?.last())?.let { filename ->
                    val lowercase = filename.lowercase()
                    if ("windows" in lowercase || lowercase.startsWith("win")) {
                        mViewModel.setShowWindowsAlertUri(uri)
                        return
                    }
                }
            } catch (e: Exception) {
                Log.w("Failed to get filename", e)
            }
        }
        mViewModel.setOpenedImage(uri)

        if (mViewModel.state.value.massStorageDevices.size == 1) {
            mViewModel.selectDevice(this, mViewModel.state.value.massStorageDevices.first())
            mNavController.navigate("confirm")
        }
    }

    private fun writeImage(uri: Uri, device: UsbMassStorageDeviceDescriptor) {
        val jobId = Random.nextInt()
        val intent =
            getStartJobIntent(uri, device, jobId, 0, false, this, WorkerService::class.java)
        Log.d(TAG, "Starting service with intent: $intent")
        startForegroundServiceCompat(intent)
        startActivity(
            getProgressUpdateIntent(
                uri, device, jobId, 0f, 0, 0, false, this,
                ProgressActivity::class.java
            )
        )
        finish()
    }

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pendingIntentFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }

        mUsbPermissionIntent =
            PendingIntent.getBroadcast(this, 0, Intent(USB_PERMISSION), pendingIntentFlags)

        mSettings = AppSettings(this).apply {
            addListener(mViewModel)
            mViewModel.refreshSettings(this)
        }

        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { openUri(it) }
        }

        setContent {
            MainView(mViewModel) {
                mNavController = rememberAnimatedNavController()
                AnimatedNavHost(mNavController, startDestination = "start") {
                    composable("start") {
                        StartView(
                            mViewModel, setThemeMode = { mSettings.themeMode = it },
                            setDynamicTheme = { mSettings.dynamicColors = it },
                            onCTAClick = { mFilePickerActivity.launch() },
                            openAboutView = { mNavController.navigate("about") },
                        )
                        val uiState by mViewModel.state.collectAsState()
                        if (uiState.showWindowsAlertForUri != null) {
                            WindowsImageAlertDialog(
                                onDismissRequest = { mViewModel.setShowWindowsAlertUri(null) },
                                onConfirm = { openUri(uiState.showWindowsAlertForUri!!, true) })
                        }
                        if (uiState.openedImage != null) {
                            val context = LocalContext.current
                            UsbDevicePickerBottomSheet(
                                onDismissRequest = {
                                    if (uiState.selectedDevice == null) mViewModel.setOpenedImage(
                                        null
                                    )
                                },
                                selectDevice = { mViewModel.selectDevice(context, it) },
                                availableDevices = { uiState.massStorageDevices },
                                onSelectAnimationComplete = {
                                    if (uiState.selectedDevice != null) mNavController.navigate(
                                        "confirm"
                                    )
                                })
                        }
                    }
                    composable("confirm") {
                        val uiState by mViewModel.state.collectAsState()
                        var showLayflatDialog by rememberSaveable { mutableStateOf(false) }

                        LaunchedEffect(uiState.selectedDevice) {
                            if (uiState.selectedDevice == null) {
                                mNavController.popBackStack("start", false)
                                toast(getString(R.string.usb_device_was_unplugged))
                            }
                        }

                        ConfirmationView(
                            mViewModel,
                            onConfirm = {
                                showLayflatDialog = true
                            },
                            onCancel = {
                                mViewModel.setOpenedImage(null)
                                mNavController.popBackStack("start", false)
                            },
                            askUsbPermission = {
                                val usbManager =
                                    getSystemService(USB_SERVICE) as UsbManager
                                usbManager.requestPermission(
                                    uiState.selectedDevice!!.usbDevice,
                                    mUsbPermissionIntent
                                )
                            }
                        )

                        LaunchedEffect(uiState.hasUsbPermission, showLayflatDialog) {
                            println("hasUsbPermission: ${uiState.hasUsbPermission}")
                        }

                        if (uiState.hasUsbPermission && showLayflatDialog) {
                            LayFlatOnTableBottomSheet(
                                onReady = {
                                    writeImage(
                                        uiState.openedImage!!,
                                        uiState.selectedDevice!!
                                    )
                                },
                                onDismissRequest = { showLayflatDialog = false },
                            )
                        }
                    }
                    composable("about") {
                        AboutView(mViewModel)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerUsbReceiver()
    }

    override fun onStop() {
        super.onStop()
        unregisterUsbReceiver()
    }
}

@Composable
fun StartView(
    viewModel: MainActivityViewModel,
    setThemeMode: (ThemeMode) -> Unit = {},
    setDynamicTheme: (Boolean) -> Unit = {},
    onCTAClick: () -> Unit = {},
    openAboutView: () -> Unit = {},
) {
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val uiState by viewModel.state.collectAsState()
        var menuOpen by remember { mutableStateOf(false) }
        var whatCanIWriteOpen by remember { mutableStateOf(false) }

        val iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant
        val systemInDarkMode = isSystemInDarkTheme()
        val darkMode by remember {
            derivedStateOf {
                when (uiState.themeMode) {
                    ThemeMode.SYSTEM -> systemInDarkMode
                    ThemeMode.DARK -> true
                    else -> false
                }
            }
        }

        val (title, centerBox, bottomButton, menuButton, dropdown) = createRefs()
        Text(
            modifier = Modifier.constrainAs(title) {
                top.linkTo(parent.top, 24.dp)
                start.linkTo(parent.start, 16.dp)
                end.linkTo(parent.end, 16.dp)
            },
            text = "EtchDroid",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
        )

        Column(
            modifier = Modifier.constrainAs(centerBox) {
                top.linkTo(parent.top, 16.dp)
                bottom.linkTo(parent.bottom, 16.dp)
                start.linkTo(parent.start, 16.dp)
                end.linkTo(parent.end, 16.dp)
            }, horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(64.dp)
        ) {


            Icon(
                modifier = Modifier.size(128.dp).run {
                    if (darkMode) {
                        coloredShadow(
                            MaterialTheme.colorScheme.onSecondaryContainer, borderRadius = 64.dp,
                            shadowRadius = 128.dp, alpha = 0.5f
                        )
                    } else {
                        drawBehind {
                            drawCircle(
                                color = iconBackgroundColor, radius = 96.dp.toPx()
                            )
                        }
                    }
                }, imageVector = getEtchDroidIcon(
                    headColor = if (darkMode) MaterialTheme.colorScheme.primary.toArgb()
                        .toLong() else MaterialTheme.colorScheme.primaryContainer.toArgb().toLong(),
                ), contentDescription = "EtchDroid", tint = Color.Unspecified
            )
            ExtendedFloatingActionButton(
                onClick = onCTAClick,
                text = { Text(stringResource(R.string.write_an_image)) },
                icon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(
                            id = R.drawable.ic_write_to_usb
                        ), contentDescription = null
                    )
                },
            )
        }

        OutlinedButton(
            modifier = Modifier.constrainAs(bottomButton) {
                bottom.linkTo(parent.bottom, 16.dp)
                start.linkTo(parent.start, 16.dp)
                end.linkTo(parent.end, 16.dp)
            },
            onClick = { whatCanIWriteOpen = true },
        ) {
            Text(stringResource(R.string.whats_supported))
        }

        IconButton(modifier = Modifier.constrainAs(menuButton) {
            bottom.linkTo(parent.bottom, 16.dp)
            end.linkTo(parent.end, 16.dp)
        }, onClick = { menuOpen = true }) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_menu),
                contentDescription = "Menu"
            )
        }

        Box(modifier = Modifier.constrainAs(dropdown) {
            bottom.linkTo(menuButton.top)
            end.linkTo(parent.end, 16.dp)
        }) {
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                Text(
                    stringResource(R.string.style), style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(12.dp, 0.dp)
                )

                // Radio buttons for theme modes SYSTEM, LIGHT, DARK
                DropdownMenuItem(onClick = { setThemeMode(ThemeMode.SYSTEM) },
                    text = { Text(stringResource(R.string.device_setting)) }, leadingIcon = {
                        RadioButton(modifier = Modifier.size(20.dp),
                            selected = uiState.themeMode == ThemeMode.SYSTEM,
                            onClick = { setThemeMode(ThemeMode.SYSTEM) })
                    })
                DropdownMenuItem(onClick = { setThemeMode(ThemeMode.LIGHT) },
                    text = { Text(stringResource(R.string.light)) }, leadingIcon = {
                        RadioButton(modifier = Modifier.size(20.dp),
                            selected = uiState.themeMode == ThemeMode.LIGHT,
                            onClick = { setThemeMode(ThemeMode.LIGHT) })
                    })
                DropdownMenuItem(onClick = { setThemeMode(ThemeMode.DARK) },
                    text = { Text(stringResource(R.string.dark)) }, leadingIcon = {
                        RadioButton(modifier = Modifier.size(20.dp),
                            selected = uiState.themeMode == ThemeMode.DARK,
                            onClick = { setThemeMode(ThemeMode.DARK) })
                    })

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    DropdownMenuItem(onClick = { setDynamicTheme(!uiState.dynamicColors) },
                        text = { Text(stringResource(R.string.dynamic_colors)) }, leadingIcon = {
                            Checkbox(modifier = Modifier.size(20.dp),
                                checked = uiState.dynamicColors,
                                onCheckedChange = { setDynamicTheme(!uiState.dynamicColors) })
                        })
                }

                Divider()

                DropdownMenuItem(
                    onClick = { openAboutView() }, text = { Text(stringResource(R.string.about)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.TwoTone.Info,
                            contentDescription = stringResource(R.string.about)
                        )
                    })
            }
        }

        if (whatCanIWriteOpen) {
            WhatCanIWriteBottomSheet(
                onDismissRequest = {
                    whatCanIWriteOpen = false
                },
                darkTheme = darkMode
            )
        }
    }
}

@Composable
fun WindowsImageAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {},
) = AlertDialog(onDismissRequest = onDismissRequest, title = {
    Text(text = stringResource(R.string.is_this_a_windows_iso))
}, text = {
    Text(
        text = stringResource(R.string.a_regular_windows_iso_won_t_work)
    )
}, icon = {
    Icon(
        imageVector = ImageVector.vectorResource(id = R.drawable.ic_windows),
        contentDescription = "Windows logo"
    )
}, confirmButton = {
    TextButton(onClick = {
        onConfirm()
        onDismissRequest()
    }) {
        Text(stringResource(R.string.continue_anyway))
    }
}, dismissButton = {
    TextButton(onClick = {
        onCancel()
        onDismissRequest()
    }) {
        Text(stringResource(R.string.cancel))
    }
})

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsbDevicePickerBottomSheet(
    onDismissRequest: () -> Unit,
    selectDevice: (UsbMassStorageDeviceDescriptor) -> Unit,
    onSelectAnimationComplete: () -> Unit = {},
    availableDevices: () -> Set<UsbMassStorageDeviceDescriptor>,
    skipHalfExpanded: Boolean = true,
) {
    val bottomSheetState =
        rememberPorkedAroundSheetState(onDismissRequest, skipPartiallyExpanded = skipHalfExpanded)
    val scope = rememberCoroutineScope()
    val anyDeviceAvailable by remember(availableDevices) {
        derivedStateOf { availableDevices().isNotEmpty() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = bottomSheetState,
    ) {
        Text(
            text = if (anyDeviceAvailable) stringResource(
                R.string.select_a_usb_drive
            ) else stringResource(
                R.string.connect_a_usb_drive
            ),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(32.dp, 0.dp, 32.dp, 16.dp)
                .fillMaxWidth()
        )
        if (anyDeviceAvailable) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(availableDevices().toList()) { device ->
                    ListItem(
                        modifier = Modifier.clickable {
                            selectDevice(device)
                            scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                                if (!bottomSheetState.isVisible) {
                                    onDismissRequest()
                                }
                                onSelectAnimationComplete()
                            }
                        },
                        headlineContent = { Text(device.name) },
                        supportingContent = { Text(device.vidpid, fontStyle = FontStyle.Italic) },
                        leadingContent = {
                            Icon(
                                imageVector = ImageVector.vectorResource(
                                    id = R.drawable.ic_usb_stick
                                ), contentDescription = "USB drive"
                            )
                        },
                    )
                }
            }
        } else {
            Icon(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp)
                    .height(128.dp),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_usb_stick_search),
                contentDescription = stringResource(R.string.looking_for_usb_drives),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 24.dp, 0.dp, 0.dp),
        )
    }
}

@Composable
fun ConfirmationView(
    viewModel: MainActivityViewModel,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    askUsbPermission: () -> Unit,
) {
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (title, cards, warningCard, cancelButton, confirmButton) = createRefs()

        val uiState by viewModel.state.collectAsState()
        val sourceUri = uiState.openedImage
        val context = LocalContext.current
        val sourceFileName by remember {
            derivedStateOf {
                sourceUri?.getFileName(context) ?: sourceUri?.getFilePath(context)
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
            modifier = Modifier.constrainAs(title) {
                top.linkTo(parent.top, 24.dp)
                start.linkTo(parent.start, 16.dp)
                end.linkTo(parent.end, 16.dp)
            },
            text = stringResource(R.string.confirm_operation),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .constrainAs(cards) {
                    top.linkTo(title.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(warningCard.top)
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                    ) {
                        Text(
                            text = stringResource(R.string.image_to_write),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 8.dp)
                        )
                        Column {
                            Text(
                                text = sourceFileName, style = MaterialTheme.typography.labelLarge,
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = sourceFileSize, style = MaterialTheme.typography.labelMedium,
                                fontStyle = FontStyle.Italic, maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    modifier = Modifier.size(48.dp), imageVector = Icons.TwoTone.ArrowDownward,
                    contentDescription = null
                )
            }

            Card {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
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
                                style = MaterialTheme.typography.labelLarge, maxLines = 1,
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
                            contentPadding = if (!uiState.hasUsbPermission)
                                PaddingValues(24.dp, 8.dp)
                            else
                                PaddingValues(24.dp, 8.dp, 16.dp, 8.dp),
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
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .constrainAs(warningCard) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(confirmButton.top, 16.dp)
                }, colors = CardDefaults.cardColors(
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
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        OutlinedButton(
            modifier = Modifier.constrainAs(cancelButton) {
                bottom.linkTo(parent.bottom, 16.dp)
                end.linkTo(confirmButton.start, 8.dp)
            }, onClick = onCancel
        ) {
            Text(stringResource(R.string.cancel))
        }

        Button(
            modifier = Modifier.constrainAs(confirmButton) {
                bottom.linkTo(parent.bottom, 16.dp)
                end.linkTo(parent.end, 16.dp)
            },
            onClick = onConfirm,
            enabled = uiState.selectedDevice != null && uiState.hasUsbPermission
        ) {
            Text(text = stringResource(R.string.write_image))
        }
    }
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
                if (gravityCircularBuffer.size >= readings)
                    gravityCircularBuffer.average().toFloat()
                else
                    0f
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
                            property = LottieProperty.COLOR_FILTER,
                            value = PorterDuffColorFilter(
                                MaterialTheme.colorScheme.primary.toArgb(),
                                PorterDuff.Mode.SRC_ATOP
                            ),
                            keyPath = arrayOf("**")
                        )
                    )
                    LottieAnimation(
                        composition,
                        progress = { progress },
                        modifier = Modifier.size(128.dp),
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

enum class SupportStatus {
    SUPPORTED,
    MAYBE_SUPPORTED,
    UNSUPPORTED,
}

@Composable
fun ItemSupportEntry(
    description: String,
    supportStatus: SupportStatus,
    darkTheme: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (supportStatus) {
                SupportStatus.SUPPORTED -> Icons.TwoTone.Check
                SupportStatus.MAYBE_SUPPORTED -> Icons.Outlined.HelpCenter
                SupportStatus.UNSUPPORTED -> Icons.TwoTone.Clear
            },
            contentDescription = when (supportStatus) {
                SupportStatus.SUPPORTED -> stringResource(R.string.supported)
                SupportStatus.MAYBE_SUPPORTED -> stringResource(R.string.maybe_supported)
                SupportStatus.UNSUPPORTED -> stringResource(R.string.not_supported)
            },
            modifier = Modifier
                .size(20.dp)
                .padding(0.dp, 4.dp, 0.dp, 0.dp),
            tint = when (supportStatus) {
                SupportStatus.SUPPORTED -> supportedGreen(darkTheme)
                SupportStatus.MAYBE_SUPPORTED -> partiallySupportedYellow(darkTheme)
                SupportStatus.UNSUPPORTED -> notSupportedRed(darkTheme)
            }
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatCanIWriteBottomSheet(onDismissRequest: () -> Unit, darkTheme: Boolean = false) {
    val sheetState = rememberPorkedAroundSheetState(onDismissRequest, false)
    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp, 16.dp, 32.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        ) {
            item {
                Text(
                    text = stringResource(R.string.supported_devices_and_images),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                )
            }
            item {
                Text(
                    text = stringResource(R.string.devices),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = 8.dp, top = 8.dp)
                        .fillMaxWidth()
                )
            }
            item {
                ItemSupportEntry(
                    stringResource(R.string.usb_flash_drives),
                    SupportStatus.SUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    stringResource(R.string.memory_cards_using_a_usb_adapter),
                    SupportStatus.SUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    description = stringResource(R.string.usb_hard_drives_ssds_some_might_work),
                    SupportStatus.MAYBE_SUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    description = stringResource(
                        R.string.usb_docks_and_hubs_might_have_power_issues
                    ),
                    SupportStatus.MAYBE_SUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    stringResource(R.string.memory_cards_using_the_internal_slot),
                    SupportStatus.UNSUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    description = stringResource(R.string.optical_disk_drives),
                    SupportStatus.UNSUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    description = stringResource(R.string.floppy_disk_drives),
                    SupportStatus.UNSUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    description = stringResource(R.string.any_thunderbolt_only_device),
                    SupportStatus.UNSUPPORTED, darkTheme
                )
            }
            item {
                Text(
                    text = stringResource(R.string.disk_images),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = 8.dp, top = 32.dp)
                        .fillMaxWidth()
                )
            }
            item {
                ItemSupportEntry(
                    description = stringResource(R.string.microsoft_windows_isos_any_version),
                    SupportStatus.UNSUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    description = stringResource(R.string.apple_dmg_disk_images),
                    SupportStatus.UNSUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    description = stringResource(
                        R.string.arch_linux_ubuntu_debian_fedora_pop_os_linux_mint_freebsd_etc
                    ),
                    SupportStatus.SUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    description = stringResource(
                        R.string.other_modern_gnu_linux_and_bsd_distributions_live_isos
                    ),
                    SupportStatus.SUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    description = stringResource(R.string.raspberry_pi_sd_card_images_unzip_first),
                    SupportStatus.SUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    description = stringResource(
                        R.string.any_other_image_that_works_with_balena_etcher_or_dd
                    ),
                    SupportStatus.SUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    description = stringResource(R.string.older_gnu_linux_distributions_isos_2010),
                    SupportStatus.MAYBE_SUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    description = "Damn Small Linux, Puppy Linux",
                    SupportStatus.UNSUPPORTED, darkTheme
                )
            }
            item {
                val uriHandler = LocalUriHandler.current
                val annotatedString = buildAnnotatedString {
                    val str = stringResource(R.string.support_for_dmg_images_was_removed, "GitHub")
                    val startIndex = str.indexOf("GitHub")
                    val endIndex = startIndex + "GitHub".length
                    append(str)
                    addStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = startIndex,
                        end = endIndex
                    )
                    addStringAnnotation(
                        tag = "URL",
                        annotation = "https://github.com/EtchDroid/EtchDroid/releases/tag/dmg-support",
                        start = startIndex,
                        end = endIndex
                    )
                }
                ClickableText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    onClick = {
                        annotatedString
                            .getStringAnnotations("URL", it, it)
                            .firstOrNull()?.let { stringAnnotation ->
                                uriHandler.openUri(stringAnnotation.item)
                            }
                    })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutView(viewModel: MainActivityViewModel) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        val box = createRef()

        Column(
            modifier = Modifier
                .constrainAs(box) {
                    centerTo(parent)
                }
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${stringResource(R.string.app_name)} v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                textAlign = TextAlign.Center,
            )
            val darkMode by viewModel.darkMode
            val iconBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    modifier = Modifier
                        .size(128.dp)
                        .run {
                            if (darkMode) {
                                coloredShadow(
                                    MaterialTheme.colorScheme.onSecondaryContainer,
                                    borderRadius = 64.dp,
                                    shadowRadius = 128.dp, alpha = 0.5f
                                )
                            } else {
                                drawBehind {
                                    drawCircle(
                                        color = iconBackgroundColor, radius = 96.dp.toPx()
                                    )
                                }
                            }
                        }, imageVector = getEtchDroidIcon(
                        headColor = if (darkMode) MaterialTheme.colorScheme.primary.toArgb()
                            .toLong() else MaterialTheme.colorScheme.primaryContainer.toArgb()
                            .toLong(),
                    ), contentDescription = "EtchDroid", tint = Color.Unspecified
                )
            }

            val annotatedText = buildAnnotatedString {
                val name = "Davide Depau"
                val contributors = stringResource(R.string.contributors)
                val github = "GitHub"
                val str = stringResource(R.string.developed_by, name, contributors, github)
                val nameStart = str.indexOf(name)
                val nameEnd = nameStart + name.length
                val contributorsStart = str.indexOf(contributors)
                val contributorsEnd = contributorsStart + contributors.length
                val githubStart = str.indexOf(github)
                val githubEnd = githubStart + github.length
                append(str)

                for ((start, end) in listOf(
                    nameStart to nameEnd,
                    contributorsStart to contributorsEnd,
                    githubStart to githubEnd
                )) {
                    addStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = start,
                        end = end
                    )
                }

                addStringAnnotation(
                    tag = "URL",
                    annotation = "https://depau.eu",
                    start = nameStart,
                    end = nameEnd
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = "https://github.com/EtchDroid/EtchDroid/graphs/contributors",
                    start = contributorsStart,
                    end = contributorsEnd
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = "https://github.com/EtchDroid/EtchDroid",
                    start = githubStart,
                    end = githubEnd
                )
            }

            val activity = LocalContext.current.activity
            ClickableText(
                modifier = Modifier
                    .fillMaxWidth(),
                text = annotatedText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                ),
                onClick = {
                    annotatedText
                        .getStringAnnotations("URL", it, it)
                        .firstOrNull()?.let { stringAnnotation ->
                            activity?.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(stringAnnotation.item)
                                )
                            )
                        }
                })

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                OutlinedButton(
                    onClick = {
                        activity?.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://etchdroid.depau.eu")
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.website))
                }
                OutlinedButton(
                    onClick = {
                        activity?.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://etchdroid.depau.eu/donate")
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.donate))
                }
                val reviewHelper = remember { activity?.let { WriteReviewHelper(it) } }
                if (reviewHelper != null) {
                    OutlinedButton(onClick = { reviewHelper.launchReviewFlow() }) {
                        Text(
                            text = if (reviewHelper.isGPlayFlavor) stringResource(R.string.review)
                            else "GitHub"
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4, showSystemUi = true)
@Composable
fun StartViewPreview() {
    val viewModel = remember { MainActivityViewModel() }
    val uiState by viewModel.state.collectAsState()

    MainView(viewModel) {
        StartView(
            viewModel,
            setThemeMode = {
                viewModel.setState(
                    uiState.copy(
                        themeMode = it
                    )
                )
            },
            setDynamicTheme = {
                viewModel.setState(
                    uiState.copy(
                        dynamicColors = it
                    )
                )
            },
            onCTAClick = { /*TODO*/ },
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4, showSystemUi = true)
@Composable
fun WindowsAlertDialogPreview() {
    val viewModel = remember { MainActivityViewModel() }

    MainView(viewModel) {
        WindowsImageAlertDialog(onDismissRequest = { /*TODO*/ }, onConfirm = { /*TODO*/ },
            onCancel = { /*TODO*/ })
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4, showSystemUi = true)
@Composable
fun UsbDevicePickerBottomSheetPreview() {
    val viewModel = remember { MainActivityViewModel() }
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }

    MainView(viewModel) {
        Row(
            Modifier.toggleable(value = openBottomSheet, role = Role.Checkbox,
                onValueChange = { checked -> openBottomSheet = checked })
        ) {
            Checkbox(checked = openBottomSheet, onCheckedChange = null)
            Spacer(Modifier.width(16.dp))
            Text("Open bottom sheet")
        }

        LaunchedEffect(Unit) {
            openBottomSheet = true
        }

        if (openBottomSheet) {
            UsbDevicePickerBottomSheet(onDismissRequest = { openBottomSheet = false },
                selectDevice = { /*TODO*/ }, availableDevices = {
                    setOf(
                        UsbMassStorageDeviceDescriptor(
                            previewUsbDevice = PreviewUsbDevice(
                                name = "Kingston DataTraveler 3.0",
                                vidpid = "dead:beef",
                            )
                        ),
                        UsbMassStorageDeviceDescriptor(
                            previewUsbDevice = PreviewUsbDevice(
                                name = "SanDisk Cruzer Blade",
                                vidpid = "cafe:babe",
                            )
                        ),
                        UsbMassStorageDeviceDescriptor(
                            previewUsbDevice = PreviewUsbDevice(
                                name = "ADATA DashDrive Elite HE720",
                                vidpid = "1337:d00d",
                            )
                        ),
                    )
                })
        }
    }
}


@Preview(showBackground = true, device = Devices.PIXEL_4, showSystemUi = true)
@Composable
fun EmptyUsbDevicePickerBottomSheetPreview() {
    val viewModel = remember { MainActivityViewModel() }
    var openBottomSheet by remember { mutableStateOf(false) }

    MainView(viewModel) {
        Row(
            Modifier.toggleable(value = openBottomSheet, role = Role.Checkbox,
                onValueChange = { checked -> openBottomSheet = checked })
        ) {
            Checkbox(checked = openBottomSheet, onCheckedChange = null)
            Spacer(Modifier.width(16.dp))
            Text("Open bottom sheet")
        }

        LaunchedEffect(Unit) {
            openBottomSheet = true
        }

        if (openBottomSheet) {
            UsbDevicePickerBottomSheet(onDismissRequest = { openBottomSheet = false },
                selectDevice = { /*TODO*/ }, availableDevices = { emptySet() })
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4, showSystemUi = true)
@Composable
fun ConfirmationViewPreview() {
    val viewModel = remember {
        MainActivityViewModel().apply {
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
        ConfirmationView(
            viewModel,
            onCancel = {
                viewModel.setState(
                    viewModel.state.value.copy(
                        hasUsbPermission = false
                    )
                )
            },
            onConfirm = {
                showLayFlatSheet = true
            },
            askUsbPermission = {
                viewModel.setState(
                    viewModel.state.value.copy(
                        hasUsbPermission = true
                    )
                )
            }
        )

        if (showLayFlatSheet) {
            LayFlatOnTableBottomSheet(
                onReady = { showLayFlatSheet = false },
                onDismissRequest = { showLayFlatSheet = false },
                useGravitySensor = true
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4, showSystemUi = true)
@Composable
fun AboutViewPreview() {
    val viewModel = remember { MainActivityViewModel() }
    MainView(viewModel) {
        AboutView(viewModel)
    }
}