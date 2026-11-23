package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.firebase.UserProfile
import com.example.domain.calc.StandardFamily
import com.example.domain.calc.UnitSystem
import com.example.ui.UiState
import com.example.ui.theme.MonoEquationStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportPreviewScreen(
    title: String,
    reportText: String,
    onBack: () -> Unit,
    onCopied: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("report_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Calculation Report Preview") }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("CivilSpec Report", reportText))
                            onCopied()
                        },
                        modifier = Modifier.weight(1f).testTag("copy_report_button")
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy")
                    }
                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "CivilSpec Calculation Sheet - $title")
                                putExtra(Intent.EXTRA_TEXT, reportText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export Calculation Report"))
                        },
                        modifier = Modifier.weight(1f).testTag("share_report_button")
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export / Share")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth()
            ) {
                Text(
                    text = reportText,
                    style = MonoEquationStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    profileState: UiState<UserProfile?>,
    userEmail: String,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSaveProfile: (String, String, String, UnitSystem, StandardFamily) -> Unit,
    onSignOut: () -> Unit
) {
    BackHandler { onBack() }
    val existing = (profileState as? UiState.Success)?.data
    var displayName by remember(existing) { mutableStateOf(existing?.displayName ?: "") }
    var org by remember(existing) { mutableStateOf(existing?.organization ?: "") }
    var license by remember(existing) { mutableStateOf(existing?.licenseOrTitle ?: "") }
    var unit by remember(existing) { mutableStateOf(UnitSystem.fromCode(existing?.preferredUnitSystem)) }
    var std by remember(existing) { mutableStateOf(StandardFamily.fromCode(existing?.preferredStandard)) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("profile_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Engineer Profile & Defaults") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Signed-in Firebase Account", style = MaterialTheme.typography.labelMedium)
                        Text(userEmail.ifBlank { "Google Account" }, style = MaterialTheme.typography.titleSmall)
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Engineer Name (shown on reports) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )
                OutlinedTextField(
                    value = org,
                    onValueChange = { org = it },
                    label = { Text("Firm / University / Team") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = license,
                    onValueChange = { license = it },
                    label = { Text("Role / Credential (e.g. EIT, PE, Field Eng)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Default Unit System", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    UnitSystem.entries.forEachIndexed { i, u ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(i, UnitSystem.entries.size),
                            selected = unit == u,
                            onClick = { unit = u }
                        ) { Text(u.label) }
                    }
                }

                Text("Default Design Standard Family", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    StandardFamily.entries.forEachIndexed { i, s ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(i, StandardFamily.entries.size),
                            selected = std == s,
                            onClick = { std = s }
                        ) { Text(if (s == StandardFamily.ACI_ASCE) "ACI / ASCE" else "Eurocode") }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { onSaveProfile(displayName, org, license, unit, std) },
                    enabled = !isBusy && displayName.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_button")
                ) {
                    Text("Save Preferences")
                }

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}
