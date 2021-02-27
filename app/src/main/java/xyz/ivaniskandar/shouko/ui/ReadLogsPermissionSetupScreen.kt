package xyz.ivaniskandar.shouko.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PermDeviceInformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ui.theme.ShoukoTheme
import xyz.ivaniskandar.shouko.util.isRootAvailable

private fun sendTextContent(context: Context, string: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, string)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}

@Composable
fun ReadLogsPermissionSetup(onPermissionGranted: () -> Unit) {
    if (isRootAvailable) {
        ReadLogsPermissionSetupRoot()
    } else {
        ReadLogsPermissionSetupNoRoot()
    }

    // Permission listener
    val context = LocalContext.current
    rememberCoroutineScope().launch(Dispatchers.Default) {
        while (context.checkSelfPermission(Manifest.permission.READ_LOGS) != PackageManager.PERMISSION_GRANTED) {
            Timber.d("Waiting for READ_LOGS permission")
            // Check every 1 second
            delay(1000)
        }
        launch(Dispatchers.Main) {
            Timber.d("READ_LOGS permission granted. Calling callback...")
            onPermissionGranted()
        }
    }
}

@Composable
fun ReadLogsPermissionSetupRoot() {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.PermDeviceInformation,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .padding(bottom = 12.dp),
            tint = MaterialTheme.colors.primary
        )

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
            Text(
                text = stringResource(R.string.read_logs_permission_setup_title),
                style = MaterialTheme.typography.h6
            )
        }

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = stringResource(R.string.read_logs_permission_setup_desc_root),
                modifier = Modifier.padding(vertical = 24.dp),
                textAlign = TextAlign.Justify,
                style = MaterialTheme.typography.body1
            )
        }

        val coroutineScope = rememberCoroutineScope()
        val packageName = LocalContext.current.packageName
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    Shell.su("pm grant $packageName ${Manifest.permission.READ_LOGS}").submit()
                }
            }
        ) {
            Text(text = stringResource(R.string.button_continue))
        }
    }
}

@Composable
fun ReadLogsPermissionSetupNoRoot() {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.PermDeviceInformation,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .padding(bottom = 12.dp),
            tint = MaterialTheme.colors.primary
        )

        Text(
            text = stringResource(R.string.read_logs_permission_setup_title),
            style = MaterialTheme.typography.h6
        )

        val annotatedString = buildAnnotatedString {
            pushStyle(SpanStyle(color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium)))
            val str = stringResource(id = R.string.read_logs_permission_setup_desc_noroot)
            val txtToBeAnnotated = stringResource(R.string.read_logs_permission_setup_desc_noroot_adb_link_label)
            val startAnnotationIndex = str.indexOf(txtToBeAnnotated)
            val endAnnotationIndex = startAnnotationIndex + txtToBeAnnotated.length
            append(str)
            addStyle(
                style = SpanStyle(
                    color = MaterialTheme.colors.primary,
                    textDecoration = TextDecoration.Underline
                ),
                start = startAnnotationIndex,
                end = endAnnotationIndex
            )
            addStringAnnotation(
                tag = "URL",
                annotation = stringResource(R.string.setup_adb_link),
                start = startAnnotationIndex,
                end = endAnnotationIndex
            )
        }
        val uriHandler = LocalUriHandler.current
        ClickableText(
            text = annotatedString,
            modifier = Modifier.padding(vertical = 24.dp),
            style = MaterialTheme.typography.body1
        ) {
            annotatedString
                .getStringAnnotations("URL", it, it)
                .firstOrNull()?.let { stringAnnotation ->
                    uriHandler.openUri(stringAnnotation.item)
                }
        }

        val context = LocalContext.current
        val adbCommand = "adb shell pm grant ${context.packageName} ${Manifest.permission.READ_LOGS}"
        Card(
            modifier = Modifier
                .clickable { sendTextContent(context, adbCommand) }
                .fillMaxWidth(),
            backgroundColor = Color.Black,
            contentColor = Color.White,
            elevation = 0.dp
        ) {
            Text(
                text = adbCommand,
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = stringResource(R.string.read_logs_permission_setup_command_card_caption),
                style = MaterialTheme.typography.caption
            )
        }

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = stringResource(R.string.read_logs_permission_setup_desc_noroot_extra),
                modifier = Modifier.padding(vertical = 24.dp),
                style = MaterialTheme.typography.body1
            )
        }

        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
    }
}

@Preview
@Composable
fun ReadLogsPermissionSetupRootPreview() {
    ShoukoTheme {
        Surface(color = MaterialTheme.colors.background) {
            ReadLogsPermissionSetupRoot()
        }
    }
}

@Preview
@Composable
fun ReadLogsPermissionSetupNoRootPreview() {
    ShoukoTheme {
        Surface(color = MaterialTheme.colors.background) {
            ReadLogsPermissionSetupNoRoot()
        }
    }
}