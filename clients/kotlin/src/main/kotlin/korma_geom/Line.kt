package korma_geom

data class Line(val from: Point, val to: Point) {
    val length: Double get() = run { from.distanceTo(to) }

    fun interpolateBy(step: Point) = sequence {
        val current: Point = from.copy()
        while (from.distanceTo(current) < length) {
            current.add(step)
            yield(current)
        }
    }

    // todo is it works?
    fun intersects(p: Point): Boolean = (p.x - from.x) / (to.x - from.x) == (p.y - from.y) / (to.y - from.y)

    fun times(n: Double): Line {
        return Line(from, Point(from.x + (to.x - from.x) * n, from.y + (to.y - from.y) * n))
    }

    companion object {
        // todo limit!
        fun createFromPointAimAndSpeed(from: Point, aim: Point, speed: Double): Sequence<Point> {
            val speedPoint = aim.copy()
            speedPoint.normalize()
            // by ticks
            speedPoint.mul(speed / 60)
            return Line(from, from.copy().add(speedPoint)).times(150.0).interpolateBy(speedPoint)
        }
    }
}