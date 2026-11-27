package com.jdluu.flexinsight.ui.screens.history.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryHeader(
    activeFilter: String = "All Time",
    onFilterClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.history_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            actions = {
                if (activeFilter != "All Time") {
                    PlateStatusPill(
                        label = activeFilter,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = stringResource(id = R.string.history_filter_dialog_title),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
    }
}

@Composable
fun TabSelector(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        stringResource(id = R.string.history_tab_summary),
        stringResource(id = R.string.history_tab_exercises),
        stringResource(id = R.string.history_tab_compare)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, FlexTheme.colors.hairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    onClick = { onTabSelected(index) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
