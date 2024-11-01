package com.example.pautoskip

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.example.pautoskip.ForegroundService


import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;


class SpotifyManager(private val context: Context, private val clientId: String, private val redirectUri: String) {

    private var spotifyAppRemote: SpotifyAppRemote? = null

    // Callback for connection status change
    var onConnectionStatusChanged: ((String) -> Unit)? = null

    fun connect() {
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                onConnectionStatusChanged?.invoke("Connected")
                Log.d("SpotifyManager", "Connected! Yay!")
                connected()

                val serviceIntent = Intent(context, ForegroundService::class.java)
                context.startForegroundService(serviceIntent)
            }

            override fun onFailure(throwable: Throwable) {
                onConnectionStatusChanged?.invoke("Connection Failed")
                Log.e("SpotifyManager", "Connection failed: ${throwable.message}", throwable)
            }
        })
    }

    private fun connected() {
        // Listen for track changes to advertisement then skip to end
        spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
            // Log or use the playerContext data
            val trackUri = playerState.track?.uri
            val durationValue = playerState.track?.duration
            val isAd = trackUri?.contains("spotify:ad:") == true

            // Only seek if it's an ad and we have a valid duration
            if (isAd && durationValue != null) {
                Log.d("AdvertisementFound", "Ad Found, seeking to $durationValue")
                spotifyAppRemote?.playerApi?.seekTo(durationValue)
            }

            // Log.d("PlayerState", playerState.toString());

        }?.setErrorCallback { throwable ->
            Log.e("PLayerState", "Error in subscribing to player state", throwable)
        }

    }

    fun disconnect() {
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
            onConnectionStatusChanged?.invoke("Disconnected")
        }
    }
}