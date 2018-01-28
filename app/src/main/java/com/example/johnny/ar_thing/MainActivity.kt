package com.example.johnny.ar_thing

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.example.johnny.ar_thing.rendering.*
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.Trackable
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import javax.vecmath.Vector2f
import javax.vecmath.Vector3f


class MainActivity : AppCompatActivity(), GLSurfaceView.Renderer, SensorEventListener, GestureDetector.OnGestureListener {
    private val TAG = MainActivity::class.java.simpleName

    private var session: Session? = null

    private lateinit var displayRotationHelper: DisplayRotationHelper

    private val backgroundRenderer = BackgroundRenderer()
    private val pointCloudRenderer = PointCloudRenderer()
    private val lineShaderRenderer = LineShaderRenderer()

    private lateinit var mSubscriptions: CompositeDisposable

    private val sensorManager: SensorManager by lazy { getSystemService(SensorManager::class.java) }
    private var accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)

    private lateinit var biquadFilter: BiquadFilter
    private lateinit var mLastPoint: Vector3f
    private var lastTouch: Vector2f = Vector2f()

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private var zeroMatrix = FloatArray(16)


    private var screenWidth = 0f
    private var screenHeight = 0f

    private var mLineWidthMax = 0.33f
    private var mDistanceScale = 0.0f
    private var mLineSmoothing = 0.1f

    private var lastFramePosition: FloatArray? = null

    private val bIsTracking = AtomicBoolean(true)
    private val bReCenterView = AtomicBoolean(false)
    private val bTouchDown = AtomicBoolean(false)
    private val bClearDrawing = AtomicBoolean(false)
    private val bLineParameters = AtomicBoolean(false)
    private val bUndo = AtomicBoolean(false)
    private val bNewStroke = AtomicBoolean(false)

    private var strokes: ArrayList<ArrayList<Vector3f>> = ArrayList()
    private lateinit var currentStroke: Drawing

    private val detector: GestureDetectorCompat by lazy { GestureDetectorCompat(this, this) }

    private lateinit var frame: Frame


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        displayRotationHelper = DisplayRotationHelper(this)

        mSubscriptions = CompositeDisposable()

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

        // Reset the zero matrix
        Matrix.setIdentityM(zeroMatrix, 0)

        surface.setOnTouchListener { _, event ->
            surfaceTouched(event)

            true
        }

        val config = Config(session)
        if (session?.isSupported(config) != true) {
            Log.e(TAG, "This device does not support AR")
        }

        session?.configure(config)

        allDrawings()

        if (ContextCompat.checkSelfPermission(this.applicationContext, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this,  arrayOf(Manifest.permission.CAMERA), 1);
    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()

        PositionHelpers.scheduleLocationUpdates(this, object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                this@MainActivity.location.text = "Latitude: ${location?.latitude}, Longitude: ${location?.longitude}"
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String?) {}

            override fun onProviderDisabled(provider: String?) {}
        })

        val location = PositionHelpers.getCurrentLocation(this)
        this@MainActivity.location.text = "Latitude: ${location?.latitude}, Longitude: ${location?.longitude}"

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI)

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onResume() {
        super.onResume()

        displayRotationHelper.onResume()
        session?.resume()
        surface.onResume()

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenHeight = displayMetrics.heightPixels.toFloat()
        screenWidth = displayMetrics.widthPixels.toFloat()
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

    private fun surfaceTouched(event: MotionEvent) {
        onTouchEvent(event)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (session == null) {
            return
        }

        displayRotationHelper.updateSessionIfNeeded(session!!)

        try {
            frame = session!!.update()
            val camera = frame.camera

            if (camera.trackingState == Trackable.TrackingState.TRACKING && !bIsTracking.get()) {
                bIsTracking.set(true)
            }
            else if (camera.trackingState == Trackable.TrackingState.PAUSED && bIsTracking.get()) {
                bIsTracking.set(false)
                bTouchDown.set(false)
            }

            camera.getProjectionMatrix(projectionMatrix, 0, 0.001f, 100f)
            camera.getViewMatrix(viewMatrix, 0)

            val position = FloatArray(3)
            camera.pose.getTranslation(position, 0)

            // Check if camera has moved much, if thats the case, stop touchDown events
            // (stop drawing lines abruptly through the air)
            if (lastFramePosition != null) {
                val distance = Vector3f(position[0], position[1], position[2])
                distance.sub(Vector3f(lastFramePosition!![0], lastFramePosition!![1], lastFramePosition!![2]))

                if (distance.length() > 0.15) {
                    bTouchDown.set(false)
                }
            }
            lastFramePosition = position

            // Multiply the zero matrix
            Matrix.multiplyMM(viewMatrix, 0, viewMatrix, 0, zeroMatrix, 0)

            if (bNewStroke.get()) {
                bNewStroke.set(false)
                addStroke(lastTouch)
                lineShaderRenderer.bNeedsUpdate.set(true)
            } else if (bTouchDown.get()) {
                addPoint(lastTouch)
                lineShaderRenderer.bNeedsUpdate.set(true)
            }

            if (bReCenterView.get()) {
                bReCenterView.set(false)
                zeroMatrix = getCalibrationMatrix()
            }

            if (bClearDrawing.get()) {
                bClearDrawing.set(false)
                clearDrawing()
                lineShaderRenderer.bNeedsUpdate.set(true)
            }

            if (bUndo.get()) {
                bUndo.set(false)
                if (strokes.size > 0) {
                    strokes.removeAt(strokes.size - 1)
                    lineShaderRenderer.bNeedsUpdate.set(true)
                }
            }
            
            lineShaderRenderer.setDrawDebug(bLineParameters.get())
            if (lineShaderRenderer.bNeedsUpdate.get()) {
                lineShaderRenderer.setColor(Vector3f(255f, 255f, 255f))
                lineShaderRenderer.mDrawDistance = 0.125f
                lineShaderRenderer.setDistanceScale(mDistanceScale)
                lineShaderRenderer.setLineWidth(mLineWidthMax)
                lineShaderRenderer.clear()
                lineShaderRenderer.updateStrokes(strokes)
                lineShaderRenderer.upload()
            }

            backgroundRenderer.draw(frame)

            val pointCloud = frame.acquirePointCloud()
            pointCloudRenderer.update(pointCloud)
            pointCloudRenderer.draw(viewMatrix, projectionMatrix)

            pointCloud.release()

            if (camera.trackingState == Trackable.TrackingState.TRACKING) {
                lineShaderRenderer.draw(viewMatrix, projectionMatrix, screenWidth, screenHeight, 0.001f, 100.0f)
            }

            // TODO: Draw 3d objects I guess
        } catch (t: Throwable) {
            Log.e(TAG, "Exception on the OpenGl thread", t)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
        session?.setDisplayGeometry(0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1F, 0.1F, 0.1F, 0.1F)

        backgroundRenderer.createOnGlThread(this)
        if (session != null) {
            session!!.setCameraTextureName(backgroundRenderer.textureId)
        }

        pointCloudRenderer.createOnGlThread(this)
        lineShaderRenderer.createOnGlThread(this)
    }

    /**
     * Get a matrix usable for zero calibration (only position and compass direction)
     */
    private fun getCalibrationMatrix(): FloatArray {
        val t = FloatArray(3)
        val m = FloatArray(16)

        frame.camera.pose.getTranslation(t, 0)
        val z = frame.camera.pose.zAxis
        val zAxis = Vector3f(z[0], z[1], z[2])
        zAxis.y = 0f
        zAxis.normalize()

        val rotate = Math.atan2(zAxis.x.toDouble(), zAxis.z.toDouble())

        Matrix.setIdentityM(m, 0)
        Matrix.translateM(m, 0, t[0], t[1], t[2])
        Matrix.rotateM(m, 0, Math.toDegrees(rotate).toFloat(), 0f, 1f, 0f)
        return m
    }

    /**
     * Clears the Datacollection of Strokes and sets the Line Renderer to clear and update itself
     * Designed to be executed on the GL Thread
     */
    fun clearDrawing() {
        strokes.clear()
        lineShaderRenderer.clear()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
    }

    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.size)
        }

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)

        val orientationAngles = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        bearing.text = "Bearing: ${Math.toDegrees(orientationAngles[0].toDouble())}"
    }

    override fun onDestroy() {
        mSubscriptions.clear()
        super.onDestroy()
    }

    fun allDrawings() {
        mSubscriptions.add(getApi().allDrawings()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::handleResponse,this::handleError))
    }

    fun searchDrawings(lat: Double, lon: Double) {
        mSubscriptions.add(getApi().searchDrawings(lat, lon)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::handleResponse,this::handleError))
    }


    fun addDrawing(d: Drawing) {
        mSubscriptions.add(getApi().addDrawing(d)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::handleResponse,this::handleError))
    }

