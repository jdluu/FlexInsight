package com.jdluu.flexinsight.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun FeaturedWorkoutCardSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(172.dp)
            .clip(MaterialTheme.shapes.large)
            .shimmerEffect()
    )
}

@Composable
fun StatsSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) { index ->
            key(index) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .clip(MaterialTheme.shapes.large)
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
fun WorkoutHistoryListSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) { index ->
            key(index) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .clip(MaterialTheme.shapes.large)
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
fun PlannerSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(MaterialTheme.shapes.large)
                .shimmerEffect()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(MaterialTheme.shapes.medium)
                .shimmerEffect()
        )
        repeat(3) { index ->
            key(index) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(MaterialTheme.shapes.large)
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
fun RecoverySkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(MaterialTheme.shapes.large)
                .shimmerEffect()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(MaterialTheme.shapes.large)
                .shimmerEffect()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(MaterialTheme.shapes.large)
                .shimmerEffect()
        )
    }
}
