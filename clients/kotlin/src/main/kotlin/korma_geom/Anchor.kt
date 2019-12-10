package korma_geom

data class Anchor(val sx: Double, val sy: Double) {
    companion object {
        inline operator fun invoke(sx: Number, sy: Number) = Anchor(sx.toDouble(), sy.toDouble())

        val TOP_LEFT = Anchor(0, 0)
        val TOP_CENTER = Anchor(.5, 0)
        val TOP_RIGHT = Anchor(1, 0)

        val MIDDLE_LEFT = Anchor(0, .5)
        val MIDDLE_CENTER = Anchor(.5, .5)
        val MIDDLE_RIGHT = Anchor(1, .5)

        val BOTTOM_LEFT = Anchor(0, 1)
        val BOTTOM_CENTER = Anchor(.5, 1)
        val BOTTOM_RIGHT = Anchor(1, 1)
    }
}
