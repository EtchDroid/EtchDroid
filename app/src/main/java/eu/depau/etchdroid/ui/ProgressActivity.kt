package eu.depau.etchdroid.ui

import android.Manifest.permission
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
import eu.depau.etchdroid.getStartJobIntent
import eu.depau.etchdroid.massstorage.EtchDroidUsbMassStorageDevice.Companion.isMassStorageDevice
import eu.depau.etchdroid.massstorage.UsbMassStorageDeviceDescriptor
import eu.depau.etchdroid.massstorage.doesNotMatch
import eu.depau.etchdroid.service.WorkerService
import eu.depau.etchdroid.ui.composables.GifImage
import eu.depau.etchdroid.ui.composables.KeepScreenOn
import eu.depau.etchdroid.ui.composables.MainView
import eu.depau.etchdroid.utils.broadcastReceiver
import eu.depau.etchdroid.utils.exception.InitException
import eu.depau.etchdroid.utils.exception.MissingPermissionException
import eu.depau.etchdroid.utils.exception.UsbCommunicationException
import eu.depau.etchdroid.utils.exception.VerificationFailedException
import eu.depau.etchdroid.utils.exception.base.FatalException
import eu.depau.etchdroid.utils.exception.base.RecoverableException
import eu.depau.etchdroid.utils.exception.base.isUnplugged
import eu.depau.etchdroid.utils.ktexts.activity
import eu.depau.etchdroid.utils.ktexts.broadcastLocally
import eu.depau.etchdroid.utils.ktexts.getFileName
import eu.depau.etchdroid.utils.ktexts.registerExportedReceiver
import eu.depau.etchdroid.utils.ktexts.startForegroundServiceCompat
import eu.depau.etchdroid.utils.ktexts.toHRSize
import eu.depau.etchdroid.utils.ktexts.toast
import eu.depau.etchdroid.utils.ktexts.usbDevice
import eu.depau.etchdroid.utils.reviews.WriteReviewHelper
import kotlinx.coroutines.delay
import me.jahnen.libaums.libusbcommunication.LibusbError
import me.jahnen.libaums.libusbcommunication.LibusbException

private const val TAG = "ProgressActivity"
private const val LAST_NOTIFICATION_TIMEOUT = 10 * 1000L

class ProgressActivity : ComponentActivity() {
    private lateinit var mSettings: AppSettings
    private val mViewModel: ProgressActivityViewModel by viewModels()

