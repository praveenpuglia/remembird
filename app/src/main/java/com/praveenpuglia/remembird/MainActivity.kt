package com.praveenpuglia.remembird

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.praveenpuglia.remembird.ui.AboutScreen
import com.praveenpuglia.remembird.ui.AddDateScreen
import com.praveenpuglia.remembird.ui.OccasionsScreen
import com.praveenpuglia.remembird.ui.ProfileScreen
import com.praveenpuglia.remembird.ui.theme.RemembirdTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.toKotlinLocalDate

class MainActivity : ComponentActivity() {
    private var hasPermission by mutableStateOf(false)
    private var permanentlyDenied by mutableStateOf(false)
    private var occasions by mutableStateOf<List<Occasion>?>(null)
    private var today by mutableStateOf(java.time.LocalDate.now().toKotlinLocalDate())
    private var people by mutableStateOf<List<Person>?>(null)

    private val requestContacts = registerForActivityResult(RequestPermission()) { granted ->
        hasPermission = granted
        // After a second denial Android stops showing the dialog; only settings can turn access on.
        permanentlyDenied = !granted && !shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)
        if (granted) load()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always light paper, so always dark status and navigation bar icons.
        val paperBars = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = paperBars, navigationBarStyle = paperBars)
        setContent {
            RemembirdTheme {
                // Which screen is up, saved so a rotation doesn't drop you back on the list.
                var profileId by rememberSaveable { mutableStateOf<Long?>(null) }
                var profileName by rememberSaveable { mutableStateOf("") }
                var profilePhoto by rememberSaveable { mutableStateOf<String?>(null) }
                var adding by rememberSaveable { mutableStateOf(false) }
                var addingFor by rememberSaveable { mutableStateOf<Long?>(null) }
                var about by rememberSaveable { mutableStateOf(false) }
                when {
                    about -> AboutScreen(onBack = { about = false })
                    adding -> {
                        LaunchedEffect(Unit) { people = withContext(Dispatchers.IO) { ContactOccasions.people(contentResolver) } }
                        AddDateScreen(
                            people = people,
                            dated = occasions.orEmpty().mapTo(HashSet()) { it.contactId },
                            chosenAtStart = addingFor,
                            onBack = { adding = false },
                            onSave = { person, bucket, label, date -> if (saveInContacts(person, bucket, label, date)) adding = false },
                        )
                    }
                    profileId != null -> ProfileScreen(
                        name = profileName,
                        photoUri = profilePhoto,
                        occasions = occasions.orEmpty().filter { it.contactId == profileId },
                        today = today,
                        onBack = { profileId = null },
                        onRemember = { addingFor = profileId; adding = true },
                        onEdit = { profileId?.let(::editInContacts) },
                    )
                    else -> OccasionsScreen(
                        hasPermission = hasPermission,
                        permanentlyDenied = permanentlyDenied,
                        occasions = occasions,
                        today = today,
                        onGrant = ::askForContacts,
                        onAdd = { addingFor = null; adding = true },
                        onOpen = { profileId = it.contactId; profileName = it.name; profilePhoto = it.photoUri },
                        onAbout = { about = true },
                    )
                }
            }
        }
    }

    // Contacts can change while we're away (a new birthday, access revoked in settings), so refresh on every return.
    // onResume, not onStart: the contacts editor can open as a sheet over us, so we're never stopped while it's up.
    override fun onResume() {
        super.onResume()
        today = java.time.LocalDate.now().toKotlinLocalDate()
        hasPermission = checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) load()
    }

    private fun askForContacts() {
        if (permanentlyDenied) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        } else {
            requestContacts.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    // The contacts app saves it; onResume reloads when the user comes back, so the new date shows up straight away.
    private fun saveInContacts(person: Person, bucket: Bucket, label: String?, date: EventDate) =
        openInContacts(ContactOccasions.addDateIntent(person, bucket, label, date))

    private fun editInContacts(contactId: Long) = openInContacts(ContactOccasions.editIntent(contentResolver, contactId))

    private fun openInContacts(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.add_no_contacts_app, Toast.LENGTH_LONG).show()
        false
    }

    private fun load() {
        lifecycleScope.launch {
            occasions = withContext(Dispatchers.IO) { ContactOccasions.load(contentResolver) }
        }
    }
}
