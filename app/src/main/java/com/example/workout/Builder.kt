package com.example.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val SEPARATOR = "\n"

/**
 * Builds or edits a custom workout from the built-in exercises.
 * Pass [existing] to edit one, or [copyFrom] to start from a built-in workout.
 */
@Composable
fun WorkoutBuilder(existing: CustomWorkout?, copyFrom: Day?, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    var title by rememberSaveable {
        mutableStateOf(existing?.title ?: copyFrom?.let { "${it.title} (copy)" } ?: "")
    }
    // The chosen exercise names in order, kept as one string so it survives rotation.
    var order by rememberSaveable {
        mutableStateOf(
            when {
                existing != null -> existing.exercises
                copyFrom != null -> copyFrom.steps.filterIsInstance<Step.Work>().map { it.exercise.name }
                else -> emptyList()
            }.joinToString(SEPARATOR)
        )
    }
    val names = if (order.isEmpty()) emptyList() else order.split(SEPARATOR)
    val groups = remember { Days.exercisePool.groupBy { it.group } }
    val canSave = title.isNotBlank() && names.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        QuietButton("Cancel", onClose)
        Text(
            if (existing != null) "Edit workout" else "New workout",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(40) },
            label = { Text("Name", fontSize = 13.sp) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
        Text(
            "Tap moves to add them in order. Tap again to remove.",
            fontSize = 12.sp,
            color = muted,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            groups.forEach { (group, list) ->
                item(key = "group-$group") {
                    Text(
                        group.uppercase(),
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        color = muted,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                items(list, key = { it.name }) { exercise ->
                    val position = names.indexOf(exercise.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = if (position >= 0) names - exercise.name else names + exercise.name
                                order = next.joinToString(SEPARATOR)
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (position >= 0) "${position + 1}" else "",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(exercise.name, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text(
                            if (exercise.isReps) "${exercise.reps} reps" else "${exercise.workSeconds(Storage.timing.value.work)} s",
                            fontSize = 12.sp,
                            color = muted
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            PrimaryButton(
                if (names.isEmpty()) "Save" else "Save (${names.size} moves)",
                onClick = {
                    Storage.saveCustom(
                        CustomWorkout(
                            id = existing?.id ?: System.currentTimeMillis().toString(),
                            title = title.trim(),
                            exercises = names
                        )
                    )
                    onClose()
                },
                enabled = canSave
            )
        }
    }
}