    private val mNotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private var mPermissionAsked = false
    private val mNotificationPermissionRequester =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            mViewModel.setNotificationsPermission(granted)
        }

    private val mBroadcastReceiver = broadcastReceiver { intent ->
        mViewModel.updateFromIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, IntentFilter().apply {
            addAction(Intents.JOB_PROGRESS)
            addAction(Intents.ERROR)
            addAction(Intents.FINISHED)
        })
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
    }

    private fun refreshNotificationsPermission() {
        mViewModel.setNotificationsPermission(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) mNotificationManager.areNotificationsEnabled()
            else true
        )
    }

    private fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        if (mNotificationManager.areNotificationsEnabled()) return refreshNotificationsPermission()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !mPermissionAsked && shouldShowRequestPermissionRationale(
                permission.POST_NOTIFICATIONS
            )) {
            mPermissionAsked = true
            return mNotificationPermissionRequester.launch(permission.POST_NOTIFICATIONS)
        }

        startActivity(Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("app_package", packageName)
                putExtra("app_uid", applicationInfo.uid)
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println(
            "ProgressActivity running in thread ${Thread.currentThread().name} (${Thread.currentThread().id})"
        )

        mSettings = AppSettings(this).apply {
            mViewModel.refreshSettings(this)
        }
        refreshNotificationsPermission()
        mViewModel.updateFromIntent(intent)

        setContent {
            MainView(mViewModel) {
                val appState by mViewModel.state.collectAsState()

                if (appState.jobState == JobState.IN_PROGRESS) {
                    LaunchedEffect(key1 = appState.lastNotificationTime) {
                        delay(LAST_NOTIFICATION_TIMEOUT)
                        if (System.currentTimeMillis() - appState.lastNotificationTime >= LAST_NOTIFICATION_TIMEOUT) {
                            mViewModel.setTimeoutError()
                        }
                    }
                }

                when (appState.jobState) {
                    JobState.IN_PROGRESS, JobState.RECOVERABLE_ERROR -> {
                        BackHandler { println("Ignoring back button") }
                        JobInProgressView(mViewModel, requestNotificationsPermission = {
                            requestNotificationsPermission()
                        }, dismissNotificationsBanner = {
                            mSettings.showNotificationsBanner = false
                        }, cancelVerification = {
                            Intent(Intents.SKIP_VERIFY).broadcastLocally(this@ProgressActivity)
                        })
                    }

                    JobState.SUCCESS -> {
                        BackHandler { finish() }
                        SuccessView()
                    }

                    JobState.FATAL_ERROR -> {
                        FatalErrorView(
                            exception = appState.exception!! as FatalException, imageUri = appState.sourceUri!!,
                            jobId = appState.jobId, device = appState.destDevice!!
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun JobInProgressView(
    viewModel: ProgressActivityViewModel,
    requestNotificationsPermission: () -> Unit = {},
    dismissNotificationsBanner: () -> Unit = {},
    cancelVerification: () -> Unit = {},
) {
    val uiState by viewModel.state.collectAsState()

    KeepScreenOn()
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (title, subtitle, graphic, progressBox, notificationsBanner) = createRefs()

        Text(
            modifier = Modifier.constrainAs(title) {
                top.linkTo(parent.top, 48.dp)
                start.linkTo(parent.start, 16.dp)
                end.linkTo(parent.end, 16.dp)
            },
            text = if (uiState.isVerifying) stringResource(
                R.string.verifying_image
            ) else stringResource(
                R.string.writing_image
            ),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
        )
        Text(
            modifier = Modifier.constrainAs(subtitle) {
                top.linkTo(title.bottom, 8.dp)
                start.linkTo(parent.start, 16.dp)
                end.linkTo(parent.end, 16.dp)
            }, text = stringResource(R.string.please_avoid_using_your_device),
            style = MaterialTheme.typography.titleLarge
        )

        var clickLastTime by remember { mutableStateOf(0L) }
        var clickCount by remember { mutableStateOf(0) }
        var easterEgg by remember { mutableStateOf(false) }

        Column(modifier = Modifier
            .constrainAs(graphic) {
                centerTo(parent)
            }
            .fillMaxWidth()
            .clickable {
                val now = System.currentTimeMillis()
                if (now - clickLastTime < 500) {
                    clickCount++
                    if (clickCount >= 5) {
                        clickCount = 0
                        easterEgg = !easterEgg
                    }
                } else {
                    clickCount = 0
                }
                clickLastTime = now
            }) {
            if (easterEgg) {
                GifImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 56.dp, start = 16.dp, end = 16.dp),
                    gifRes = if (uiState.isVerifying) R.drawable.win_xp_verify else R.drawable.win_xp_copy
                )
            } else {
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(256.dp)
                ) {
                    val (usbDrive, imagesRow, magnifyingGlass) = createRefs()

                    val numberOfImages = 3
                    val imageWidth = 64
                    val density = LocalDensity.current
                    var rowSize by remember { mutableStateOf(IntSize.Zero) }
                    val repeatWidth by remember(rowSize) {
                        derivedStateOf {
                            val dpWidth = with(density) { rowSize.width.toDp() }.value
                            (dpWidth - (imageWidth * numberOfImages)) / (numberOfImages - 1) + imageWidth
                        }
                    }

                    val anchorTransition = rememberInfiniteTransition(label = "anchorTransition")
                    val anchor by anchorTransition.animateValue(
                        initialValue = if (uiState.isVerifying) 100.dp else (100 + repeatWidth).dp,
                        targetValue = if (uiState.isVerifying) (100 + repeatWidth).dp else 100.dp,
                        typeConverter = TwoWayConverter(convertToVector = { AnimationVector1D(it.value) },
                            convertFromVector = { it.value.dp }), animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart
                        ), label = "anchor"
                    )

                    Row(modifier = Modifier
                        .constrainAs(imagesRow) {
                            centerVerticallyTo(usbDrive)
                            if (uiState.isVerifying) {
                                start.linkTo(usbDrive.start, anchor)
                            } else {
                                end.linkTo(usbDrive.end, anchor)
                            }
                        }
                        .padding(bottom = 32.dp)
                        .fillMaxWidth()
                        .onSizeChanged {
                            rowSize = it
                        }, horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 0 until numberOfImages) Icon(
                            imageVector = ImageVector.vectorResource(
                                id = R.drawable.ic_disk_image_large
                            ),
                            modifier = Modifier.size(imageWidth.dp),
                            contentDescription = "",
                        )
                    }

                    val bgColor = MaterialTheme.colorScheme.background
                    Icon(
                        imageVector = ImageVector.vectorResource(
                            id = R.drawable.ic_usb_stick_large
                        ),
                        modifier = Modifier
                            .constrainAs(usbDrive) {
                                centerTo(parent)
                            }
                            .padding(
                                if (uiState.isVerifying) PaddingValues(
                                    end = 128.dp
                                ) else PaddingValues(start = 128.dp)
                            )
                            .drawBehind {
                                drawRect(bgColor, topLeft = with(density) { Offset(88.dp.toPx(), 22.dp.toPx()) },
                                    size = with(density) { DpSize(80.dp, 180.dp).toSize() })
                            }
                            .size(256.dp),
                        contentDescription = "",
                    )

                    if (uiState.isVerifying) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                id = R.drawable.ic_magnifying_glass
                            ),
                            modifier = Modifier
                                .constrainAs(magnifyingGlass) {
                                    top.linkTo(imagesRow.top, 24.dp)
                                    start.linkTo(usbDrive.start, 224.dp)
                                }
                                .size(96.dp),
                            contentDescription = "",
                        )
                    }
                }
            }
        }

        if (uiState.showNotificationsBanner && !uiState.notificationsPermission) {
            Card(modifier = Modifier
                .constrainAs(notificationsBanner) {
                    bottom.linkTo(progressBox.top, 16.dp)
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                }
                .padding(16.dp), elevation = CardDefaults.elevatedCardElevation(6.dp)) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.would_you_like_to_be_notified),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        OutlinedButton(onClick = dismissNotificationsBanner) {
                            Text(text = stringResource(R.string.no_thanks))
                        }
                        Button(onClick = requestNotificationsPermission) {
                            Text(text = stringResource(R.string.sure))
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.constrainAs(progressBox) {
            start.linkTo(parent.start, 16.dp)
            end.linkTo(parent.end, 16.dp)
            bottom.linkTo(parent.bottom, 16.dp)
        }) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                val context = LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (uiState.isVerifying) stringResource(R.string.verifying) else stringResource(
                            R.string.copying
                        )
                    )
                    Text(
                        text = " " + if (uiState.isVerifying) uiState.destDevice?.name
                        else uiState.sourceUri?.getFileName(context),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (uiState.isVerifying) stringResource(
                            R.string.against
                        ) else stringResource(R.string.to)
                    )
                    Text(
                        text = " " + if (uiState.isVerifying) uiState.sourceUri?.getFileName(
                            context
                        )
                        else uiState.destDevice?.name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    text = if (uiState.percent >= 0) "${uiState.processedBytes.toHRSize()} / ${uiState.totalBytes.toHRSize()}" + " ${if (uiState.isVerifying) "verified" else "written"}, ${uiState.speed.toHRSize()}/s"
                    else stringResource(R.string.getting_ready), style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }

            if (uiState.percent >= 0) {
                LinearProgressIndicator(
                    progress = uiState.percent / 100f, modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (uiState.isVerifying) {
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp), onClick = cancelVerification
                ) {
                    Text(text = stringResource(R.string.skip_verification))
                }
            }
        }
    }

    if (uiState.jobState == JobState.RECOVERABLE_ERROR && uiState.exception != null && uiState.exception is RecoverableException) {
        AutoJobRestarter(
            uiState.sourceUri!!, uiState.jobId, uiState.isVerifying, uiState.destDevice!!, uiState.processedBytes
        )
        ReconnectUsbDriveDialog(exception = uiState.exception as RecoverableException)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuccessView() {
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (stuffBox, card) = createRefs()
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.animated_check)
        )

        Column(modifier = Modifier
            .constrainAs(stuffBox) {
                centerTo(parent)
            }
            .padding(32.dp), verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.image_written_successfully),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
            )


            val progress by animateLottieCompositionAsState(composition)
            val lottieDynamicProperties = rememberLottieDynamicProperties(
                rememberLottieDynamicProperty(
                    property = LottieProperty.COLOR_FILTER, value = PorterDuffColorFilter(
                        MaterialTheme.colorScheme.primary.toArgb(), PorterDuff.Mode.SRC_ATOP
                    ), keyPath = arrayOf("**")
                )
            )
            LottieAnimation(
                composition, progress = { progress }, modifier = Modifier.size(256.dp),
                dynamicProperties = lottieDynamicProperties
            )

            val activity = LocalContext.current.activity
            val reviewHelper = remember { activity?.let { WriteReviewHelper(it) } }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                if (reviewHelper != null) {
                    OutlinedButton(onClick = { reviewHelper.launchReviewFlow() }) {
                        Text(
                            text = if (reviewHelper.isGPlayFlavor) stringResource(
                                R.string.write_a_review
                            )
                            else stringResource(R.string.star_on_github)
                        )
                    }
                }
                OutlinedButton(onClick = {
                    activity?.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://etchdroid.depau.eu/donate/"))
                    )
                }) {
                    Text(stringResource(R.string.support_the_project))
                }
                val context = LocalContext.current
                OutlinedButton(onClick = {
                    context.startActivity(Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    activity?.finish()
                }) {
                    Text(stringResource(R.string.write_another_image))
                }
            }
        }
        Card(
            modifier = Modifier
                .constrainAs(card) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.got_an_unsupported_drive_notification),
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                )

                val uriHandler = LocalUriHandler.current
                val annotatedString = buildAnnotatedString {
                    val learnMoreStr = stringResource(R.string.learn_what_it_means)
                    val str = stringResource(R.string.it_s_safe_to_ignore, learnMoreStr)
                    val startIndex = str.indexOf(learnMoreStr)
                    val endIndex = startIndex + learnMoreStr.length
                    append(str)
                    addStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline
                        ), start = startIndex, end = endIndex
                    )
                    addStringAnnotation(
                        tag = "URL", annotation = "https://etchdroid.depau.eu/broken_usb/", start = startIndex,
                        end = endIndex
                    )
                }
                ClickableText(modifier = Modifier.fillMaxWidth(), text = annotatedString,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ), onClick = {
                        annotatedString.getStringAnnotations("URL", it, it).firstOrNull()?.let { stringAnnotation ->
                            uriHandler.openUri(stringAnnotation.item)
                        }
                    })
            }
        }
    }
}

