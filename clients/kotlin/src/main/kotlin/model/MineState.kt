package model

enum class MineState private constructor(var discriminant: Int) {
    PREPARING(0),
    IDLE(1),
    TRIGGERED(2),
    EXPLODED(3);

    override fun toString(): String{
        return "MineState.${this.name}"
    }
}
