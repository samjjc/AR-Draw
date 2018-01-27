package com.example.johnny.ar_thing.rendering

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.example.johnny.ar_thing.R
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
    private var textureId: Int = -1

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
}