package com.offgrid.compositor

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import java.io.File

private const val PLAYBACK_TAG = "OffgridCompositor"

internal fun MainActivity.showPlayback() {
        val file = recordingFile()
        if (!file.exists() || file.length() == 0L) {
            showMessage(R.string.record_failed, MainActivity.ButtonMode.LIVE)
            return
        }
        playRequested = true
        playbackBackCallback.isEnabled = true
        terminalPanel.setHiddenForPlayback(true)
        playbackBackdrop.visibility = View.VISIBLE
        playbackView.visibility = View.VISIBLE
        // Phase 1.2: no playback bar on the stage. The hint goes to Phone 2; a tap or Back returns.
        val diamond = diamondInVideo ?: terminalPanel.testDiamondOn
        sayStatus(getString(if (diamond) R.string.playback_hint else R.string.playback_hint_plain))
        playbackRotation = readVideoRotation(file)
        if (playbackSurface != null) {
            startPlayer(file)
        }
    }

internal fun MainActivity.hidePlayback() {
        playRequested = false
        playbackBackCallback.isEnabled = false
        releasePlayer()
        playbackRotation = 0
        playbackView.rotation = 0f
        playbackView.translationX = 0f
        playbackView.translationY = 0f
        playbackView.setTransform(Matrix())
        val params = playbackView.layoutParams
        params.width = FrameLayout.LayoutParams.MATCH_PARENT
        params.height = FrameLayout.LayoutParams.MATCH_PARENT
        playbackView.layoutParams = params
        playbackBackdrop.visibility = View.GONE
        playbackView.visibility = View.GONE
        val wasPlaying = terminalPanel.hiddenForPlayback
        terminalPanel.setHiddenForPlayback(false)
        if (wasPlaying && hasCameraPermission() && !recording) showLivePicture()
    }

internal fun MainActivity.startPlayer(file: File) {
        val surface = playbackSurface ?: return
        releasePlayer()
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            playbackFd = fd
            player.setDataSource(fd.fileDescriptor, 0, file.length())
            player.setSurface(surface)
            player.isLooping = true
            player.setVolume(1f, 1f)
            player.setOnVideoSizeChangedListener { mp, width, height ->
                if (mediaPlayer !== mp) {
                    return@setOnVideoSizeChangedListener
                }
                videoWidth = width
                videoHeight = height
                fitPlayback()
            }
            player.setOnPreparedListener { mp ->
                if (mediaPlayer !== mp) {
                    return@setOnPreparedListener
                }
                videoWidth = mp.videoWidth
                videoHeight = mp.videoHeight
                fitPlayback()
                mp.start()
            }
            player.setOnErrorListener { mp, what, extra ->
                Log.e(PLAYBACK_TAG, "Playback failed $what $extra")
                if (mediaPlayer === mp) {
                    runOnUiThread {
                        hidePlayback()
                        showMessage(R.string.record_failed, MainActivity.ButtonMode.LIVE)
                    }
                }
                true
            }
            player.prepareAsync()
        } catch (error: Exception) {
            Log.e(PLAYBACK_TAG, "Playback failed", error)
            hidePlayback()
            showMessage(R.string.record_failed, MainActivity.ButtonMode.LIVE)
        }
    }

internal fun MainActivity.releasePlayer() {
        mediaPlayer?.let { player ->
            player.setOnPreparedListener(null)
            player.setOnErrorListener(null)
            player.setOnVideoSizeChangedListener(null)
            try {
                player.stop()
            } catch (error: IllegalStateException) {
                Log.w(PLAYBACK_TAG, "Playback was already stopped")
            }
            player.release()
        }
        mediaPlayer = null
        playbackFd?.close()
        playbackFd = null
    }

internal fun MainActivity.fitPlayback() {
        val parent = playbackView.parent as? View ?: return
        val parentW = parent.width
        val parentH = parent.height
        if (parentW == 0 || parentH == 0 || videoWidth == 0 || videoHeight == 0) {
            return
        }
        val swap = playbackRotation == 90 || playbackRotation == 270
        val layoutW = if (swap) parentH else parentW
        val layoutH = if (swap) parentW else parentH
        val params = playbackView.layoutParams as FrameLayout.LayoutParams
        if (params.width != layoutW || params.height != layoutH) {
            params.width = layoutW
            params.height = layoutH
            params.gravity = Gravity.START or Gravity.TOP
            playbackView.layoutParams = params
        }
        playbackView.pivotX = layoutW / 2f
        playbackView.pivotY = layoutH / 2f
        playbackView.rotation = playbackRotation.toFloat()
        playbackView.translationX = (parentW - layoutW) / 2f
        playbackView.translationY = (parentH - layoutH) / 2f
        val viewAspect = layoutW.toFloat() / layoutH.toFloat()
        val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
        val scaleX: Float
        val scaleY: Float
        if (videoAspect > viewAspect) {
            scaleX = 1f
            scaleY = viewAspect / videoAspect
        } else {
            scaleX = videoAspect / viewAspect
            scaleY = 1f
        }
        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY, layoutW / 2f, layoutH / 2f)
        playbackView.setTransform(matrix)
    }

internal fun MainActivity.readVideoRotation(file: File): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
        } catch (error: RuntimeException) {
            Log.w(PLAYBACK_TAG, "Could not read which way the video is turned", error)
            0
        } finally {
            retriever.release()
        }
    }

internal fun MainActivity.playbackSurfaceListener(): TextureView.SurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
            playbackSurface = Surface(st)
            if (playRequested) {
                startPlayer(recordingFile())
            }
        }

        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
            fitPlayback()
        }

        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
            releasePlayer()
            playbackSurface?.release()
            playbackSurface = null
            return true
        }

        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
    }

