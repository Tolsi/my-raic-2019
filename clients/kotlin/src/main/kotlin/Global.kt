import extensions.tilesToRectangles
import extensions.toPoint
import korma_geom.Point
import korma_geom.Rectangle
import model.Game

object Global {
    var properties: model.Properties = model.Properties()
    var level: model.Level = model.Level()
    var wallsAsRectangles: Collection<Rectangle> = emptyList()
    var jumpPadsAsRectangles: Collection<Rectangle> = emptyList()
    var laddersAsRectangles: Collection<Rectangle> = emptyList()
    var platformsAsRectangles: Collection<Rectangle> = emptyList()
    var startPositions: Map<Int, Point> = emptyMap()

    fun init(game: Game) {
        if (!Global.init) {
            level = game.level
            properties = game.properties
            wallsAsRectangles = level.tilesToRectangles(game, model.Tile.WALL)
            jumpPadsAsRectangles = level.tilesToRectangles(game, model.Tile.JUMP_PAD)
            laddersAsRectangles = level.tilesToRectangles(game, model.Tile.LADDER)
            platformsAsRectangles = level.tilesToRectangles(game, model.Tile.PLATFORM)
            init = true
            startPositions = game.units.map { it.id to it.position.toPoint() }.toMap()
        }
    }

    public var init: Boolean = false
}