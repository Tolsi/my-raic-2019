package model

import util.StreamUtil

abstract class Item {
    @Throws(java.io.IOException::class)
    abstract fun writeTo(stream: java.io.OutputStream)
    companion object {
        @Throws(java.io.IOException::class)
        fun readFrom(stream: java.io.InputStream): Item {
            when (StreamUtil.readInt(stream)) {
                HealthPack.TAG -> return HealthPack.readFrom(stream)
                Weapon.TAG -> return Weapon.readFrom(stream)
                Mine.TAG -> return Mine.readFrom(stream)
                else -> throw java.io.IOException("Unexpected discriminant value")
            }
        }
    }

    class HealthPack : Item {
        var health: Int = 0
        constructor() {}
        constructor(health: Int) {
            this.health = health
        }
        companion object {
            val TAG = 0
            @Throws(java.io.IOException::class)
            fun readFrom(stream: java.io.InputStream): HealthPack {
                val result = HealthPack()
                result.health = StreamUtil.readInt(stream)
                return result
            }
        }
        @Throws(java.io.IOException::class)
        override fun writeTo(stream: java.io.OutputStream) {
            StreamUtil.writeInt(stream, TAG)
            StreamUtil.writeInt(stream, health)
        }

        override fun toString(): String {
            return "HealthPack(health=$health)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HealthPack

            if (health != other.health) return false

            return true
        }

        override fun hashCode(): Int {
            return health
        }


    }

    class Weapon : Item {
        lateinit var weaponType: model.WeaponType
        constructor() {}
        constructor(weaponType: model.WeaponType) {
            this.weaponType = weaponType
        }
        companion object {
            val TAG = 1
            @Throws(java.io.IOException::class)
            fun readFrom(stream: java.io.InputStream): Weapon {
                val result = Weapon()
                when (StreamUtil.readInt(stream)) {
                0 ->result.weaponType = model.WeaponType.PISTOL
                1 ->result.weaponType = model.WeaponType.ASSAULT_RIFLE
                2 ->result.weaponType = model.WeaponType.ROCKET_LAUNCHER
                else -> throw java.io.IOException("Unexpected discriminant value")
                }
                return result
            }
        }
        @Throws(java.io.IOException::class)
        override fun writeTo(stream: java.io.OutputStream) {
            StreamUtil.writeInt(stream, TAG)
            StreamUtil.writeInt(stream, weaponType.discriminant)
        }

        override fun toString(): String {
            return "Weapon(weaponType=$weaponType)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Weapon

            if (weaponType != other.weaponType) return false

            return true
        }

        override fun hashCode(): Int {
            return weaponType.hashCode()
        }


    }

    class Mine : Item {
        constructor() {}
        companion object {
            val TAG = 2
            @Throws(java.io.IOException::class)
            fun readFrom(stream: java.io.InputStream): Mine {
                val result = Mine()
                return result
            }
        }
        @Throws(java.io.IOException::class)
        override fun writeTo(stream: java.io.OutputStream) {
            StreamUtil.writeInt(stream, TAG)
        }

        override fun toString(): String {
            return "Mine()"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }


    }
}
