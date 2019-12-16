package korma_geom

import korma_geom.shape.Shape2d
import korma_geom.shape.clip
import korma_geom.triangle.Triangle

object LineTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val l1 = Line(Point(0, 0), Point(3,0))
        println(l1.infiniteDistance(Point(5,2)))
        val tri = Triangle(Point(100,150), Point(200,250),  Point(300,200), true, true)
        val rec = Shape2d.Rectangle(Rectangle.fromBounds(150, 150, 200, 200))
        println(tri.clip(rec))
    }
}