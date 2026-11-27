package com.jdluu.flexinsight.ui.screens.workoutdetail.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailTopBar(
    onNavigateBack: () -> Unit,
    onShareClick: () -> Unit,
    shareEnabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.workout_detail_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.content_desc_back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                if (shareEnabled) {
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(id = R.string.content_desc_share),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
fun WorkoutDetailSessionHeader(
    title: String,
    date: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (date.isNotBlank()) {
            Text(
                text = date,
                style = MonoMetricMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WorkoutDetailHeader(
    title: String,
    date: String,
    onNavigateBack: () -> Unit,
    onShareClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        WorkoutDetailTopBar(
            onNavigateBack = onNavigateBack,
            onShareClick = onShareClick
        )
        WorkoutDetailSessionHeader(
            title = title,
            date = date
        )
    }
}
