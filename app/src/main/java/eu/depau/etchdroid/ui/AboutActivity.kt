package eu.depau.etchdroid.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import eu.depau.etchdroid.AppSettings
import eu.depau.etchdroid.BuildConfig
import eu.depau.etchdroid.R
import eu.depau.etchdroid.ui.composables.MainView
import eu.depau.etchdroid.ui.composables.coloredShadow
import eu.depau.etchdroid.utils.ktexts.activity
import eu.depau.etchdroid.utils.reviews.WriteReviewHelper

class AboutActivity : ComponentActivity() {
    private val mViewModel: ThemeViewModel by viewModels()
    private lateinit var mSettings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSettings = AppSettings(this).apply {
            addListener(mViewModel)
            mViewModel.refreshSettings(this)
        }

        setContent {
            MainView(viewModel = mViewModel) {
                AboutView(mViewModel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutView(viewModel: ThemeViewModel) {
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
            verticalArrangement = Arrangement.spacedBy(48.dp)
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
                    Text(stringResource(R.string.support_the_project))
                }
                val reviewHelper = remember { activity?.let { WriteReviewHelper(it) } }
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
            }
        }
    }
}


@Preview(showBackground = true, device = Devices.PIXEL_4, showSystemUi = true)
@Composable
fun AboutViewPreview() {
    val viewModel = remember { ThemeViewModel() }
    MainView(viewModel) {
        AboutView(viewModel)
    }
}