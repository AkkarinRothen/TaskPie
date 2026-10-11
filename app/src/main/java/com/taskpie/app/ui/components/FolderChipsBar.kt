package com.taskpie.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskpie.app.model.FolderItem
import com.taskpie.app.ui.theme.LightBorder
import com.taskpie.app.ui.theme.LightSurface
import com.taskpie.app.ui.theme.PrimaryBlue
import com.taskpie.app.ui.theme.PrimaryBlueLight

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderChipsBar(
    folders: List<FolderItem>,
    selectedFolder: String,
    onSelectFolder: (String) -> Unit,
    onNewFolderClick: () -> Unit,
    onEditFolderClick: (FolderItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp)
) {
    LazyRow(
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        // "Todas" chip
        item {
            val isSelected = selectedFolder.equals("Todas", ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) PrimaryBlueLight else LightSurface,
                border = CardDefaults.outlinedCardBorder(enabled = !isSelected),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(
                        onClick = { onSelectFolder("Todas") }
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Todas",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        // User folders
        items(folders, key = { it.id }) { folder ->
            val isSelected = selectedFolder.equals(folder.name, ignoreCase = true)
            val folderColor = runCatching {
                Color(android.graphics.Color.parseColor(folder.colorHex))
            }.getOrDefault(PrimaryBlue)

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) folderColor.copy(alpha = 0.15f) else LightSurface,
                border = CardDefaults.outlinedCardBorder(enabled = !isSelected),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(
                        onClick = { onSelectFolder(folder.name) },
                        onLongClick = { onEditFolderClick(folder) }
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(folderColor)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) folderColor else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        // "+ Nueva Carpeta" Button
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(onClick = onNewFolderClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nueva Carpeta",
                        modifier = Modifier.size(15.dp),
                        tint = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Carpeta",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
