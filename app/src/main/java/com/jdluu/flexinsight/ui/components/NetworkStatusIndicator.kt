package com.jdluu.flexinsight.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.core.network.NetworkState
import com.jdluu.flexinsight.ui.theme.FlexTheme

/**
 * Calibrated connectivity indicator shown only when offline.
 */
@Composable
fun NetworkStatusIndicator(
    networkState: NetworkState,
    modifier: Modifier = Modifier
) {
    if (networkState is NetworkState.Unavailable) {
        Surface(
            modifier = modifier,
            color = FlexTheme.colors.strainedContainer,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, FlexTheme.colors.strained.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = FlexTheme.colors.strained,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(id = R.string.network_status_no_connection),
                    style = MaterialTheme.typography.labelMedium,
                    color = FlexTheme.colors.onStrainedContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
