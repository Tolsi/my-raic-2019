package korma_geom

import extensions.lineView
import korma_geom.shape.Shape2d
import korma_geom.shape.clip
import korma_geom.shape.toPolygon
import korma_geom.triangle.Triangle

object LineTest {
    @JvmStatic
    fun main(args: Array<String>) {
//        val l1 = Line(Point(10, 10), Point(11,10))
//        println(l1.rotate(45.degrees))
//        println(l1.infiniteIntersects(Point(4, 0)))
//        println(l1.intersectsInfiniteDirected(l2))
//        val tri = Triangle(Point(100,150), Point(200,250),  Point(300,200), false, false)
//        val rec = Shape2d.Line(100, 100, 200, 200)
//        println(rec.clip(tri))
        val rec = listOf(Point(4.286754508188248,20.000000001),
                Point(4.286754508188248,21.800000001),
                Point(5.1867545081882485,21.800000001),
                Point(5.1867545081882485,20.000000001)).toPolygon()
        val l = Line(Point(5.186754508188244,19.772884087072057), Point(5.186754508188244, 23.638145154343608))
        println(rec.lineView(l))
    }
}