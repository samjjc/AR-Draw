package tech.ardraw.app

import javax.vecmath.Vector3f

/**
 * BiquadFilter is a object for easily lowpass filtering incomming values.
 */
class BiquadFilter internal constructor(Fc: Double) {
    private val value = Vector3f()

    private val inst = arrayOfNulls<BiquadFilterInstance>(3)

    init {
        for (i in 0..2) {
            inst[i] = BiquadFilterInstance(Fc)
        }
    }

    internal fun update(`in`: Vector3f): Vector3f {
        value.x = inst[0]!!.process(`in`.x.toDouble()).toFloat()
        value.y = inst[1]!!.process(`in`.y.toDouble()).toFloat()
        value.z = inst[2]!!.process(`in`.z.toDouble()).toFloat()
        return value
    }

    private inner class BiquadFilterInstance internal constructor(fc: Double) {
        internal var a0: Double = 0.toDouble()
        internal var a1: Double = 0.toDouble()
        internal var a2: Double = 0.toDouble()
        internal var b1: Double = 0.toDouble()
        internal var b2: Double = 0.toDouble()
        internal var Fc = 0.5
        internal var Q = 0.707
        internal var peakGain = 0.0
        internal var z1 = 0.0
        internal var z2 = 0.0

        init {
            Fc = fc
            calcBiquad()
        }

        internal fun process(`in`: Double): Double {
            val out = `in` * a0 + z1
            z1 = `in` * a1 + z2 - b1 * out
            z2 = `in` * a2 - b2 * out
            return out
        }

        internal fun calcBiquad() {
            val norm: Double
            val K = Math.tan(Math.PI * Fc)
            norm = 1 / (1.0 + K / Q + K * K)
            a0 = K * K * norm
            a1 = 2 * a0
            a2 = a0
            b1 = 2.0 * (K * K - 1) * norm
            b2 = (1 - K / Q + K * K) * norm
        }
    }
}
