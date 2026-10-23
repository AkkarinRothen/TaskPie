package com.taskpie.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskpie.app.ui.theme.LightBorder
import com.taskpie.app.ui.theme.LightTextSecondary
import com.taskpie.app.ui.theme.PrimaryBlue

@Composable
fun CreationStepIndicator(currentStep: Int) {
    val stepNames = listOf("Detalles", "Contenido", "Revisión")
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Paso ${currentStep + 1} de ${stepNames.size}",
                style = MaterialTheme.typography.labelMedium.copy(color = LightTextSecondary)
            )
            Text(
                text = stepNames[currentStep],
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
        LinearProgressIndicator(
            progress = { (currentStep + 1) / stepNames.size.toFloat() },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = PrimaryBlue,
            trackColor = LightBorder
        )
    }
}
