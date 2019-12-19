package korma_geom

import korma_geom.shape.Shape2d
import korma_geom.shape.clip
import korma_geom.triangle.Triangle

object LineTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val l1 = Line(Point(10, 10), Point(11,10))
        println(l1.rotate(45.degrees))
//        println(l1.infiniteIntersects(Point(4, 0)))
//        println(l1.intersectsInfiniteDirected(l2))
        val tri = Triangle(Point(100,150), Point(200,250),  Point(300,200), false, false)
        val rec = Shape2d.Rectangle(100, 100, 100, 100)
        println(rec.clip(tri))
    }
}