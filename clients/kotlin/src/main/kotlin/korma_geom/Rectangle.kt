package korma_geom

import korma_geom.internal.*
import simulation.WorldSimulation

interface IRectangle {
    val x: Double
    val y: Double
    val width: Double
    val height: Double

    companion object {
        inline operator fun invoke(x: Number, y: Number, width: Number, height: Number): IRectangle = Rectangle(x, y, width, height)
    }
}

val IRectangle.left get() = x
val IRectangle.top get() = y
val IRectangle.right get() = x + width
val IRectangle.bottom get() = y + height
val IRectangle.points get() = listOf(Point(x + width / 2, y + height / 2), Point(x + width / 2,  y - height / 2), Point(x - width / 2, y + height / 2), Point(x - width / 2, y - height / 2))
fun Iterable<IPoint>.farPoint(from: IPoint): IPoint? {
    return this.sortedBy { -from.distanceTo(it) }.firstOrNull()
}
fun Iterable<IPoint>.closestPoint(from: IPoint): IPoint? {
    return this.sortedBy { from.distanceTo(it) }.firstOrNull()
}

data class Rectangle(
    override var x: Double, override var y: Double,
    override var width: Double, override var height: Double
) : IRectangle, Sizeable {
    companion object {
        inline operator fun invoke(): Rectangle = Rectangle(0.0, 0.0, 0.0, 0.0)
        inline operator fun invoke(x: Number, y: Number, width: Number, height: Number): Rectangle = Rectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        inline fun fromBounds(left: Number, top: Number, right: Number, bottom: Number): Rectangle = Rectangle().setBounds(left, top, right, bottom)
        fun isContainedIn(a: Rectangle, b: Rectangle): Boolean = a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height
    }

    val isEmpty: Boolean get() = area == 0.0
    val isNotEmpty: Boolean get() = area != 0.0
    val area: Double get() = width * height
    var left: Double; get() = x; set(value) = run { x = value }
    var top: Double; get() = y; set(value) = run { y = value }
    var right: Double; get() = x + width; set(value) = run { width = value - x }
    var bottom: Double; get() = y + height; set(value) = run { height = value - y }

    val position: Point get() = Point(x, y)
    override val size: Size get() = Size(width, height)

    fun setTo(x: Double, y: Double, width: Double, height: Double) = this.apply {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
    }

    fun copyFrom(that: Rectangle) = setTo(that.x, that.y, that.width, that.height)

    fun setBounds(left: Double, top: Double, right: Double, bottom: Double) = setTo(left, top, right - left, bottom - top)

    operator fun times(scale: Double) = Rectangle(x * scale, y * scale, width * scale, height * scale)
    operator fun div(scale: Double) = Rectangle(x / scale, y / scale, width / scale, height / scale)

    operator fun contains(that: Rectangle) = isContainedIn(that, this)
    operator fun contains(that: IPoint) = contains(that.x, that.y)
    fun contains(x: Double, y: Double) =
//            Math.abs(x - this.left) < 1f/6 || Math.abs(x - this.right) < 1f/6 ||
//            Math.abs(y - this.left) < 1f/6 || Math.abs(y - this.right) < 1f/6 ||
            (x >= left && x < right) && (y >= top && y < bottom)

    infix fun intersects(that: Rectangle): Boolean = intersectsX(that) && intersectsY(that)

    infix fun intersectsX(that: Rectangle): Boolean = Math.abs(that.right - this.left) < 1f/6 ||
            Math.abs(that.left - this.right) < 1f/6 ||
            that.left <= this.right && that.right >= this.left
    infix fun intersectsY(that: Rectangle): Boolean = Math.abs(that.top - this.bottom) < 1f/6 ||
            Math.abs(that.bottom - this.top) < 1f/6 ||
            that.top <= this.bottom && that.bottom >= this.top

    fun setToIntersection(a: Rectangle, b: Rectangle) = this.apply { a.intersection(b, this) }

    infix fun intersection(that: Rectangle) = intersection(that, Rectangle())

    fun intersection(that: Rectangle, target: Rectangle = Rectangle()) = if (this intersects that) target.setBounds(
        kotlin.math.max(this.left, that.left), kotlin.math.max(this.top, that.top),
        kotlin.math.min(this.right, that.right), kotlin.math.min(this.bottom, that.bottom)
    ) else null

    fun displaced(dx: Double, dy: Double) = Rectangle(this.x + dx, this.y + dy, width, height)
    fun displace(dx: Double, dy: Double) = setTo(this.x + dx, this.y + dy, this.width, this.height)

    fun inflate(dx: Double, dy: Double) {
        x -= dx; width += 2 * dx
        y -= dy; height += 2 * dy
    }

    fun clone() = Rectangle(x, y, width, height)

    //override fun toString(): String = "Rectangle([${left.niceStr}, ${top.niceStr}]-[${right.niceStr}, ${bottom.niceStr}])"
    override fun toString(): String =
        "Rectangle(x=${x.niceStr}, y=${y.niceStr}, width=${width.niceStr}, height=${height.niceStr})"

    fun toStringBounds(): String =
        "Rectangle([${left.niceStr},${top.niceStr}]-[${right.niceStr},${bottom.niceStr}])"

    fun toInt() = RectangleInt(x, y, width, height)
}

