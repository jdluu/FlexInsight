package com.example.flexinsight.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Skeleton for the FeaturedWorkoutCard on the Dashboard
 */
@Composable
fun FeaturedWorkoutCardSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .shimmerEffect()
    )
}

/**
 * Skeleton for the stats boxes on the Dashboard
 */
@Composable
fun StatsSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shimmerEffect()
            )
        }
    }
}

/**
 * Skeleton for a list of workout items (e.g., in History)
 */
@Composable
fun WorkoutHistoryListSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shimmerEffect()
            )
        }
    }
}

/**
 * Skeleton for the Planner screen
 */
@Composable
fun PlannerSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect()) // Header
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect()) // Goal card
        Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect()) // Calendar
        repeat(3) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect()) // Workout list
        }
    }
}

/**
 * Skeleton for the Recovery screen
 */
@Composable
fun RecoverySkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect()) // Header
        Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp)).shimmerEffect()) // Score card
        repeat(2) {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect()) // Sections
        }
    }
}
