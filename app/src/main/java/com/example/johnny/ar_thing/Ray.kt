package com.example.johnny.ar_thing

import android.opengl.Matrix
import javax.vecmath.Vector2f
import javax.vecmath.Vector3f


class Ray(val origin: Vector3f, val direction: Vector3f) {
    companion object {

        fun screenPointToRay(point: Vector2f, viewportSize: Vector2f, viewProjMtx: FloatArray): Ray {
            point.y = viewportSize.y - point.y
            val x = point.x * 2.0f / viewportSize.x - 1.0f
            val y = point.y * 2.0f / viewportSize.y - 1.0f
            val farScreenPoint = floatArrayOf(x, y, 1.0f, 1.0f)
            val nearScreenPoint = floatArrayOf(x, y, -1.0f, 1.0f)
            val nearPlanePoint = FloatArray(4)
            val farPlanePoint = FloatArray(4)
            val invertedProjectionMatrix = FloatArray(16)
            Matrix.setIdentityM(invertedProjectionMatrix, 0)
            Matrix.invertM(invertedProjectionMatrix, 0, viewProjMtx, 0)
            Matrix.multiplyMV(nearPlanePoint, 0, invertedProjectionMatrix, 0, nearScreenPoint, 0)
            Matrix.multiplyMV(farPlanePoint, 0, invertedProjectionMatrix, 0, farScreenPoint, 0)
            val direction = Vector3f(farPlanePoint[0] / farPlanePoint[3], farPlanePoint[1] / farPlanePoint[3], farPlanePoint[2] / farPlanePoint[3])
            val origin = Vector3f(Vector3f(nearPlanePoint[0] / nearPlanePoint[3], nearPlanePoint[1] / nearPlanePoint[3], nearPlanePoint[2] / nearPlanePoint[3]))
            direction.sub(origin)
            direction.normalize()
            return Ray(origin, direction)
        }
    }
}