@Composable
fun FatalErrorView(
    exception: FatalException,
    imageUri: Uri,
    jobId: Int,
    device: UsbMassStorageDeviceDescriptor,
) {
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val stuffBox = createRef()

        Column(modifier = Modifier
            .constrainAs(stuffBox) {
                centerTo(parent)
            }
            .padding(32.dp), verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            val context = LocalContext.current
            Text(
                text = stringResource(R.string.there_was_an_error),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
            )
            Text(
                text = exception.getUiMessage(context),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Icon(
                imageVector = ImageVector.vectorResource(
                    id = R.drawable.ic_write_to_usb_failed_large
                ),
                contentDescription = stringResource(R.string.error),
                modifier = Modifier.size(256.dp),
            )

            val activity = LocalContext.current.activity

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                val btn: @Composable (onClick: () -> Unit, content: @Composable RowScope.() -> Unit) -> Unit =
                    { onClick, content ->
                        if (exception is VerificationFailedException) {
                            OutlinedButton(onClick = onClick, content = content)
                        } else {
                            Button(onClick = onClick, content = content)
                        }
                    }

                btn(onClick = {
                    context.startActivity(Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    activity?.finish()
                }) {
                    Text(text = stringResource(R.string.start_over))
                }

                if (exception is VerificationFailedException && activity != null) {
                    Button(onClick = {
                        val serviceIntent = getStartJobIntent(
                            imageUri, device, jobId, 0, false, activity, WorkerService::class.java
                        )
                        Log.d(TAG, "Starting service with intent: $serviceIntent")
                        activity.startForegroundServiceCompat(serviceIntent)
                    }) {
                        Text(stringResource(R.string.try_again))
                    }
                }
            }

        }
    }
}

