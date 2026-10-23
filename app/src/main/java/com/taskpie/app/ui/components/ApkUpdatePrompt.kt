package com.taskpie.app.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.taskpie.app.updates.ApkRelease
import com.taskpie.app.updates.DirectApkUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ApkUpdatePrompt() {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val scope = rememberCoroutineScope()
    var release by remember { mutableStateOf<ApkRelease?>(null) }
    var downloadedApk by remember { mutableStateOf<java.io.File?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activity) {
        release = DirectApkUpdater.checkForUpdate(activity)
    }

    val availableRelease = release ?: return

    AlertDialog(
        onDismissRequest = { if (!isBusy) release = null },
        title = { Text("TaskPie ${availableRelease.versionName} disponible") },
        text = {
            Column {
                Text(availableRelease.notes.ifBlank { "Hay una versión nueva de TaskPie." })
                if (isBusy) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
                message?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                onClick = {
                    scope.launch {
                        message = null
                        val existingApk = downloadedApk
                        if (existingApk != null) {
                            if (DirectApkUpdater.canRequestInstall(activity)) {
                                DirectApkUpdater.openInstaller(activity, existingApk)
                            } else {
                                message = "Permite instalaciones para TaskPie en Ajustes y vuelve aquí para continuar."
                                DirectApkUpdater.requestInstallPermission(activity)
                            }
                            return@launch
                        }

                        isBusy = true
                        try {
                            val apk = DirectApkUpdater.downloadRelease(activity, availableRelease)
                            downloadedApk = apk
                            if (DirectApkUpdater.canRequestInstall(activity)) {
                                DirectApkUpdater.openInstaller(activity, apk)
                            } else {
                                message = "Descarga lista. Permite instalaciones para TaskPie en Ajustes y vuelve aquí para continuar."
                                DirectApkUpdater.requestInstallPermission(activity)
                            }
                        } catch (error: Exception) {
                            message = if (error.message == "APK checksum does not match") {
                                "No se pudo verificar la descarga, así que no se instalará. Inténtalo más tarde."
                            } else {
                                "No se pudo preparar la actualización. Comprueba tu conexión e inténtalo de nuevo."
                            }
                        } finally {
                            isBusy = false
                        }
                    }
                }
            ) {
                Text(
                    when {
                        isBusy -> "Descargando…"
                        downloadedApk != null -> "Instalar actualización"
                        else -> "Descargar actualización"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { release = null }, enabled = !isBusy) {
                Text("Más tarde")
            }
        }
    )
}