inline fun Rectangle.setTo(x: Number, y: Number, width: Number, height: Number) =
    this.setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

inline fun Rectangle.setBounds(left: Number, top: Number, right: Number, bottom: Number) = setBounds(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

inline operator fun Rectangle.times(scale: Number) = times(scale.toDouble())
inline operator fun Rectangle.div(scale: Number) = div(scale.toDouble())
inline fun Rectangle.contains(x: Number, y: Number) = contains(x.toDouble(), y.toDouble())

inline fun Rectangle.displaced(dx: Number, dy: Number) = displaced(dx.toDouble(), dy.toDouble())
inline fun Rectangle.displace(dx: Number, dy: Number) = displace(dx.toDouble(), dy.toDouble())
inline fun Rectangle.inflate(dx: Number, dy: Number) = inflate(dx.toDouble(), dy.toDouble())

//////////// INT

interface IRectangleInt {
    val x: Int
    val y: Int
    val width: Int
    val height: Int

    companion object {
        inline operator fun invoke(x: Number, y: Number, width: Number, height: Number): IRectangleInt = RectangleInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())
    }
}

val IRectangleInt.left get() = x
val IRectangleInt.top get() = y
val IRectangleInt.right get() = x + width
val IRectangleInt.bottom get() = y + height

inline class RectangleInt(val rect: Rectangle) : IRectangleInt {
    override var x: Int
        set(value) = run { rect.x = value.toDouble() }
        get() = rect.x.toInt()

    override var y: Int
        set(value) = run { rect.y = value.toDouble() }
        get() = rect.y.toInt()

    override var width: Int
        set(value) = run { rect.width = value.toDouble() }
        get() = rect.width.toInt()

    override var height: Int
        set(value) = run { rect.height = value.toDouble() }
        get() = rect.height.toInt()

    var left: Int
        set(value) = run { rect.left = value.toDouble() }
        get() = rect.left.toInt()

    var top: Int
        set(value) = run { rect.top = value.toDouble() }
        get() = rect.top.toInt()

    var right: Int
        set(value) = run { rect.right = value.toDouble() }
        get() = rect.right.toInt()

    var bottom: Int
        set(value) = run { rect.bottom = value.toDouble() }
        get() = rect.bottom.toInt()

    companion object {
        operator fun invoke() = RectangleInt(Rectangle())
        inline operator fun invoke(x: Number, y: Number, width: Number, height: Number) = RectangleInt(Rectangle(x, y, width, height))

        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): RectangleInt =
            RectangleInt(left, top, right - left, bottom - top)
    }

    override fun toString(): String = "Rectangle(x=$x, y=$y, width=$width, height=$height)"
}

fun RectangleInt.setTo(that: RectangleInt) = setTo(that.x, that.y, that.width, that.height)

fun RectangleInt.setTo(x: Int, y: Int, width: Int, height: Int) = this.apply {
    this.x = x
    this.y = y
    this.width = width
    this.height = height
}

fun RectangleInt.setPosition(x: Int, y: Int) = this.apply { this.x = x; this.y = y }

fun RectangleInt.setSize(width: Int, height: Int) = this.apply {
    this.width = width
    this.height = height
}

fun RectangleInt.setBoundsTo(left: Int, top: Int, right: Int, bottom: Int) = setTo(left, top, right - left, bottom - top)

////////////////////

operator fun IRectangleInt.contains(v: SizeInt): Boolean = (v.width <= width) && (v.height <= height)

fun Rectangle.asInt() = RectangleInt(this)
fun RectangleInt.asDouble() = this.rect

val IRectangle.int get() = RectangleInt(x, y, width, height)
val IRectangleInt.float get() = Rectangle(x, y, width, height)

fun IRectangleInt.anchor(ax: Double, ay: Double): IPointInt =
    PointInt((x + width * ax).toInt(), (y + height * ay).toInt())

inline fun IRectangleInt.anchor(ax: Number, ay: Number): IPointInt = anchor(ax.toDouble(), ay.toDouble())

val IRectangleInt.center get() = anchor(0.5, 0.5)

///////////////////////////

fun Iterable<Rectangle>.bounds(target: Rectangle = Rectangle()): Rectangle {
    var first = true
    var left = 0.0
    var right = 0.0
    var top = 0.0
    var bottom = 0.0
    for (r in this) {
        if (first) {
            left = r.left
            right = r.right
            top = r.top
            bottom = r.bottom
            first = false
        } else {
            left = kotlin.math.min(left, r.left)
            right = kotlin.math.max(right, r.right)
            top = kotlin.math.min(top, r.top)
            bottom = kotlin.math.max(bottom, r.bottom)
        }
    }
    return target.setBounds(left, top, right, bottom)
}

val IRectangle.asRectangle: Rectangle get() = Rectangle(x, y, width, height)