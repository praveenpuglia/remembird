package com.praveenpuglia.remembird.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praveenpuglia.remembird.R
import com.praveenpuglia.remembird.ui.theme.Handwriting
import com.praveenpuglia.remembird.ui.theme.Paper

/** What the app is, which version, and where its privacy policy, terms and source code live. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }

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
                title = { Highlighted(stringResource(R.string.about), fontSize = 34) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(painterResource(R.drawable.bird), null, Modifier.size(112.dp))
            Text(stringResource(R.string.app_name), fontFamily = Handwriting, fontWeight = FontWeight.Bold, fontSize = 40.sp)
            Text(stringResource(R.string.about_version, version), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.permission_body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)

            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                LinkRow(R.string.privacy_policy, R.string.privacy_policy_url)
                HorizontalDivider(color = Paper.ink.copy(alpha = 0.15f))
                LinkRow(R.string.terms, R.string.terms_url)
                HorizontalDivider(color = Paper.ink.copy(alpha = 0.15f))
                LinkRow(R.string.about_source, R.string.source_url)
            }
        }
    }
}

/** A full-width row that opens [url] in the browser. */
@Composable
private fun LinkRow(label: Int, url: Int) {
    val uriHandler = LocalUriHandler.current
    val target = stringResource(url)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) { uriHandler.openUri(target) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text("↗", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
