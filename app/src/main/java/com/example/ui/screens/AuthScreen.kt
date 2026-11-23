package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.theme.BatStateCrimson
import com.example.ui.theme.BatStateCrimsonDeep
import com.example.ui.theme.BatStateGoldBright
import com.example.ui.theme.StatusFailContainer
import com.example.ui.theme.StatusFailText
import com.example.ui.theme.WarmPaperBackground

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isAutoChecking by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = {
                isAutoChecking = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isAutoChecking = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = WarmPaperBackground,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, BatStateCrimson.copy(alpha = 0.28f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.img_brand_logo_banner),
                        contentDescription = "Civil Engineering App Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BatStateCrimsonDeep, BatStateCrimson)
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Traceable Civil Engineering Workspace",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "SI / US",
                                style = MaterialTheme.typography.labelMedium,
                                color = BatStateGoldBright,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Engineer Registration & Sync",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sign in with Google to register your CivilSpec workspace, sync projects and calculation revisions to Cloud Firestore, and export traceable reports.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FeatureBullet(
                        icon = Icons.Outlined.Architecture,
                        title = "6 Core Civil Modules",
                        body = "Units & Takeoff, Structural RC & Loads, Concrete & Rebar, Geotech Footings, Hydrology, and Surveying."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FeatureBullet(
                        icon = Icons.Outlined.Description,
                        title = "Glass-Box Equations & Revisions",
                        body = "Inspect formulas, substituted values, editable assumptions, design code references, and revision diffs."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FeatureBullet(
                        icon = Icons.Outlined.CloudDone,
                        title = "Isolated Project Storage",
                        body = "Projects, versions, and engineering notes are scoped strictly to your authenticated Firebase account."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (errorMessage != null) {
                        Surface(
                            color = StatusFailContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusFailText,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            isSigningIn = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                scope = scope,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isSigningIn = false
                                    errorMessage = msg
                                },
                                onAuthCancelled = {
                                    isSigningIn = false
                                }
                            )
                        },
                        enabled = !isSigningIn && !isAutoChecking,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isSigningIn || isAutoChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(if (isAutoChecking) "Checking session..." else "Signing in...")
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Login,
                                contentDescription = "Sign in with Google",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sign in with Google", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = "Safeguard",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Engineering Decision Support: Outputs require verification by a qualified Professional Engineer (PE/SE) prior to construction or permitting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureBullet(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(text = body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
