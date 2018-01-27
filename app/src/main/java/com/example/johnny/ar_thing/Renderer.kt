package com.example.johnny.ar_thing

import org.artoolkit.ar.base.ARToolKit
import org.artoolkit.ar.base.rendering.ARRenderer
import org.artoolkit.ar.base.rendering.Cube
import javax.microedition.khronos.opengles.GL10

/**
 * Created by derek on 1/27/2018.
 * Render code currently taken from the docs
 */
class Renderer : ARRenderer() {

    private var markerId: Int = -1
    private val cube = Cube(80f, 0f, 0f, 40f)

    override fun configureARScene(): Boolean {
        //markerId = ARToolKit.getInstance().addMarker("single;/sdcard/AR/Data/what.the;80")
        return true
    }

    override fun draw(gl: GL10?) {
        gl?.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)

        // Apply the ARToolKit projection matrix
        gl?.glMatrixMode(GL10.GL_PROJECTION)
        gl?.glLoadMatrixf(ARToolKit.getInstance().projectionMatrix, 0)

        // If the marker is visible, apply its transformation, and draw a cube
        /*if (ARToolKit.getInstance().queryMarkerVisible(markerId)) {
            gl?.glMatrixMode(GL10.GL_MODELVIEW)
            gl?.glLoadMatrixf(ARToolKit.getInstance().queryMarkerTransformation(markerId), 0)
            cube.draw(gl)
        }*/
    }
}