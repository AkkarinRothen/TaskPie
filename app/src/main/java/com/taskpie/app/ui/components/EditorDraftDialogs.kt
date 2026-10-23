package com.taskpie.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun EditorDraftRecoveryDialog(
    itemType: String,
    onContinue: () -> Unit,
    onDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Hay un borrador pendiente") },
        text = { Text("Encontramos cambios sin guardar en esta $itemType. ¿Quieres continuar donde lo dejaste?") },
        confirmButton = {
            TextButton(onClick = onContinue) { Text("Continuar") }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) { Text("Descartar") }
        }
    )
}

@Composable
fun InvalidEditorDraftDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("No se pudo recuperar el borrador") },
        text = { Text("El borrador estaba dañado y se descartó. Puedes empezar de nuevo.") },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendido") }
        }
    )
}
