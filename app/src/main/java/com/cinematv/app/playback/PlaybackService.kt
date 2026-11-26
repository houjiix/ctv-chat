package com.cinematv.app.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.cinematv.app.CinemaApplication
import com.cinematv.app.data.WatchProgressEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
    private val cleanupJob = SupervisorJob()
    private val cleanupScope = CoroutineScope(cleanupJob + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private val progressTask = object : Runnable {
        override fun run() {
            saveCurrentProgress()
            handler.postDelayed(this, 5_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val sessionActivity = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val sessionBuilder = MediaSession.Builder(this, exoPlayer)
        if (sessionActivity != null) sessionBuilder.setSessionActivity(sessionActivity)
        mediaSession = sessionBuilder.build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                handler.removeCallbacks(progressTask)
                if (isPlaying) handler.post(progressTask) else saveCurrentProgress()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val mediaId = exoPlayer.currentMediaItem?.mediaId
                    if (!mediaId.isNullOrBlank()) {
                        serviceScope.launch {
                            (application as CinemaApplication).database.cinemaDao().removeProgress(mediaId)
                        }
                    }
                }
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private fun saveCurrentProgress() {
        val entry = progressSnapshot() ?: return
        serviceScope.launch {
            (application as CinemaApplication).database.cinemaDao().saveProgress(entry)
        }
    }

    private fun progressSnapshot(): WatchProgressEntity? {
        val currentPlayer = player ?: return null
        if (currentPlayer.playbackState == Player.STATE_ENDED) return null
        val current = currentPlayer.currentMediaItem ?: return null
        if (currentPlayer.duration <= 0L || currentPlayer.currentPosition < 5_000L) return null
        val metadata = current.mediaMetadata
        val entry = WatchProgressEntity(
            id = current.mediaId,
            title = metadata.title?.toString().orEmpty().ifBlank { "Video" },
            posterUrl = metadata.artworkUri?.toString(),
            playbackUrl = current.localConfiguration?.uri?.toString().orEmpty(),
            licenceUrl = metadata.extras?.getString(PlayerActivity.EXTRA_LICENCE_URL),
            licenceLabel = metadata.extras?.getString(PlayerActivity.EXTRA_LICENCE_LABEL),
            positionMs = currentPlayer.currentPosition,
            durationMs = currentPlayer.duration,
            updatedAt = System.currentTimeMillis()
        )
        return entry.takeIf { it.playbackUrl.isNotBlank() }
    }

    override fun onDestroy() {
        handler.removeCallbacks(progressTask)
        val finalProgress = progressSnapshot()
        if (finalProgress != null) {
            cleanupScope.launch {
                try {
                    (application as CinemaApplication).database.cinemaDao().saveProgress(finalProgress)
                } finally {
                    cleanupJob.cancel()
                }
            }
        } else {
            cleanupJob.cancel()
        }
        player?.release()
        player = null
        mediaSession?.release()
        mediaSession = null
        serviceJob.cancel()
        super.onDestroy()
    }
}
