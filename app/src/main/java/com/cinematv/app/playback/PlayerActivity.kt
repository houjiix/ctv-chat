package com.cinematv.app.playback

import android.content.ComponentName
import android.os.Bundle
import android.app.Activity
import android.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.cinematv.app.R
import com.google.common.util.concurrent.ListenableFuture

class PlayerActivity : Activity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var errorDialog: AlertDialog? = null
    private lateinit var playerView: PlayerView
    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            if (!isFinishing && !isDestroyed) showPlaybackError()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerView = PlayerView(this).apply {
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        }
        setContentView(playerView)

        val mediaUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val mediaId = intent.getStringExtra(EXTRA_ID).orEmpty()
        val poster = intent.getStringExtra(EXTRA_POSTER)
        val licenceUrl = intent.getStringExtra(EXTRA_LICENCE_URL)
        val licenceLabel = intent.getStringExtra(EXTRA_LICENCE_LABEL)
        if (mediaUrl.isBlank() || mediaId.isBlank()) {
            showPlaybackError()
            return
        }

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                if (isFinishing || isDestroyed) return@addListener
                val mediaController = future.get()
                controller = mediaController
                mediaController.addListener(playerListener)
                playerView.player = mediaController
                val metadataExtras = Bundle().apply {
                    putString(EXTRA_LICENCE_URL, licenceUrl)
                    putString(EXTRA_LICENCE_LABEL, licenceLabel)
                }
                val metadata = MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtworkUri(poster?.let(android.net.Uri::parse))
                    .setExtras(metadataExtras)
                    .build()
                val mediaItem = MediaItem.Builder()
                    .setMediaId(mediaId)
                    .setUri(mediaUrl)
                    .setMediaMetadata(metadata)
                    .build()
                mediaController.setMediaItem(mediaItem)
                mediaController.prepare()
                val resumeAt = intent.getLongExtra(EXTRA_RESUME_MS, 0L)
                if (resumeAt > 0L) mediaController.seekTo(resumeAt)
                mediaController.play()
            } catch (_: Exception) {
                showPlaybackError()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun showPlaybackError() {
        if (isFinishing || isDestroyed || errorDialog?.isShowing == true) return
        val builder = AlertDialog.Builder(this)
            .setMessage(R.string.playback_error)
            .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
        if (controller != null) {
            builder.setPositiveButton(R.string.retry) { _, _ ->
                controller?.prepare()
                controller?.play()
            }
        } else {
            builder.setPositiveButton(android.R.string.ok) { _, _ -> finish() }
        }
        errorDialog = builder.create().also { it.show() }
    }

    override fun onStart() {
        super.onStart()
        controller?.let { playerView.player = it }
    }

    override fun onStop() {
        playerView.player = null
        // Keep the MediaSession alive when the app is backgrounded. Stop only
        // when the user is actually leaving the player screen (for example Back).
        if (isFinishing) controller?.pause()
        super.onStop()
    }

    override fun onDestroy() {
        errorDialog?.dismiss()
        errorDialog = null
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "playback_url"
        const val EXTRA_TITLE = "playback_title"
        const val EXTRA_ID = "playback_id"
        const val EXTRA_POSTER = "playback_poster"
        const val EXTRA_RESUME_MS = "playback_resume_ms"
        const val EXTRA_LICENCE_URL = "playback_licence_url"
        const val EXTRA_LICENCE_LABEL = "playback_licence_label"
    }
}
