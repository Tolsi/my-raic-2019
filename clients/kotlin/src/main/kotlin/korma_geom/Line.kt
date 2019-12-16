package korma_geom

import extensions.lines

data class Line(val from: Point, val to: Point) {
    val length: Double get() = run { from.distanceTo(to) }

    init {
        require(from != to)
    }

    fun interpolateBy(step: Point) = sequence {
        val current: Point = from.copy()
        while (from.distanceTo(current) < length) {
            current.add(step)
            yield(current)
        }
    }

    fun a(): Double = from.y - to.y
    fun b(): Double = from.x - to.x
    fun c(): Double = from.x * to.y - to.x * from.y

    // todo is it works?
    fun intersects(p: Point): Boolean {
        return infiniteIntersects(p) && isInRectangleBetweenPoints(p)
    }

    private fun isInRectangleBetweenPoints(p: Point): Boolean {
        return p.x >= Math.min(from.x, to.x) && p.x <= Math.max(from.x, to.x) &&
                p.y >= Math.min(from.y, to.y) && p.y <= Math.max(from.y, to.y)
    }

    fun infiniteIntersects(p: Point): Boolean = (p.x - from.x) / (to.x - from.x) == (p.y - from.y) / (to.y - from.y)
    fun infiniteIntersects(p: Line): Point? {
        val d = (this.b() * p.a() - this.a() * p.b())
        val x = (this.c() * p.b() - this.b() * p.c()) / d
        val y = (this.c() * p.a() - this.a() * p.c()) / d
        return Point(x, y).takeIf { x.isFinite() && y.isFinite() }
    }

    fun intersects(p: Line): Point? {
        return infiniteIntersects(p)?.takeIf { isInRectangleBetweenPoints(it) }
    }

    fun distance(p: Point): Double {
        val v = to - from
        val w = p - from
        val c1 = w.dot(v)
        val c2 = v.dot(v)
        if (c1 <= 0)
            return p.distanceTo(from)
        if (c2 <= c1)
            return p.distanceTo(to)
        val b = c1 / c2
        val Pb = from + v * b
        return p.distanceTo(Pb)
    }

    fun times(n: Double): Line {
        return Line(from, Point(from.x + (to.x - from.x) * n, from.y + (to.y - from.y) * n))
    }

    fun points(): List<Point> = listOf(from, to)
    override fun toString(): String {
        return "Line(from=$from, to=$to)"
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

        fun fromPointAndAngle(from: Point, angle: Point): Line {
            val speedPoint = angle.copy().normalize()
            val bigLine = Line(from, speedPoint).times(100.0)
            val intersectionWithLevel = Global.level.lines().map { bigLine.infiniteIntersects(it) }.filterNotNull().first()
            return Line(from, intersectionWithLevel)
        }
    }
}

fun Collection<Point>.toLine(): Line {
    require(this.size == 2)
    return Line(this.elementAt(0), this.elementAt(1))
}