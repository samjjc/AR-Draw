package com.example.johnny.ar_thing

import android.opengl.GLES20
import com.example.johnny.ar_thing.rendering.ShaderUtil


/**
 * Created by derek on 1/27/2018.
 */
class Triangle {
    private val vertexShaderCode = "attribute vec4 vPosition;" +
    "void main() {" +
    "  gl_Position = vPosition;" +
    "}"

    private val fragmentShaderCode = (
"precision mediump float;" +
"uniform vec4 vColor;" +
"void main() {" +
"  gl_FragColor = vColor;" +
"}")

    private val program: Int

    init {
        val vertexShader = ShaderUtil.loadGLShader("Triangle", GLES20.GL_VERTEX_SHADER,
                vertexShaderCode)
        val fragmentShader = ShaderUtil.loadGLShader("Triangle", GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode)

        // create empty OpenGL ES Program
        program = GLES20.glCreateProgram()

        // add the vertex shader to program
        GLES20.glAttachShader(program, vertexShader)

        // add the fragment shader to program
        GLES20.glAttachShader(program, fragmentShader)

        // creates OpenGL ES program executables
        GLES20.glLinkProgram(program)
    }


}