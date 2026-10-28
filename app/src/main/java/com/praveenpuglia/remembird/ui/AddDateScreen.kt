package com.praveenpuglia.remembird.ui

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praveenpuglia.remembird.Bucket
import com.praveenpuglia.remembird.EventDate
import com.praveenpuglia.remembird.Person
import com.praveenpuglia.remembird.R
import com.praveenpuglia.remembird.ui.theme.Handwriting
import com.praveenpuglia.remembird.ui.theme.Paper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Adding a date: pick a person, then the occasion and date, then hand off to the contacts app to save it.
 * [dated] holds the contacts that already have a date, so the rest can be listed first. [chosenAtStart] skips
 * picking a person (coming from their profile), and back then leaves the flow instead of returning to the picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDateScreen(
    people: List<Person>?,
    dated: Set<Long>,
    chosenAtStart: Long?,
    onBack: () -> Unit,
    onSave: (Person, Bucket, String?, EventDate) -> Unit,
) {
    var chosenId by rememberSaveable { mutableStateOf(chosenAtStart) }
    val chosen = people?.firstOrNull { it.id == chosenId }
    val back = { if (chosen != null && chosenAtStart == null) chosenId = null else onBack() }
    BackHandler(onBack = back)

    Scaffold(
        modifier = Modifier.paper(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.paper(),
                navigationIcon = {
                    val description = stringResource(R.string.back)
                    IconButton(onClick = back, modifier = Modifier.semantics { contentDescription = description }) {
                        Text("←", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                },
                title = { Highlighted(stringResource(R.string.remember), fontSize = 34) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
            )
        },
    ) { padding ->
        when {
            people == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            chosen == null -> PersonPicker(people, dated, padding) { chosenId = it.id }
            else -> DateForm(chosen, padding) { bucket, label, date -> onSave(chosen, bucket, label, date) }
        }
    }
}

@Composable
private fun PersonPicker(people: List<Person>, dated: Set<Long>, padding: PaddingValues, onPick: (Person) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches = remember(people, query) { people.filter { it.name.contains(query.trim(), ignoreCase = true) } }
    val (undated, withDates) = remember(matches, dated) { matches.partition { it.id !in dated } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        item {
            OutlinedTextField(
                query,
                { query = it },
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.add_search)) },
                singleLine = true,
            )
        }
        if (matches.isEmpty()) {
            item { Text(stringResource(R.string.add_no_match), Modifier.padding(24.dp), style = MaterialTheme.typography.bodyMedium) }
        }
        for ((title, group) in listOf(R.string.add_no_dates to undated, R.string.add_has_dates to withDates)) {
            if (group.isEmpty()) continue
            item(key = title) {
                Text(
                    stringResource(title),
                    Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                    fontFamily = Handwriting,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                )
            }
            items(group, key = { it.id }) { person ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable { onPick(person) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PolaroidAvatar(person.name, person.photoUri, 36.dp, tilt = 0f)
                    Text(
                        person.name,
                        fontFamily = Handwriting,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateForm(person: Person, padding: PaddingValues, onSave: (Bucket, String?, EventDate) -> Unit) {
    var bucket by rememberSaveable { mutableStateOf(Bucket.BIRTHDAY) }
    var label by rememberSaveable { mutableStateOf("") }
    // The picked day as the date picker gives it: midnight UTC, in millis.
    var pickedMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var noYear by rememberSaveable { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }

    val picked = pickedMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
    // Only birthdays may skip the year: Google Contacts refuses to save other dates without one.
    val withoutYear = bucket == Bucket.BIRTHDAY && noYear
    val locale = LocalConfiguration.current.locales[0]

    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PolaroidAvatar(person.name, person.photoUri, 64.dp, tilt = -3f)
            Text(person.name, fontFamily = Handwriting, fontWeight = FontWeight.Bold, fontSize = 30.sp)
        }

        Text(stringResource(R.string.add_occasion), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (kind in Bucket.entries) {
                val name = stringResource(
                    when (kind) {
                        Bucket.BIRTHDAY -> R.string.kind_birthday
                        Bucket.ANNIVERSARY -> R.string.kind_anniversary
                        Bucket.OTHER -> R.string.kind_other
                    },
                )
                PaperChip("${kind.emoji()} $name", selected = bucket == kind) { bucket = kind }
            }
        }
        if (bucket == Bucket.OTHER) {
            OutlinedTextField(
                label,
                { label = it },
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.add_label)) },
                placeholder = { Text(stringResource(R.string.add_label_hint)) },
                singleLine = true,
            )
        }

        OutlinedButton(
            onClick = { picking = true },
            Modifier.fillMaxWidth().heightIn(min = 56.dp),
            border = BorderStroke(1.5.dp, Paper.ink),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Paper.card, contentColor = Paper.ink),
        ) {
            val text = picked?.let {
                val pattern = DateFormat.getBestDateTimePattern(locale, if (withoutYear) "dMMMM" else "dMMMMyyyy")
                it.format(DateTimeFormatter.ofPattern(pattern, locale))
            }
            Text(
                text ?: stringResource(R.string.add_pick_date),
                fontFamily = if (text != null) Handwriting else null,
                fontWeight = FontWeight.Bold,
                fontSize = if (text != null) 24.sp else 16.sp,
            )
        }
        if (bucket == Bucket.BIRTHDAY) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(noYear, role = Role.Checkbox) { noYear = it },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(noYear, onCheckedChange = null)
                Text(stringResource(R.string.add_no_year), Modifier.padding(start = 12.dp))
            }
        }

        Text(stringResource(R.string.add_handoff), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = {
                val date = picked ?: return@Button
                onSave(bucket, label.trim().ifEmpty { null }, EventDate(date.monthValue, date.dayOfMonth, date.year.takeUnless { withoutYear }))
            },
            Modifier.fillMaxWidth().heightIn(min = 52.dp),
            enabled = picked != null,
        ) {
            Text(stringResource(R.string.add_save), fontWeight = FontWeight.Bold)
        }
    }

    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = pickedMillis, yearRange = 1900..LocalDate.now().year + 1)
        val colors = DatePickerDefaults.colors(containerColor = Paper.card)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = { pickedMillis = state.selectedDateMillis ?: pickedMillis; picking = false }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(android.R.string.cancel)) } },
            colors = colors,
        ) {
            DatePicker(state, colors = colors)
        }
    }
}