@Composable
fun RecoverableExceptionExplanationCard(
    exception: RecoverableException,
    modifier: Modifier = Modifier,
) {
    val title = when {
        exception.isUnplugged -> stringResource(R.string.i_did_not_unplug_it)
        else -> stringResource(R.string.how_did_this_happen)
    }
    val body = when {
        exception.isUnplugged -> stringResource(R.string.perhaps_your_usb_port_is_dirty)
        exception is UsbCommunicationException || exception is InitException -> stringResource(
            R.string.usb_drives_are_unreliable
        )

        else -> stringResource(R.string.this_is_unexpected_it_s_probably_a_bug_please_report_it)
    }

    Card(
        modifier = modifier.fillMaxWidth(), elevation = CardDefaults.elevatedCardElevation(3.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconnectUsbDriveDialog(exception: RecoverableException) {
    AlertDialog(
        onDismissRequest = { },
    ) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                modifier = Modifier.padding(all = 24.dp)
            ) {
                val context = LocalContext.current
                Text(
                    text = exception.getUiMessage(context),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.to_recover_unplug), modifier = Modifier
                        .padding(bottom = 24.dp)
                        .align(Alignment.CenterHorizontally)
                        .weight(weight = 1f, fill = false), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                val vectorRes = ImageVector.vectorResource(R.drawable.unplug_reconnect_accept)
                Image(
                    imageVector = vectorRes, contentDescription = stringResource(
                        R.string.representation_of_the_required_steps
                    ), contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(AlertDialogDefaults.iconContentColor), modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(vectorRes.defaultWidth / vectorRes.defaultHeight)
                        .padding(horizontal = 32.dp)
                )

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )

                RecoverableExceptionExplanationCard(exception = exception)
            }
        }
    }
}

