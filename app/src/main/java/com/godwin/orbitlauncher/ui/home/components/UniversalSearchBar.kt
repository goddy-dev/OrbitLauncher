package com.godwin.orbitlauncher.ui.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact rounded search bar. Visually functional (typing works, cursor
 * blinks) but not wired to real search logic yet -- that's Phase 4
 * ("Search system") in the spec, which adds app/contact/settings/file/
 * web/voice search behind this same field.
 *
 * Built on BasicTextField rather than Material's OutlinedTextField so we
 * have full control over height -- OutlinedTextField enforces a 56dp
 * minimum height that can't be shrunk cleanly via modifiers alone.
 */
@Composable
fun UniversalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0x1AFFFFFF)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = "Search",
                tint = Color(0xFF888888),
                modifier = Modifier.padding(end = 8.dp)
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search the web or apps...",
                        color = Color(0xFF888888),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
