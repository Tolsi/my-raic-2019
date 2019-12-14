package model

import extensions.toPoint
import korma_geom.int
import util.StreamUtil

class UnitAction {
    var velocity: Double = 0.0
    var velocityPerTick: Double = 0.0
    var jump: Boolean = false
    var jumpDown: Boolean = false
    lateinit var aim: model.Vec2Double
    var shoot: Boolean = false
    var reload: Boolean = false
    var swapWeapon: Boolean = false
    var plantMine: Boolean = false
    constructor() {}

    fun calculateFields() {
        this.velocityPerTick = velocity / 60
    }
    constructor(velocity: Double, jump: Boolean, jumpDown: Boolean, aim: model.Vec2Double, shoot: Boolean, reload: Boolean, swapWeapon: Boolean, plantMine: Boolean) {
        this.velocity = velocity
        this.jump = jump
        this.jumpDown = jumpDown
        this.aim = aim
        this.shoot = shoot
        this.reload = reload
        this.swapWeapon = swapWeapon
        this.plantMine = plantMine
        calculateFields()
    }
    companion object {
        val Empty = UnitAction()
        init {
            Empty.aim = Vec2Double()
        }
        @Throws(java.io.IOException::class)
        fun readFrom(stream: java.io.InputStream): UnitAction {
            val result = UnitAction()
            result.velocity = StreamUtil.readDouble(stream)
            result.jump = StreamUtil.readBoolean(stream)
            result.jumpDown = StreamUtil.readBoolean(stream)
            result.aim = model.Vec2Double.readFrom(stream)
            result.shoot = StreamUtil.readBoolean(stream)
            result.reload = StreamUtil.readBoolean(stream)
            result.swapWeapon = StreamUtil.readBoolean(stream)
            result.plantMine = StreamUtil.readBoolean(stream)
            result.velocityPerTick = result.velocity / 60
            result.calculateFields()
            return result
        }
    }
    @Throws(java.io.IOException::class)
    fun writeTo(stream: java.io.OutputStream) {
        StreamUtil.writeDouble(stream, velocity)
        StreamUtil.writeBoolean(stream, jump)
        StreamUtil.writeBoolean(stream, jumpDown)
        aim.writeTo(stream)
        StreamUtil.writeBoolean(stream, shoot)
        StreamUtil.writeBoolean(stream, reload)
        StreamUtil.writeBoolean(stream, swapWeapon)
        StreamUtil.writeBoolean(stream, plantMine)
    }

    override fun toString(): String {
        return "UnitAction(velocity=$velocity, jump=$jump, jumpDown=$jumpDown, aim=$aim, shoot=$shoot, reload=$reload, swapWeapon=$swapWeapon, plantMine=$plantMine)"
    }
}
