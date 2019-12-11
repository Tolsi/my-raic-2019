import extensions.toPoint
import extensions.toRectangles
import korma_geom.Point
import korma_geom.Rectangle
import model.Game

object Global {
    var properties: model.Properties = model.Properties()
    var level: model.Level = model.Level()
    var levelAsRectangles: Collection<Rectangle> = emptyList()
    var startPositions: Map<Int, Point> = emptyMap()

    fun init(game: Game) {
        if (!Global.init) {
            level = game.level
            properties = game.properties
            levelAsRectangles = level.toRectangles(game)
            init = true
            startPositions = game.units.map { it.id to it.position.toPoint() }.toMap()
        }
    }

    public var init: Boolean = false
}