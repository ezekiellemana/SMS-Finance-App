package com.smsfinance.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import androidx.core.net.toUri
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.smsfinance.R

/**
 * Full-screen video splash screen.
 * Uses only WindowManager flags (no insetsController) for maximum device compatibility.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : FragmentActivity(), SurfaceHolder.Callback {

    private lateinit var surfaceView: SurfaceView
    private lateinit var container: FrameLayout
    private var mediaPlayer: MediaPlayer? = null
    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Modern back gesture handling — replaces deprecated onBackPressed
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { goToMain() }
        })

        // Full-screen via WindowManager flags — works on ALL Android versions and OEMs
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // Hide nav bar via systemUiVisibility — safe on all versions
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )

        // Dark navy background fills any letterbox gaps
        container = FrameLayout(this).apply {
            setBackgroundColor(0xFF05142A.toInt())
        }

        surfaceView = SurfaceView(this)
        container.addView(surfaceView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        ))
        setContentView(container)
        surfaceView.holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) { playVideo(holder) }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) { releasePlayer() }

    private fun playVideo(holder: SurfaceHolder) {
        try {
            val uri = "android.resource://$packageName/${R.raw.splash_video}".toUri()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setDisplay(holder)
                isLooping = false
                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                setOnPreparedListener { mp -> mp.start(); scaleVideoToFill(mp) }
                setOnCompletionListener { goToMain() }
                setOnErrorListener { _, _, _ -> goToMain(); true }
                prepareAsync()
            }
        } catch (_: Exception) {
            goToMain()
        }
    }

    private fun scaleVideoToFill(mp: MediaPlayer) {
        val vW = mp.videoWidth.coerceAtLeast(1).toFloat()
        val vH = mp.videoHeight.coerceAtLeast(1).toFloat()
        val sW = container.width.coerceAtLeast(1).toFloat()
        val sH = container.height.coerceAtLeast(1).toFloat()
        val scale = minOf(sW / vW, sH / vH)
        val lp = FrameLayout.LayoutParams(
            (vW * scale).toInt(), (vH * scale).toInt(), Gravity.CENTER
        )
        runOnUiThread { surfaceView.layoutParams = lp }
    }

    private fun goToMain() {
        if (launched || isFinishing || isDestroyed) return
        launched = true
        runOnUiThread {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}