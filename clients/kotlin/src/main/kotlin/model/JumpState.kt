package model

import util.StreamUtil

class JumpState {
    var canJump: Boolean = false
    var speed: Double = 0.0
    var speedPerTick: Double = 0.0
    var maxTime: Double = 0.0
    var canCancel: Boolean = false
    constructor() {}
    constructor(canJump: Boolean, speed: Double, maxTime: Double, canCancel: Boolean) {
        this.canJump = canJump
        this.speed = speed
        this.maxTime = maxTime
        this.canCancel = canCancel
        this.speedPerTick = speed / 60
    }
    companion object {
        @Throws(java.io.IOException::class)
        fun readFrom(stream: java.io.InputStream): JumpState {
            val result = JumpState()
            result.canJump = StreamUtil.readBoolean(stream)
            result.speed = StreamUtil.readDouble(stream)
            result.maxTime = StreamUtil.readDouble(stream)
            result.canCancel = StreamUtil.readBoolean(stream)
            result.speedPerTick = result.speed / 60
            return result
        }
    }
    @Throws(java.io.IOException::class)
    fun writeTo(stream: java.io.OutputStream) {
        StreamUtil.writeBoolean(stream, canJump)
        StreamUtil.writeDouble(stream, speed)
        StreamUtil.writeDouble(stream, maxTime)
        StreamUtil.writeBoolean(stream, canCancel)
    }
    fun copyOf(): JumpState {
        return JumpState(canJump, speed, maxTime, canCancel)
    }

    override fun toString(): String {
        return "JumpState(canJump=$canJump, speed=$speed, maxTime=$maxTime, canCancel=$canCancel)"
    }

}
