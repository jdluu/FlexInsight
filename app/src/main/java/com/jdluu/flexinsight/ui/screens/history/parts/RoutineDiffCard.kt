package com.jdluu.flexinsight.ui.screens.history.parts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.domain.model.RoutineComparison

@Composable
fun RoutineDiffCard(
    comparison: RoutineComparison?,
    modifier: Modifier = Modifier
) {
    if (comparison == null) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.history_routine_diff_title, comparison.routineName),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(text = comparison.summary, fontSize = 13.sp)
            if (comparison.improvements.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.history_routine_improvements),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                comparison.improvements.forEach {
                    Text("• $it", fontSize = 12.sp)
                }
            }
            if (comparison.regressions.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.history_routine_regressions),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
                comparison.regressions.forEach {
                    Text("• $it", fontSize = 12.sp)
                }
            }
        }
    }
}
