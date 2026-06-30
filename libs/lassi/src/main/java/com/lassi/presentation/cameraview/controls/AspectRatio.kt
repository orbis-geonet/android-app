package com.lassi.presentation.cameraview.controls

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * A simple class representing an aspect ratio.
 */
@Parcelize
class AspectRatio private constructor(val x: Int, val y: Int) : Comparable<AspectRatio>,
    Parcelable {

    fun matches(size: Size): Boolean {
        val gcd = gcd(size.width, size.height)
        val x = size.width / gcd
        val y = size.height / gcd
        return this.x == x && this.y == y
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (this === other) {
            return true
        }
        if (other is AspectRatio) {
            val ratio = other as AspectRatio?
            return x == ratio!!.x && y == ratio.y
        }
        return false
    }

    override fun toString(): String {
        return "$x:$y"
    }

    fun toFloat(): Float {
        return x.toFloat() / y
    }

    override fun hashCode(): Int {
        return y xor (x shl Integer.SIZE / 2 or x.ushr(Integer.SIZE / 2))
    }

    override fun compareTo(other: AspectRatio): Int {
        if (equals(other)) {
            return 0
        } else if (toFloat() - other.toFloat() > 0) {
            return 1
        }
        return -1
    }

    fun inverse(): AspectRatio {
        return AspectRatio.of(y, x)
    }

    companion object {

        internal val sCache = HashMap<String, AspectRatio>(16)

        /**
         * Creates an aspect ratio for the given size.
         *
         * @param size the size
         * @return a (possibly cached) aspect ratio
         */
        fun of(size: Size): AspectRatio {
            return AspectRatio.of(size.width, size.height)
        }

        /**
         * Creates an aspect ratio with the given values.
         *
         * @param x the width
         * @param y the height
         * @return a (possibly cached) aspect ratio
         */
        fun of(x: Int, y: Int): AspectRatio {
            var x_ = x
            var y_ = y
            val gcd = gcd(x_, y_)
            x_ /= gcd
            y_ /= gcd
            val key = "$x:$y"
            var cached = sCache[key]
            if (cached == null) {
                cached = AspectRatio(x_, y_)
                sCache[key] = cached
            }
            return cached
        }

        /**
         * Parses an aspect ratio string, for example those previously obtained
         * with [.toString].
         *
         * @param string a string of the format x:y where x and y are integers
         * @return a (possibly cached) aspect ratio
         */
        fun parse(string: String): AspectRatio {
            val parts = string.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size != 2) {
                throw NumberFormatException("Illegal AspectRatio string. Must be x:y")
            }
            val x = Integer.valueOf(parts[0])
            val y = Integer.valueOf(parts[1])
            return of(x, y)
        }

        private fun gcd(a: Int, b: Int): Int {
            var gcdA = a
            var gcdB = b
            while (gcdB != 0) {
                val c = gcdB
                gcdB = gcdA % gcdB
                gcdA = c
            }
            return gcdA
        }
    }
}