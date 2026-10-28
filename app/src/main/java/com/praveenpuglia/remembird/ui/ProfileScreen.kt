package com.praveenpuglia.remembird.ui

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praveenpuglia.remembird.Occasion
import com.praveenpuglia.remembird.R
import com.praveenpuglia.remembird.daysUntil
import com.praveenpuglia.remembird.nextDate
import com.praveenpuglia.remembird.ui.theme.Handwriting
import com.praveenpuglia.remembird.ui.theme.Paper
import kotlinx.datetime.LocalDate
import java.time.format.DateTimeFormatter

/**
 * One person's page: their polaroid and every date saved for them, soonest first. Adding goes through
 * Remembird's own form; changing or removing a date happens in the contacts app, which owns the data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    name: String,
    photoUri: String?,
    occasions: List<Occasion>,
    today: LocalDate,
    onBack: () -> Unit,
    onRemember: () -> Unit,
    onEdit: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val soonestFirst = remember(occasions, today) { occasions.sortedBy { it.daysUntil(today) } }
    val locale = LocalConfiguration.current.locales[0]

    Scaffold(
        modifier = Modifier.paper(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.paper(),
                navigationIcon = {
                    val description = stringResource(R.string.back)
                    IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = description }) {
                        Text("←", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                },
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PolaroidAvatar(name, photoUri, 128.dp, tilt = -4f)
                Highlighted(name, fontSize = 36)
            }

            Text(stringResource(R.string.profile_dates), fontFamily = Handwriting, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            if (soonestFirst.isEmpty()) {
                Text(stringResource(R.string.profile_none, name), style = MaterialTheme.typography.bodyMedium)
            }
            for (occasion in soonestFirst) {
                val next = occasion.nextDate(today)
                val date = remember(occasion, locale) {
                    val pattern = DateFormat.getBestDateTimePattern(locale, if (occasion.year != null) "dMMMMyyyy" else "dMMMM")
                    // 2000 is a leap year, so a year-less Feb 29 still formats.
                    java.time.LocalDate.of(occasion.year ?: 2000, occasion.month, occasion.day).format(DateTimeFormatter.ofPattern(pattern, locale))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(occasion.bucket.emoji(), Modifier.clearAndSetSemantics {}, fontSize = 30.sp)
                    Column {
                        Text(occasion.describe(next), fontFamily = Handwriting, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text(
                            stringResource(R.string.profile_date_when, date, whenText(occasion.daysUntil(today))),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (occasion.daysUntil(today) == 0) Paper.pen else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Button(onClick = onRemember, Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 8.dp)) {
                Text("✍️", Modifier.clearAndSetSemantics {}.padding(end = 8.dp))
                Text(stringResource(R.string.profile_remember), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onEdit,
                Modifier.fillMaxWidth().heightIn(min = 52.dp),
                border = BorderStroke(1.5.dp, Paper.ink),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Paper.card, contentColor = Paper.ink),
            ) {
                Text(stringResource(R.string.profile_edit), fontWeight = FontWeight.Bold)
            }
            Text(
                stringResource(R.string.profile_edit_hint),
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
