package com.praveenpuglia.remembird

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.praveenpuglia.remembird.ui.OccasionsScreen
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
                OccasionsScreen(
                    hasPermission = hasPermission,
                    permanentlyDenied = permanentlyDenied,
                    occasions = occasions,
                    today = today,
                    onGrant = ::askForContacts,
                )
            }
        }
    }

    // Contacts can change while we're away (a new birthday, access revoked in settings), so refresh on every return.
    override fun onStart() {
        super.onStart()
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

    private fun load() {
        lifecycleScope.launch {
            occasions = withContext(Dispatchers.IO) { ContactOccasions.load(contentResolver) }
        }
    }
}
