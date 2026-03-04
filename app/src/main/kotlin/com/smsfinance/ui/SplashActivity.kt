package com.smsfinance.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.smsfinance.R

/**
 * Full-screen video splash screen.
 * - Suppresses Android 12+ system splash icon via windowSplashScreenBackground trick
 * - Uses FrameLayout container so video stays centered when scaled to fill screen
 * - Falls back to MainActivity immediately on any error
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : FragmentActivity(), SurfaceHolder.Callback {

    private lateinit var surfaceView: SurfaceView
    private lateinit var container: FrameLayout
    private var mediaPlayer: MediaPlayer? = null
    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Suppress Android 12+ system splash screen immediately
        window.setBackgroundDrawableResource(android.R.color.black)

        super.onCreate(savedInstanceState)

        // True full-screen — hide status bar + nav bar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )

        // FrameLayout container — dark navy background fills any letterbox gaps
        container = FrameLayout(this).apply {
            setBackgroundColor(0xFF05142A.toInt())
        }

        // SurfaceView for video — starts filling container, will be scaled in onPrepared
        surfaceView = SurfaceView(this)
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
        container.addView(surfaceView, lp)
        setContentView(container)

        surfaceView.holder.addCallback(this)
    }

    // ── SurfaceHolder.Callback ────────────────────────────────────────────────

    override fun surfaceCreated(holder: SurfaceHolder) {
        playVideo(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        releasePlayer()
    }

    // ── Video playback ────────────────────────────────────────────────────────

    private fun playVideo(holder: SurfaceHolder) {
        try {
            val uri = Uri.parse("android.resource://" + packageName + "/" + R.raw.splash_video)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setDisplay(holder)
                isLooping = false
                // Let us handle scaling manually for true fill
                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)

                setOnPreparedListener { mp ->
                    mp.start()
                    scaleVideoToFill(mp)
                }
                setOnCompletionListener { goToMain() }
                setOnErrorListener { _, _, _ -> goToMain(); true }
                prepareAsync()
            }
        } catch (e: Exception) {
            goToMain()
        }
    }

    private fun scaleVideoToFill(mp: MediaPlayer) {
        val vW = mp.videoWidth.coerceAtLeast(1).toFloat()
        val vH = mp.videoHeight.coerceAtLeast(1).toFloat()
        val sW = container.width.coerceAtLeast(1).toFloat()
        val sH = container.height.coerceAtLeast(1).toFloat()

        // Scale so entire video fits inside screen — no cropping
        val scale = minOf(sW / vW, sH / vH)
        val newW = (vW * scale).toInt()
        val newH = (vH * scale).toInt()

        // Keep video centered inside the FrameLayout
        val lp = FrameLayout.LayoutParams(newW, newH, Gravity.CENTER)
        runOnUiThread { surfaceView.layoutParams = lp }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun goToMain() {
        if (launched || isFinishing || isDestroyed) return
        launched = true
        runOnUiThread {
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        goToMain()
    }
}