package model

import util.StreamUtil

enum class WeaponType private constructor(var discriminant: Int) {
    PISTOL(0),
    ASSAULT_RIFLE(1),
    ROCKET_LAUNCHER(2);

    override fun toString(): String{
        return "WeaponType(${this.javaClass.simpleName})"
    }
}
