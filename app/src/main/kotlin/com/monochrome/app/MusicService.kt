package com.monochrome.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.monochrome.app.Constants.ACTION_NEXT
import com.monochrome.app.Constants.ACTION_PAUSE
import com.monochrome.app.Constants.ACTION_PLAY
import com.monochrome.app.Constants.ACTION_PREVIOUS
import com.monochrome.app.Constants.ACTION_UPDATE_STATE
import com.monochrome.app.Constants.EXTRA_IS_PLAYING
import java.net.HttpURLConnection
import java.net.URL

class MusicService : MediaBrowserServiceCompat() {

    companion object {
        const val CHANNEL_ID          = "monochrome_playback"
        const val NOTIFICATION_ID     = 1001
        const val ACTION_UPDATE_TRACK = "com.monochrome.app.UPDATE_TRACK"
        const val EXTRA_TRACK_NAME    = "track_name"
    }

    private var mediaSession: MediaSessionCompat? = null
    private var currentTrack = ""
    private var currentArtist = "Monochrome"
    private var currentArtUrl = ""
    private var currentBitmap: Bitmap? = null
    private var isPlaying = false

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                mediaSession?.controller?.transportControls?.pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        var needsUpdate = false
        when (intent?.action) {
            ACTION_UPDATE_TRACK -> {
                val name = intent.getStringExtra(EXTRA_TRACK_NAME)?.takeIf { it.isNotBlank() } ?: "Monochrome"
                val artist = intent.getStringExtra("artist")?.takeIf { it.isNotBlank() } ?: "Monochrome"
                val artUrl = intent.getStringExtra("art_url") ?: ""
                
                if (name != currentTrack || artist != currentArtist || artUrl != currentArtUrl) {
                    currentTrack = name
                    currentArtist = artist
                    currentArtUrl = artUrl
                    needsUpdate = true
                    
                    if (artUrl.isNotBlank()) {
                        downloadArt(artUrl)
                    } else {
                        currentBitmap = null
                        updateMetadataAndNotify()
                    }
                }
            }
            ACTION_UPDATE_STATE -> {
                val playing = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                if (playing != isPlaying) {
                    isPlaying = playing
                    needsUpdate = true
                }
            }
        }
        
        if (needsUpdate || intent?.action == null) {
            updateMetadataAndNotify()
        }

        return START_STICKY
    }

    private fun downloadArt(url: String) {
        Thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.connect()
                val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                if (bitmap != null && url == currentArtUrl) {
                    currentBitmap = bitmap
                    runOnUiThread { updateMetadataAndNotify() }
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicService", "Art download failed: ${e.message}")
            }
        }.start()
    }

    private fun runOnUiThread(action: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(action)
    }

    private fun updateMetadataAndNotify() {
        updateMetadata()
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Failed to start foreground service", e)
        }
    }

    private fun updateMetadata() {
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTrack)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Monochrome")
        
        currentBitmap?.let {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, it)
        }
        
        mediaSession?.setMetadata(builder.build())

        val state = PlaybackStateCompat.Builder()
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                1.0f
            )
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .build()
        mediaSession?.setPlaybackState(state)
    }

    override fun onDestroy() {
        try { unregisterReceiver(noisyReceiver) } catch (_: Exception) { }
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot = BrowserRoot("root", null)

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) = result.sendResult(mutableListOf())

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "Monochrome").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = sendBroadcast(Intent(ACTION_PLAY).setPackage(packageName))
                override fun onPause() = sendBroadcast(Intent(ACTION_PAUSE).setPackage(packageName))
                override fun onSkipToNext() = sendBroadcast(Intent(ACTION_NEXT).setPackage(packageName))
                override fun onSkipToPrevious() = sendBroadcast(Intent(ACTION_PREVIOUS).setPackage(packageName))
            })
        }
        sessionToken = mediaSession?.sessionToken
    }

    private fun buildNotification(): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val openPi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).apply { this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP }, flags)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(currentBitmap)
            .setContentTitle(currentTrack)
            .setContentText(currentArtist)
            .setContentIntent(openPi)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(isPlaying)

        builder.addAction(android.R.drawable.ic_media_previous, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
        if (isPlaying) builder.addAction(android.R.drawable.ic_media_pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE))
        else builder.addAction(android.R.drawable.ic_media_play, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY))
        builder.addAction(android.R.drawable.ic_media_next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))

        builder.setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession?.sessionToken).setShowActionsInCompactView(0, 1, 2))

        return builder.build()
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Monochrome Playback", NotificationManager.IMPORTANCE_LOW).apply {
                    setShowBadge(false)
                    setSound(null, null)
                    description = "Playback controls for Monochrome"
                }
            )
        }
    }
}
