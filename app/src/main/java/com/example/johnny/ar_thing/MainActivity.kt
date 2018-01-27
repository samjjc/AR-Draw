package com.example.johnny.ar_thing

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.example.johnny.ar_thing.rendering.BackgroundRenderer
import com.example.johnny.ar_thing.rendering.DisplayRotationHelper
import com.example.johnny.ar_thing.rendering.PointCloudRenderer
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.Trackable
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import kotlinx.android.synthetic.main.activity_main.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity(), GLSurfaceView.Renderer {
    private val TAG = MainActivity::class.java.simpleName

    private var session: Session? = null

    private lateinit var displayRotationHelper: DisplayRotationHelper

    private val backgroundRenderer = BackgroundRenderer()
    private val pointCloudRenderer = PointCloudRenderer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        displayRotationHelper = DisplayRotationHelper(this)

        // Initialize GLSurfaceView
        with (surface) {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(this@MainActivity)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        var message: String? = null
        var exception: Exception? = null
        try {
            session  = Session(this)
        } catch (e: UnavailableArcoreNotInstalledException) {
            message = "Please install ARCore"
            exception = e
        } catch (e: UnavailableApkTooOldException) {
            message = "Please update ARCore"
            exception = e
        } catch (e: UnavailableSdkTooOldException) {
            message = "Please update this app"
            exception = e
        } catch (e: Exception) {
            message = "This device does not support AR"
            exception = e
        }

        if (message != null) {
            Log.e(TAG, "Exception creating session", exception)
            return
        }

        val config = Config(session)
        if (session?.isSupported(config) != true) {
            Log.e(TAG, "This device does not support AR")
        }

        session?.configure(config)
    }

    override fun onResume() {
        super.onResume()

        displayRotationHelper.onResume()
        session?.resume()
        surface.onResume()
    }

    override fun onPause() {
        super.onPause()

        displayRotationHelper.onPause()
        surface.onPause()
        session?.pause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Standard Android full-screen functionality.
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (session == null) {
            return
        }

        displayRotationHelper.updateSessionIfNeeded(session!!)

        try {
            val frame = session!!.update()
            val camera = frame.camera

            backgroundRenderer.draw(frame)

            // If not tracking, don't draw 3d objects.
            if (camera.trackingState == Trackable.TrackingState.PAUSED) {
                return
            }

            val projectionMatrix = FloatArray(16)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)

            val viewMatrix = FloatArray(16)
            camera.getViewMatrix(viewMatrix, 0)

            val lightIntensity = frame.lightEstimate.pixelIntensity

            val pointCloud = frame.acquirePointCloud()
            pointCloudRenderer.update(pointCloud)
            pointCloudRenderer.draw(viewMatrix, projectionMatrix)

            pointCloud.release()

            // TODO: Draw 3d objects I guess
        } catch (t: Throwable) {
            Log.e(TAG, "Exception on the OpenGl thread", t)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1F, 0.1F, 0.1F, 0.1F)

        backgroundRenderer.createOnGlThread(this)
        if (session != null) {
            session!!.setCameraTextureName(backgroundRenderer.textureId)
        }

        pointCloudRenderer.createOnGlThread(this)
    }
}
