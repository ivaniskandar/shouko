package xyz.ivaniskandar.shouko.ui.destination

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PermDeviceInformation
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import logcat.logcat
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3PreviewTheme
import xyz.ivaniskandar.shouko.util.createShareTextIntent

@Composable
fun PermissionSetup(
    title: String,
    permissionName: String,
    isRootAvailable: Boolean,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
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
            tint = MaterialTheme.colorScheme.primary
        )

        Text(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)

        val command = "pm grant ${context.packageName} $permissionName"
        if (isRootAvailable) {
            PermissionSetupRoot(command = command)
        } else {
            PermissionSetupNoRoot(command = command)
        }
    }

    // Permission listener
    LaunchedEffect(true) {
        launch(Dispatchers.Default) {
            while (context.checkSelfPermission(permissionName) != PackageManager.PERMISSION_GRANTED) {
                logcat { "Waiting for $permissionName permission" }
                // Check every 1 second
                delay(1000)
            }
            launch(Dispatchers.Main) {
                logcat { "$permissionName permission granted. Calling callback..." }
                onPermissionGranted()
            }
        }
    }
}

@Composable
fun PermissionSetupRoot(command: String) {
    Text(
        text = stringResource(R.string.shell_permission_setup_desc_root),
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge
    )
    Text(
        text = stringResource(R.string.shell_permission_setup_desc_extra),
        modifier = Modifier
            .padding(bottom = 24.dp)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge
    )

    FilledTonalButton(onClick = { Shell.su(command).submit() }) {
        Text(text = stringResource(R.string.button_continue))
    }
}

@Composable
fun PermissionSetupNoRoot(command: String) {
    val annotatedString = buildAnnotatedString {
        pushStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant))
        val str = stringResource(id = R.string.shell_permission_setup_desc_noroot)
        val txtToBeAnnotated = stringResource(R.string.shell_permission_setup_desc_noroot_adb_link_label)
        val startAnnotationIndex = str.indexOf(txtToBeAnnotated)
        val endAnnotationIndex = startAnnotationIndex + txtToBeAnnotated.length
        append(str)
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
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
        style = MaterialTheme.typography.bodyLarge
    ) {
        annotatedString
            .getStringAnnotations("URL", it, it)
            .firstOrNull()?.let { stringAnnotation ->
                uriHandler.openUri(stringAnnotation.item)
            }
    }

    val context = LocalContext.current
    val adbCommand = "adb shell $command"
    Card(
        modifier = Modifier
            .clickable { context.startActivity(createShareTextIntent(adbCommand)) }
            .fillMaxWidth(),
        containerColor = Color.Black,
        contentColor = Color.White
    ) {
        Text(
            text = adbCommand,
            modifier = Modifier.padding(12.dp),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }

    Text(
        modifier = Modifier.padding(top = 2.dp),
        text = stringResource(R.string.shell_permission_setup_command_card_caption),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )

    Text(
        text = stringResource(R.string.shell_permission_setup_desc_extra),
        modifier = Modifier.padding(vertical = 24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge
    )

    CircularProgressIndicator(modifier = Modifier.padding(24.dp))
}

@Preview
@Composable
fun ReadLogsPermissionSetupRootPreview() {
    ShoukoM3PreviewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PermissionSetup(
                title = stringResource(id = R.string.read_logs_permission_setup_title),
                permissionName = Manifest.permission.READ_LOGS,
                isRootAvailable = true,
                onPermissionGranted = {}
            )
        }
    }
}

@Preview
@Composable
fun ReadLogsPermissionSetupNoRootPreview() {
    ShoukoM3PreviewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PermissionSetup(
                title = stringResource(id = R.string.read_logs_permission_setup_title),
                permissionName = Manifest.permission.READ_LOGS,
                isRootAvailable = false,
                onPermissionGranted = {}
            )
        }
    }
}

@Preview
@Composable
fun WriteSettingsPermissionSetupNoRootPreview() {
    ShoukoM3PreviewTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PermissionSetup(
                title = stringResource(id = R.string.write_secure_settings_permission_setup_title),
                permissionName = Manifest.permission.WRITE_SECURE_SETTINGS,
                isRootAvailable = false,
                onPermissionGranted = {}
            )
        }
    }
}