//    display drawings
    fun handleResponse(drawings: List<Drawing>) {
        strokes.addAll(drawings.map{d -> d.getPoints()})
    }

    fun handleResponse(i :Int) {
//        handle data here
    }

    private fun handleError(throwable: Throwable) {
        Log.e("QWE", throwable.toString())
    }

    //making the retrofit object
    private fun getApi(): Api {

        val rxAdapter = RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io())

        return Retrofit.Builder()
                .baseUrl("http://ec2-18-216-47-242.us-east-2.compute.amazonaws.com/")
                .addCallAdapterFactory(rxAdapter)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(Api::class.java)
    }

    /**
     * addStroke adds a new stroke to the scene
     *
     * @param touchPoint a 2D point in screen space and is projected into 3D world space
     */
    private fun addStroke(touchPoint: Vector2f) {
        val newPoint = LineUtils.getWorldCoords(touchPoint, screenWidth, screenHeight, projectionMatrix, viewMatrix)
        addStroke(newPoint)
    }

    /**
     * addPoint adds a point to the current stroke
     *
     * @param touchPoint a 2D point in screen space and is projected into 3D world space
     */
    private fun addPoint(touchPoint: Vector2f) {
        val newPoint = LineUtils.getWorldCoords(touchPoint, screenWidth, screenHeight, projectionMatrix, viewMatrix)
        addPoint(newPoint)
    }

    /**
     * addStroke creates a new stroke
     *
     * @param newPoint a 3D point in world space
     */
    private fun addStroke(newPoint: Vector3f) {
        biquadFilter = BiquadFilter(mLineSmoothing.toDouble())
        for (i in 0..1499) {
            biquadFilter.update(newPoint)
        }
        val p = biquadFilter.update(newPoint)
        mLastPoint = Vector3f(p)
        strokes.add(ArrayList())
        strokes[strokes.size - 1].add(mLastPoint)
    }

    /**
     * addPoint adds a point to the current stroke
     *
     * @param newPoint a 3D point in world space
     */
    private fun addPoint(newPoint: Vector3f) {
        if (LineUtils.distanceCheck(newPoint, mLastPoint)) {
            val p = biquadFilter.update(newPoint)
            mLastPoint = Vector3f(p)
            strokes[strokes.size - 1].add(mLastPoint)
        }
    }

    /**
     * onTouchEvent handles saving the lastTouch screen position and setting bTouchDown and bNewStroke
     * AtomicBooleans to trigger addPoint and addStroke on the GL Thread to be called
     */
    override fun onTouchEvent(tap: MotionEvent): Boolean {
        this.detector.onTouchEvent(tap)

        if (tap.action == MotionEvent.ACTION_DOWN) {
            lastTouch.set(tap.x, tap.y)
            bTouchDown.set(true)
            bNewStroke.set(true)
            return true
        } else if (tap.action == MotionEvent.ACTION_MOVE) {
            lastTouch.set(tap.x, tap.y)
            bTouchDown.set(true)
            return true
        } else if (tap.action == MotionEvent.ACTION_UP) {
            bTouchDown.set(false)
            lastTouch.set(tap.x, tap.y)
            return true
        }

        return super.onTouchEvent(tap)
    }
    override fun onShowPress(e: MotionEvent?) {
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return false
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?) {
    }

}
