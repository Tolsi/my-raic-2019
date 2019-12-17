import extensions.tilesToPolygons
import extensions.tilesToRectangles
import extensions.toPoint
import korma_geom.Point
import korma_geom.Rectangle
import korma_geom.shape.Shape2d
import model.Game

object Global {
    lateinit var properties: model.Properties
    lateinit var level: model.Level
    lateinit var wallsAsRectangles: Collection<Rectangle>
    lateinit var wallsAsPolygon: List<Shape2d.Polygon>
    lateinit var jumpPadsAsRectangles: Collection<Rectangle>
    lateinit var laddersAsRectangles: Collection<Rectangle>
    lateinit var platformsAsRectangles: Collection<Rectangle>
    lateinit var startPositions: Map<Int, Point>
    lateinit var debug: Debug

    fun init(game: Game, debug: Debug = Debug.Mock) {
        if (!Global.init) {
            level = game.level
            properties = game.properties
            init = true
            this.debug = debug
            startPositions = game.units.map { it.id to it.position.toPoint() }.toMap()
            wallsAsRectangles = level.tilesToRectangles(model.Tile.WALL)
            wallsAsPolygon = level.tilesToPolygons(model.Tile.WALL)
            jumpPadsAsRectangles = level.tilesToRectangles(model.Tile.JUMP_PAD)
            laddersAsRectangles = level.tilesToRectangles(model.Tile.LADDER)
            platformsAsRectangles = level.tilesToRectangles(model.Tile.PLATFORM)
        }
    }

    public var init: Boolean = false
}