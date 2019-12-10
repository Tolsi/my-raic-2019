package model

import util.StreamUtil

class Vec2Double {
    var x: Double = 0.0
    var y: Double = 0.0
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

}
