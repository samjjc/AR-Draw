package com.example.johnny.ar_thing.rendering

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.example.johnny.ar_thing.R
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Renders the background using the camera feed.
 */
class BackgroundRenderer {
    private val TAG = BackgroundRenderer::class.java.simpleName

    private val COORDS_PER_VERTEX = 3
    private val TEXCOORDS_PER_VERTEX = 2
    private val FLOAT_SIZE = 4

    private lateinit var quadVertices: FloatBuffer
    private lateinit var quadTexCoord: FloatBuffer
    private lateinit var quadTexCoordTransformed: FloatBuffer

    private val QUAD_COORDS = floatArrayOf(-1.0f, -1.0f, 0.0f, -1.0f, +1.0f, 0.0f, +1.0f, -1.0f, 0.0f, +1.0f, +1.0f, 0.0f)
    private val QUAD_TEXCOORDS = floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f)

    private var mQuadProgram: Int = 0

    private var mQuadPositionParam: Int = 0
    private var mQuadTexCoordParam: Int = 0
    var textureId: Int = -1
        private set

    /**
     * Allocates and initializes OpenGL resources needed by the background renderer.  Must be
     * called on the OpenGL thread, typically in
     * {@link GLSurfaceView.Renderer#onSurfaceCreated(GL10, EGLConfig)}.
     *
     * @param context Needed to access shader source.
     */
    fun createOnGlThread(context: Context) {
        val textures = IntArray(1)

        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        val textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        GLES20.glBindTexture(textureTarget, textureId)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

        val numVertices = 4
        if (numVertices != QUAD_COORDS.size / COORDS_PER_VERTEX) {
            throw RuntimeException("Unexpected number of vertices in BackgroundRenderer.")
        }

        val bbVertices = ByteBuffer.allocateDirect(QUAD_COORDS.size * FLOAT_SIZE)
        bbVertices.order(ByteOrder.nativeOrder())
        quadVertices = bbVertices.asFloatBuffer()
        quadVertices.put(QUAD_COORDS)
        quadVertices.position(0)

        val bbTexCoords = ByteBuffer.allocateDirect(
                numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE)
        bbTexCoords.order(ByteOrder.nativeOrder())
        quadTexCoord = bbTexCoords.asFloatBuffer()
        quadTexCoord.put(QUAD_TEXCOORDS)
        quadTexCoord.position(0)

        val bbTexCoordsTransformed = ByteBuffer.allocateDirect(
                numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE)
        bbTexCoordsTransformed.order(ByteOrder.nativeOrder())
        quadTexCoordTransformed = bbTexCoordsTransformed.asFloatBuffer()

        val vertexShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_VERTEX_SHADER, R.raw.screenquad_vertex)
        val fragmentShader = ShaderUtil.loadGLShader(TAG, context,
                GLES20.GL_FRAGMENT_SHADER, R.raw.screenquad_fragment_oes)

        mQuadProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(mQuadProgram, vertexShader)
        GLES20.glAttachShader(mQuadProgram, fragmentShader)
        GLES20.glLinkProgram(mQuadProgram)
        GLES20.glUseProgram(mQuadProgram)

        ShaderUtil.checkGLError(TAG, "Program creation")

        mQuadPositionParam = GLES20.glGetAttribLocation(mQuadProgram, "a_Position")
        mQuadTexCoordParam = GLES20.glGetAttribLocation(mQuadProgram, "a_TexCoord")

        ShaderUtil.checkGLError(TAG, "Program parameters")
    }

    /**
     * Draws the AR background image.  The image will be drawn such that virtual content rendered
     * with the matrices provided by {@link com.google.ar.core.Camera#getViewMatrix(float[], int)}
     * and {@link com.google.ar.core.Camera#getProjectionMatrix(float[], int, float, float)} will
     * accurately follow static physical objects.
     * This must be called <b>before</b> drawing virtual content.
     *
     * @param frame The last {@code Frame} returned by {@link Session#update()}.
     */
    fun draw(frame: Frame) {
        // If display rotation changed (also includes view size change), we need to re-query the uv
        // coordinates for the screen rect, as they may have changed as well.
        if (frame.hasDisplayGeometryChanged()) {
            frame.transformDisplayUvCoords(quadTexCoord, quadTexCoordTransformed)
        }

        // No need to test or write depth, the screen quad has arbitrary depth, and is expected
        // to be drawn first.
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        GLES20.glUseProgram(mQuadProgram)

        // Set the vertex positions.
        GLES20.glVertexAttribPointer(
                mQuadPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, quadVertices)

        // Set the texture coordinates.
        GLES20.glVertexAttribPointer(mQuadTexCoordParam, TEXCOORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, 0, quadTexCoordTransformed)

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(mQuadPositionParam)
        GLES20.glEnableVertexAttribArray(mQuadTexCoordParam)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(mQuadPositionParam)
        GLES20.glDisableVertexAttribArray(mQuadTexCoordParam)

        // Restore the depth state for further drawing.
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        ShaderUtil.checkGLError(TAG, "Draw")
    }
}