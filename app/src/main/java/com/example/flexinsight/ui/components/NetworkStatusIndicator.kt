package com.example.flexinsight.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.core.network.NetworkState
import com.example.flexinsight.ui.theme.*

/**
 * Composable for displaying network connectivity status
 */
@Composable
fun NetworkStatusIndicator(
    networkState: NetworkState,
    modifier: Modifier = Modifier
) {
    when (networkState) {
        is NetworkState.Unavailable -> {
            Surface(
                modifier = modifier,
                color = RedAccent.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "●",
                        color = RedAccent,
                        fontSize = 8.sp
                    )
                    Text(
                        text = "No internet connection",
                        color = RedAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        is NetworkState.Available -> {
            // Don't show anything when online
        }
        is NetworkState.Unknown -> {
            // Don't show anything when state is unknown
        }
    }
}
