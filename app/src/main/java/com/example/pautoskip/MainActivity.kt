package com.example.pautoskip

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.pautoskip.ui.theme.PAutoSkipTheme

class MainActivity : ComponentActivity() {

    private lateinit var spotifyManager: SpotifyManager

    // Mutable state to store connection status
    private val connectionStatus = mutableStateOf("Not Connected")
    private val isLoading = mutableStateOf(false)


    // Create an ActivityResultLauncher for requesting permissions
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If building your own apk from this repo, you will need to add your own client Id and redirect uri
        val clientId = "<client id>"
        val redirectUri = "<redirect uri>"

        // Initialize ActivityResultLauncher for permissions
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val foregroundServiceGranted = permissions[Manifest.permission.FOREGROUND_SERVICE] ?: false
            if (foregroundServiceGranted) {
                Log.e("MainActivity", "Foreground Service permission ALLOWED IN INIT REQ PERMS")
                startForegroundService()
            } else {
                Log.e("MainActivity", "Foreground Service permission denied")
            }


        }

        // Initialize SpotifyManager
        spotifyManager = SpotifyManager(this, clientId, redirectUri).apply {
            onConnectionStatusChanged = { status ->
                connectionStatus.value = status
                if (status == "Connected") {
                    // Check for both foreground service and notification permissions
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Check foreground service permission
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED) {
                            // Check notification permission for API level 33 and above
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                startForegroundService()
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                // Request notification permission if not granted
                                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                            } else {
                                startForegroundService()
                            }
                        } else {
                            // Request foreground service permission if not granted
                            requestPermissionLauncher.launch(arrayOf(Manifest.permission.FOREGROUND_SERVICE))
                        }
                    } else {
                        startForegroundService()
                    }
                }
                isLoading.value = false
            }
        }



        setContent {
            PAutoSkipTheme {

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        WelcomeScreen("PAutoSkip")

                        Spacer(modifier = Modifier.weight(1f))

                        Spacer(modifier = Modifier.height(24.dp))


                        CurrentStatus(connectionStatus.value)

                        Spacer(modifier = Modifier.weight(1f))

                        // Instruction text with (i) icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Please make sure Spotify is installed and you are logged in before pressing connect",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp)) // Reduced space below the welcome text

                        SpotifyInstalled(checkSpotifyInstallation())

                        // Center section
                        Spacer(modifier = Modifier.weight(.5f))

                        Spacer(modifier = Modifier.height(32.dp))

                        ConnectButton(onClick = {
                            if (!isLoading.value) {
                                isLoading.value = true
                                spotifyManager.connect()
                                Log.d("MainActivity", "Button Clicked")
                            }
                        }, checkSpotifyInstallation(), isLoading.value)


                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    fun checkSpotifyInstallation(): Boolean {
        val pm = packageManager
        return try {
            pm.getPackageInfo("com.spotify.music", 0)
            // Installed
            true
        } catch (e: PackageManager.NameNotFoundException) {
            // Not installed
            false
        }
    }

    private fun startForegroundService() {
        // Start the foreground service
        val serviceIntent = Intent(this, ForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopForegroundService() {
        // Stop the foreground service
        val serviceIntent = Intent(this, ForegroundService::class.java)
        stopService(serviceIntent)
    }

    private fun requestForegroundServicePermission() {
        requestPermissionLauncher.launch(arrayOf(Manifest.permission.FOREGROUND_SERVICE))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        spotifyManager.disconnect()
        stopForegroundService()
    }
}

@Composable
fun SpotifyInstalled(isInstalled: Boolean, modifier: Modifier = Modifier) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val icon = if (isInstalled) Icons.Default.CheckCircle else Icons.Default.Close
        val iconColor = if (isInstalled) Color.Green else Color.Red
        val message = if (isInstalled) "Spotify is installed" else "Spotify is not installed"

        Icon(
            imageVector = icon,
            contentDescription = if (isInstalled) "Checkmark" else "Red X",
            tint = iconColor,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun WelcomeScreen(text: String, modifier: Modifier = Modifier) {
    Text(
        text = "PAutoSkip",
        style = MaterialTheme.typography.displayLarge,
        color = Color.Green
    )
}

@Composable
fun CurrentStatus(text: String, modifier: Modifier = Modifier) {
    val (statusText, statusColor) = when (text) {
        "Connected" -> "Connected" to Color.Green
        "Not Connected" -> "Not Connected" to Color.Red
        else -> text to Color.Red
    }

    // Create an annotated string for different colors
    val annotatedString = buildAnnotatedString {
        append("Current Status: ")
        withStyle(style = SpanStyle(color = statusColor)) {
            append(statusText) // Apply color only to the status text
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
    )
}

@Composable
fun ConnectButton(onClick: () -> Unit, isInstalled: Boolean, isLoading: Boolean, modifier: Modifier = Modifier) {
    Button(
        enabled = isInstalled || !isLoading,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Green,
            contentColor = Color.Black
        ),
        modifier = modifier
            .width(200.dp)
            .height(50.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = if (isLoading) "Connecting..." else "Connect")
    }
}
