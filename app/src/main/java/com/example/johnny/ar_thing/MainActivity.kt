package com.example.johnny.ar_thing

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.ar.core.Config
import com.google.ar.core.Session
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
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10



class MainActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    private lateinit var session: Session

    private lateinit var mSubscriptions: CompositeDisposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            message = "Please update ARCore";
            exception = e;
        } catch (e: UnavailableSdkTooOldException) {
            message = "Please update this app";
            exception = e;
        } catch (e: Exception) {
            message = "This device does not support AR";
            exception = e;
        }

        if (message != null) {
            Log.e("MainActivity", "Exception creating session", exception)
            return
        }

        val config = Config(session)
        if (session.isSupported(config)) {
            Log.e("MainActivity", "This device does not support AR")
        }

        session.configure(config)
    }

    override fun onDrawFrame(gl: GL10?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDestroy() {
        mSubscriptions.clear()
        super.onDestroy()
    }

    fun getDrawing(d: Drawing) {
        mSubscriptions.add(getApi().AddDrawing()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::handleResponse,this::handleError))
    }


    fun handleResponse(d :Drawing) {
//        handle data here
    }

    private fun handleError(throwable: Throwable) {
        Log.e("QWE", throwable.toString())
    }

    //making the retrofit object
    private fun getApi(): Api {

        val rxAdapter = RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io())

        return Retrofit.Builder()
                .baseUrl("some-url")
                .addCallAdapterFactory(rxAdapter)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(Api::class.java)
    }
}
