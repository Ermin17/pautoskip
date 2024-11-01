package com.example.pautoskip

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.pautoskip.ui.theme.PAutoSkipTheme

class MainActivity : ComponentActivity() {

    private lateinit var spotifyManager: SpotifyManager

    // Mutable state to store connection status
    private val connectionStatus = mutableStateOf("Not Connected");

    // Create an ActivityResultLauncher for requesting permissions
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Spotify credentials and initialize the manager
        // val clientId = BuildConfig.SPOTIFY_CLIENT_ID
        // val redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI

        val clientId = "<your client id>"
        val redirectUri = "<your redirect uri"

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
                                Log.e("MainActivity", "All permissions granted; starting foreground service.")
                                startForegroundService()
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                // Request notification permission if not granted
                                Log.e("MainActivity", "Requesting notification permission.")
                                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                            } else {
                                Log.e("MainActivity", "Starting foreground service on older Android version.")
                                startForegroundService()
                            }
                        } else {
                            // Request foreground service permission if not granted
                            Log.e("MainActivity", "Requesting foreground service permission.")
                            requestPermissionLauncher.launch(arrayOf(Manifest.permission.FOREGROUND_SERVICE))
                        }
                    } else {
                        Log.e("MainActivity", "Starting foreground service on older Android version.")
                        startForegroundService()
                    }
                }
            }
        }


        setContent {
            PAutoSkipTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Arrange items in a vertical column
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top, // Align items towards the top
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp)) // Space above the welcome text

                        WelcomeScreen("Welcome to PAutoSkip")

                        Spacer(modifier = Modifier.height(16.dp)) // Reduced space below the welcome text

                        // Center section
                        Spacer(modifier = Modifier.weight(1f)) // Push the status and button down

                        CurrentStatus(connectionStatus.value)

                        Spacer(modifier = Modifier.height(32.dp)) // Reduced space between status and button

                        ConnectButton(onClick = {
                            spotifyManager.connect()
                            Log.d("MainActivity", "Button Clicked")
                        })

                        Spacer(modifier = Modifier.weight(1f)) // Optional spacer to keep things centered
                    }
                }
            }
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
        // Any other logic you want to add on start
        Log.d("MainActivity", "onStart called")
    }

    override fun onStop() {
        super.onStop()
        Log.d("MainActivity", "onStop called")
    }

    override fun onDestroy() {
        super.onDestroy()
        spotifyManager.disconnect()
        stopForegroundService()
    }
}

@Composable
fun WelcomeScreen(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
    )
}

@Composable
fun CurrentStatus(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
    )
}

@Composable
fun ConnectButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Green,
            contentColor = Color.Black
        ),
        modifier = modifier
    ) {
        Text(text = "Connect")
    }
}



@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    PAutoSkipTheme {
        // Preview of the layout
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top, // Align items towards the top
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Space for preview
            WelcomeScreen("Welcome to PAutoSkip")
            Spacer(modifier = Modifier.height(16.dp)) // Space below the welcome text
            CurrentStatus("Not Connected")
            Spacer(modifier = Modifier.height(8.dp)) // Space between status and button
            //ConnectButton()
        }
    }
}
