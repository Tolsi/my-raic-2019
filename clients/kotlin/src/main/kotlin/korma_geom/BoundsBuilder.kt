package korma_geom

class BoundsBuilder {
    val tempRect = Rectangle()

    companion object {
        private val MIN = Double.NEGATIVE_INFINITY
        private val MAX = Double.POSITIVE_INFINITY
    }

    private var npoints = 0
    private var xmin = MAX
    private var xmax = MIN
    private var ymin = MAX
    private var ymax = MIN

    fun reset() {
        xmin = MAX
        xmax = MIN
        ymin = MAX
        ymax = MIN
        npoints = 0
    }

    fun add(x: Double, y: Double): BoundsBuilder {
        xmin = kotlin.math.min(xmin, x)
        xmax = kotlin.math.max(xmax, x)
        ymin = kotlin.math.min(ymin, y)
        ymax = kotlin.math.max(ymax, y)
        npoints++
        //println("add($x, $y) -> ($xmin,$ymin)-($xmax,$ymax)")
        return this
    }

    fun getBoundsOrNull(out: Rectangle = Rectangle()): Rectangle? = if (npoints == 0) null else out.setBounds(xmin, ymin, xmax, ymax)

    fun getBounds(out: Rectangle = Rectangle()): Rectangle {
        if (getBoundsOrNull(out) == null) {
            out.setBounds(0, 0, 0, 0)
        }
        return out
    }
}

inline fun BoundsBuilder.add(x: Number, y: Number) = add(x.toDouble(), y.toDouble())
fun BoundsBuilder.add(p: IPoint) = add(p.x, p.y)
fun BoundsBuilder.add(ps: Iterable<IPoint>) = this.apply { for (p in ps) add(p) }
fun BoundsBuilder.add(ps: IPointArrayList) = run { for (n in 0 until ps.size) add(ps.getX(n), ps.getY(n)) }
fun BoundsBuilder.add(rect: Rectangle) = this.apply {
    if (rect.isNotEmpty) {
        add(rect.left, rect.top)
        add(rect.right, rect.bottom)
    }
}
