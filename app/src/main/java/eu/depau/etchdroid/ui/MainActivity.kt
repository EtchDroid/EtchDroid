package eu.depau.etchdroid.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpCenter
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.Clear
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import eu.depau.etchdroid.AppSettings
import eu.depau.etchdroid.R
import eu.depau.etchdroid.ThemeMode
import eu.depau.etchdroid.getConfirmOperationActivityIntent
import eu.depau.etchdroid.massstorage.UsbMassStorageDeviceDescriptor
import eu.depau.etchdroid.ui.composables.MainView
import eu.depau.etchdroid.ui.composables.coloredShadow
import eu.depau.etchdroid.ui.theme.notSupportedRed
import eu.depau.etchdroid.ui.theme.partiallySupportedYellow
import eu.depau.etchdroid.ui.theme.supportedGreen
import eu.depau.etchdroid.ui.utils.rememberPorkedAroundSheetState
import eu.depau.etchdroid.utils.broadcastReceiver
import eu.depau.etchdroid.utils.ktexts.getFileName
import eu.depau.etchdroid.utils.ktexts.getFilePath
import eu.depau.etchdroid.utils.ktexts.registerExportedReceiver
import eu.depau.etchdroid.utils.ktexts.usbDevice

private const val TAG = "MainActivity"


class MainActivity : ComponentActivity() {
    private val mViewModel: MainActivityViewModel by viewModels()
    private lateinit var mSettings: AppSettings

    private val mFilePickerActivity: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { result ->
            openUri(result ?: return@registerForActivityResult)
        }

    private fun ActivityResultLauncher<Array<String>>.launch() = launch(arrayOf("*/*"))

    private val mUsbDevicesReceiver = broadcastReceiver { intent ->

        if (intent.usbDevice == null) {
            Log.w(TAG, "Received USB broadcast without device, ignoring: $intent")
            return@broadcastReceiver
        }

        Log.d(
            TAG,
            "Received USB broadcast: $intent, action: ${intent.action}, device: ${intent.usbDevice}"
        )

        when (intent.action) {
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
        val usbAttachedFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        val usbDetachedFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)

        registerExportedReceiver(mUsbDevicesReceiver, usbAttachedFilter)
        registerExportedReceiver(mUsbDevicesReceiver, usbDetachedFilter)

        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        mViewModel.replaceUsbDevices(usbManager.deviceList.values)
    }

    private fun unregisterUsbReceiver() {
        unregisterReceiver(mUsbDevicesReceiver)
    }