@Composable
fun AutoJobRestarter(
    imageUri: Uri,
    jobId: Int,
    isVerifying: Boolean,
    expectedDevice: UsbMassStorageDeviceDescriptor,
    resumeOffset: Long,
) {
    val context = LocalContext.current
    val activity = remember { context.activity } ?: run {
        Log.e(TAG, "AutoJobRestarter: activity not found")
        return
    }
    val usbManager = remember { context.getSystemService(Context.USB_SERVICE) as UsbManager }
    val forceFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT
    } else {
        0
    }
    val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE or forceFlag
    else 0
    val pendingIntent = remember {
        PendingIntent.getBroadcast(
            activity, 0, Intent(Intents.USB_PERMISSION), pendingIntentFlags
        )
    }

    fun onUsbAttached(usbDevice: UsbDevice) {
        expectedDevice.findMatchingForNew(usbDevice) ?: return
        usbManager.requestPermission(usbDevice, pendingIntent)
    }

    val broadcastReceiver = remember {
        broadcastReceiver { intent ->
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val usbDevice = intent.usbDevice!!
                    if (!usbDevice.isMassStorageDevice) return@broadcastReceiver
                    if (usbDevice doesNotMatch expectedDevice.usbDevice) {
                        activity.toast(
                            context.getString(R.string.plug_in_the_same_usb), Toast.LENGTH_SHORT
                        )
                    } else {
                        onUsbAttached(usbDevice)
                    }
                }

                Intents.USB_PERMISSION -> {
                    val usbDevice = intent.usbDevice!!
                    if (!usbDevice.isMassStorageDevice) return@broadcastReceiver
                    val msd = expectedDevice.findMatchingForNew(usbDevice)
                    if (msd == null) {
                        activity.toast(
                            context.getString(R.string.plug_in_the_same_usb), Toast.LENGTH_SHORT
                        )
                        return@broadcastReceiver
                    }

                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (!granted) {
                        activity.toast(
                            context.getString(
                                R.string.permission_denied_for_usb_device, usbDevice.deviceName
                            )
                        )
                    } else {
                        activity.toast(context.getString(R.string.usb_device_reconnected_resuming))
                        val serviceIntent = getStartJobIntent(
                            imageUri, msd, jobId, resumeOffset, isVerifying, activity, WorkerService::class.java
                        )
                        Log.d(TAG, "Starting service with intent: $serviceIntent")
                        activity.startForegroundServiceCompat(serviceIntent)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        Log.d(TAG, "Registering broadcast receiver")
        activity.apply {
            registerExportedReceiver(broadcastReceiver, IntentFilter(Intents.USB_PERMISSION))
            registerExportedReceiver(broadcastReceiver, IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED))
            for (usbDevice in usbManager.deviceList.values) onUsbAttached(usbDevice)
        }
        onDispose {
            Log.d(TAG, "Unregistering broadcast receiver")
            activity.unregisterReceiver(broadcastReceiver)
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4, showSystemUi = true)
@Composable
fun SuccessViewPreview() {
    val viewModel = remember { ProgressActivityViewModel() }
    MainView(viewModel) {
        SuccessView()
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4, showSystemUi = true)
@Composable
fun ErrorViewPreview() {
    val viewModel = remember { ProgressActivityViewModel() }
    MainView(viewModel) {
        FatalErrorView(
            exception = VerificationFailedException(), Uri.EMPTY, 1337, UsbMassStorageDeviceDescriptor()
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
fun ExceptionCardsPreview() {
    val viewModel = remember { ProgressActivityViewModel() }
    MainView(viewModel) {
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                RecoverableExceptionExplanationCard(
                    exception = UsbCommunicationException(
                        LibusbException("yolo", LibusbError.NO_DEVICE)
                    )
                )
            }
            item {
                RecoverableExceptionExplanationCard(
                    exception = UsbCommunicationException()
                )
            }
            item {
                RecoverableExceptionExplanationCard(
                    exception = InitException("yolo")
                )
            }
            item {
                RecoverableExceptionExplanationCard(
                    exception = MissingPermissionException()
                )
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4, showSystemUi = true)
@Composable
fun ReconnectUsbDriveDialogPreview() {
    val viewModel = remember { ProgressActivityViewModel() }
    MainView(viewModel) {
        ReconnectUsbDriveDialog(
            UsbCommunicationException(LibusbException("yolo", LibusbError.NO_DEVICE))
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4, showSystemUi = true)
@Composable
fun ProgressViewPreview() {
    val viewModel = remember { ProgressActivityViewModel() }
    val progressTransition = rememberInfiniteTransition()
    val progress by progressTransition.animateFloat(
        initialValue = 0f, targetValue = 2f, animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing), repeatMode = RepeatMode.Restart
        )
    )
    LaunchedEffect(progress) {
        viewModel.setState(
            viewModel.state.value.copy(
                percent = ((progress * 100) % 100).toInt(),
                processedBytes = ((progress * 1000000000) % 1000000000).toLong(), totalBytes = 1000000000,
                speed = 10000000f, isVerifying = progress > 1
            )
        )
    }

    MainView(viewModel) {
        JobInProgressView(
            viewModel,
            dismissNotificationsBanner = {
                viewModel.setState(
                    viewModel.state.value.copy(showNotificationsBanner = false)
                )
            },
        )
    }
}