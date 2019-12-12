package model

import util.StreamUtil

class BulletParams {
    var speed: Double = 0.0
    var speedPerTick: Double = 0.0
    var size: Double = 0.0
    var damage: Int = 0
    constructor() {}
    constructor(speed: Double, size: Double, damage: Int) {
        this.speed = speed
        this.size = size
        this.damage = damage
        this.speedPerTick = speed / 60
    }
    companion object {
        @Throws(java.io.IOException::class)
        fun readFrom(stream: java.io.InputStream): BulletParams {
            val result = BulletParams()
            result.speed = StreamUtil.readDouble(stream)
            result.size = StreamUtil.readDouble(stream)
            result.damage = StreamUtil.readInt(stream)
            result.speedPerTick = result.speed / 60
            return result
        }
    }
    @Throws(java.io.IOException::class)
    fun writeTo(stream: java.io.OutputStream) {
        StreamUtil.writeDouble(stream, speed)
        StreamUtil.writeDouble(stream, size)
        StreamUtil.writeInt(stream, damage)
    }

    override fun toString(): String {
        return "BulletParams(speed=$speed, size=$size, damage=$damage)"
    }

}