    private fun launchConfirmationActivity(
        selectedDevice: UsbMassStorageDeviceDescriptor,
        openedImage: Uri,
    ) {
        startActivity(
            getConfirmOperationActivityIntent(
                openedImage,
                selectedDevice,
                this,
                ConfirmOperationActivity::class.java
            )
        )
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
            launchConfirmationActivity(
                mViewModel.state.value.massStorageDevices.first(),
                uri
            )
        }
    }


    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        mSettings = AppSettings(this).apply {
            addListener(mViewModel)
            mViewModel.refreshSettings(this)
        }

        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { openUri(it) }
        }

        setContent {
            MainView(mViewModel) {
                StartView(
                    mViewModel, setThemeMode = { mSettings.themeMode = it },
                    setDynamicTheme = { mSettings.dynamicColors = it },
                    onCTAClick = { mFilePickerActivity.launch() },
                    openAboutView = {
                        startActivity(Intent(this, AboutActivity::class.java))
                    },
                )
                val uiState by mViewModel.state.collectAsState()
                if (uiState.showWindowsAlertForUri != null) {
                    WindowsImageAlertDialog(
                        onDismissRequest = { mViewModel.setShowWindowsAlertUri(null) },
                        onConfirm = { openUri(uiState.showWindowsAlertForUri!!, true) })
                }
                if (uiState.openedImage != null) {
                    UsbDevicePickerBottomSheet(
                        onDismissRequest = {
                            mViewModel.setOpenedImage(null)
                        },
                        selectDevice = {
                            launchConfirmationActivity(
                                it,
                                mViewModel.state.value.openedImage!!,
                            )
                        },
                        availableDevices = { uiState.massStorageDevices }
                    )
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
                modifier = Modifier
                    .size(128.dp)
                    .run {
                        if (darkMode) {
                            coloredShadow(
                                MaterialTheme.colorScheme.onSecondaryContainer,
                                borderRadius = 64.dp, shadowRadius = 128.dp, alpha = 0.5f
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

                DropdownMenuItem(onClick = { openAboutView() },
                    text = { Text(stringResource(R.string.about)) }, leadingIcon = {
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
                }, darkTheme = darkMode
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
    availableDevices: () -> Set<UsbMassStorageDeviceDescriptor>,
    skipHalfExpanded: Boolean = true,
) {
    val bottomSheetState =
        rememberPorkedAroundSheetState(onDismissRequest, skipPartiallyExpanded = skipHalfExpanded)
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
            ), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
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


enum class SupportStatus {
    SUPPORTED, MAYBE_SUPPORTED, UNSUPPORTED,
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
            }, contentDescription = when (supportStatus) {
                SupportStatus.SUPPORTED -> stringResource(R.string.supported)
                SupportStatus.MAYBE_SUPPORTED -> stringResource(R.string.maybe_supported)
                SupportStatus.UNSUPPORTED -> stringResource(R.string.not_supported)
            }, modifier = Modifier
                .size(20.dp)
                .padding(0.dp, 4.dp, 0.dp, 0.dp),
            tint = when (supportStatus) {
                SupportStatus.SUPPORTED -> supportedGreen(darkTheme)
                SupportStatus.MAYBE_SUPPORTED -> partiallySupportedYellow(darkTheme)
                SupportStatus.UNSUPPORTED -> notSupportedRed(darkTheme)
            }
        )
        Text(
            text = description, style = MaterialTheme.typography.bodyLarge,
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
                    style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                )
            }
            item {
                Text(
                    text = stringResource(R.string.devices),
                    style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = 8.dp, top = 8.dp)
                        .fillMaxWidth()
                )
            }
            item {
                ItemSupportEntry(
                    stringResource(R.string.usb_flash_drives), SupportStatus.SUPPORTED, darkTheme
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
                    ), SupportStatus.MAYBE_SUPPORTED, darkTheme
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
                    style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
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
                    description = stringResource(R.string.community_windows_images),
                    supportStatus = SupportStatus.MAYBE_SUPPORTED, darkTheme
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
                    ), SupportStatus.SUPPORTED, darkTheme
                )
            }
            item {
                ItemSupportEntry(
                    description = stringResource(
                        R.string.other_modern_gnu_linux_and_bsd_distributions_live_isos
                    ), SupportStatus.SUPPORTED, darkTheme
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
                    ), SupportStatus.SUPPORTED, darkTheme
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
                    description = "Damn Small Linux, Puppy Linux", SupportStatus.UNSUPPORTED,
                    darkTheme
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
                        ), start = startIndex, end = endIndex
                    )
                    addStringAnnotation(
                        tag = "URL",
                        annotation = "https://github.com/EtchDroid/EtchDroid/releases/tag/dmg-support",
                        start = startIndex, end = endIndex
                    )
                }
                ClickableText(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                    text = annotatedString, style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ), onClick = {
                        annotatedString.getStringAnnotations("URL", it, it)
                            .firstOrNull()
                            ?.let { stringAnnotation ->
                                uriHandler.openUri(stringAnnotation.item)
                            }
                    })
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
            UsbDevicePickerBottomSheet(
                onDismissRequest = { openBottomSheet = false },
                selectDevice = { },
                availableDevices = { viewModel.state.value.massStorageDevices }
            )
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
            UsbDevicePickerBottomSheet(
                onDismissRequest = { openBottomSheet = false },
                selectDevice = { },
                availableDevices = { emptySet() },
            )
        }
    }
}