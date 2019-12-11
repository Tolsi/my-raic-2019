package extensions

import Debug
import Global
import korma_geom.*
import model.*
import model.Unit
import simulation.WorldSimulation

class GameDataExtension {
    public lateinit var me: model.Unit
    public lateinit var game: Game
    public lateinit var debug: Debug
    public val lastStepsUnits: MutableList<Game> = mutableListOf()

    constructor() {
        me = Unit()
        game = Game()
        debug = Debug.Mock
    }

    constructor(me: model.Unit, game: Game, debug: Debug) {
        update(me, game, debug)
    }

    fun update(me: model.Unit, game: Game, debug: Debug) {
        this.me = me
        this.game = game
        this.debug = debug
    }

    public fun nearestEnemy(): model.Unit? {
        return game.units.findAmongBy({ it.playerId != me.playerId }, { distanceToMe(it.position).toInt() })
    }

    public fun enemies(): List<model.Unit> {
        return game.units.filter { it.playerId != me.playerId }
    }

    public fun notMe(): List<model.Unit> {
        return game.units.filter { it.id != me.id }
    }

    public inline fun <reified T : model.Item> nearestItemType(): LootBox? {
        return game.lootBoxes.findAmongBy({ it.item is T }, { distanceToMe(it.position).toInt() })
    }


    fun distanceSqr(a: Vec2Double, b: Vec2Double): Double {
        return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
    }

    fun distanceToMe(smtn: Vec2Double): Double = distanceSqr(smtn, me.position)

    // todo remove after deikstra algo will be realized
    fun isStayOnPlaceLastMoves(n: Int): Boolean {
        return lastStepsUnits.size >= n &&
                lastStepsUnits.reversed().take(n).map { it.units.find { it.id == me.id }!!.position }.toSet().size == 1
    }

    // todo что будет если я выстрелю сейчас? попаду ли я в себя?
    // todo предсказывать движения себя и противка и пуль и смотреть куда стрелять
    // todo стрелять на опережение
    fun isCanHitMyself(target: Point): Boolean {
        val weaponParams = me.weapon!!.params
        if (weaponParams.explosion?.radius ?: 0.0 > 0) {
            val collisionPoint = Line.createFromPointAimAndSpeed(me.centerPosition.toPoint(), target, weaponParams.bullet.speed).find { p ->
                Global.wallsAsRectangles.plus(enemies()).any { r -> r.asRectangle.intersects(p.toRectangleWithCenterInPoint(weaponParams.bullet.size)) }
            } ?: return false
            val explosionRadiusRectangle = collisionPoint.toRectangleWithCenterInPoint(weaponParams.explosion!!.radius)
            return explosionRadiusRectangle.intersects(me.asRectangle) &&
                    !notMe().any { explosionRadiusRectangle.intersects(it.asRectangle) }
        }
        return false
    }

    fun isCanShoot(): Boolean {
        return me.weapon != null && me.weapon!!.fireTimer ?: 0.0 == 0.0
    }

    fun myStartPosition(): Point {
        return Global.startPositions[me.id]!!
    }
}

fun <T> Array<T>.findAmongBy(filter: (T) -> Boolean,
                             compare: (T) -> Int): T? {
    return this.filter(filter).sortedBy(compare).firstOrNull()
}

fun IPoint.toVec2Float(): Vec2Float {
    return Vec2Float(this.x.toFloat(), this.y.toFloat())
}

fun IPoint.toVec2Double(): Vec2Double {
    return Vec2Double(this.x, this.y)
}

fun Vec2Double.toPoint(): Point {
    return Point(this.x, this.y)
}

fun Vec2Double.toVec2Float(): Vec2Float {
    return Vec2Float(this.x.toFloat(), this.y.toFloat())
}

fun model.Level.tilesToRectangles(game: Game, tile: model.Tile): Collection<Rectangle> {
    val tiles = this.tiles.mapIndexed { x, line ->
        line.mapIndexed { y, tile ->
            if (tile == tile) {
                Rectangle(x, y, 1, 1)
            } else null
        }
    }.flatten().filterNotNull()

    val field = mutableListOf<Rectangle>()
    for (y in -1..Global.level.tiles[0].size + 1) {
        field.plus(Rectangle(-1, y, 1, 1))
    }
    for (x in -1..Global.level.tiles.size + 1) {
        field.plus(Rectangle(x, -1, 1, 1))
    }
    return tiles.plus(field)
}

fun Point.toRectangleWithCenterInPoint(radius: Double): Rectangle {
    return Rectangle(this.x - radius, this.y - radius, radius * 2, radius * 2)
}

fun Unit.isStaysOnMe(unit: model.Unit): Boolean {
    return this.position.y - unit.topCenterPosition.y <= WorldSimulation.EPS
}