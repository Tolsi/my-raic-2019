package model

import korma_geom.IPoint
import util.StreamUtil

class Vec2Double: IPoint {
    override var x: Double = 0.0
    override var y: Double = 0.0
    constructor() {}
    constructor(x: Double, y: Double) {
        this.x = x
        this.y = y
    }
    companion object {
        @Throws(java.io.IOException::class)
        fun readFrom(stream: java.io.InputStream): Vec2Double {
            val result = Vec2Double()
            result.x = StreamUtil.readDouble(stream)
            result.y = StreamUtil.readDouble(stream)
            return result
        }
    }
    @Throws(java.io.IOException::class)
    fun writeTo(stream: java.io.OutputStream) {
        StreamUtil.writeDouble(stream, x)
        StreamUtil.writeDouble(stream, y)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vec2Double

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    override fun toString(): String {
        return "Vec2Double(x=$x, y=$y)"
    }

    fun add(s: Vec2Double): Vec2Double {
        return Vec2Double(x + s.x, y + s.y)
    }

    fun copyOf(): Vec2Double {
        return Vec2Double(x, y)
    }

}
