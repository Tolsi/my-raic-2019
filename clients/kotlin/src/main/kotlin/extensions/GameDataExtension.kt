package extensions

import Debug
import Global
import korma_geom.*
import model.*
import model.Unit
import simulation.WorldSimulation
import strategies.*
import java.awt.Color

class GameDataExtension {
    public lateinit var me: model.Unit
    public lateinit var game: Game
    public lateinit var debug: Debug
    public val lastStepsUnits: MutableList<Game> = mutableListOf()
    public var jumpUntil: Int = -1

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
    // todo если у меня больше жизней, чем у врага
    fun isCanHitMyselfOrWithEnemies(target: Point): Boolean {
        val weaponParams = me.weapon!!.params
        if (weaponParams.explosion?.radius ?: 0.0 > 0) {
            val myCenterPosition = me.centerPosition.toPoint()
            val bulletPoints = Line.createFromPointAimAndSpeed(myCenterPosition, target.minus(myCenterPosition).mutable, weaponParams.bullet.speed)
            val collisionPoint = bulletPoints.find { p ->
                Global.wallsAsRectangles.plus(enemies()).any { r ->
                    r.asRectangle.intersects(
                            p.toRectangleWithCenterInPoint(weaponParams.bullet.size))
                }
            } ?: return false
            // todo remove mult after simulation
            val explosionRadiusRectangleForMe = collisionPoint.toRectangleWithCenterInPoint(weaponParams.explosion!!.radius * 1.5)
            val explosionRadiusRectangle = collisionPoint.toRectangleWithCenterInPoint(weaponParams.explosion!!.radius)
            return explosionRadiusRectangleForMe.intersects(me.asRectangle) &&
                    !notMe().any { explosionRadiusRectangle.intersects(it.asRectangle) }
        }
        return false
    }

    fun debugIfIShootNow(target: Point) {
        if (me.weapon != null) {
            val weaponParams = me.weapon!!.params
            val myCenterPosition = me.centerPosition.toPoint()
            val bulletPoints = Line.createFromPointAimAndSpeed(myCenterPosition, target.minus(myCenterPosition).mutable, weaponParams.bullet.speed)
            bulletPoints.forEach { debug.draw(CustomData.Rect(it.toVec2Float(), Vec2Float(0.1f, 0.1f), Color.GRAY.toColorFloat(0.4f))) }
            val collisionPoint = bulletPoints.find { p ->
                Global.wallsAsRectangles.plus(enemies()).any { r ->
                    r.asRectangle.intersects(
                            p.toRectangleWithCenterInPoint(weaponParams.bullet.size))
                }
            } ?: return
            debug.draw(CustomData.Rect(collisionPoint.toVec2Float(), Vec2Float(0.5f, 0.5f), Color.YELLOW.toColorFloat(0.4f)))
            if (weaponParams.explosion != null) {
                val explosionRadiusRectangleForMe = collisionPoint.toRectangleWithCenterInPoint(weaponParams.explosion!!.radius * 1.5)
                val explosionRadiusRectangle = collisionPoint.toRectangleWithCenterInPoint(weaponParams.explosion!!.radius)
                debug.draw(CustomData.Rect(explosionRadiusRectangleForMe.position.toVec2Float(), explosionRadiusRectangleForMe.size.toVec2Float(), Color.BLUE.toColorFloat(0.2f)))
                debug.draw(CustomData.Rect(explosionRadiusRectangle.position.toVec2Float(), explosionRadiusRectangle.size.toVec2Float(), Color.RED.toColorFloat(0.4f)))
            }
        }
    }

    fun debugAllBullets() {
        for (bullet in game.bullets) {
            val weaponParams = game.properties.weaponParams[bullet.weaponType]!!
            val bulletPoints = Line.createFromPointAimAndSpeed(bullet.position.toPoint(), bullet.velocity.toPoint(), weaponParams.bullet.speed)
            bulletPoints.forEach { debug.draw(CustomData.Rect(it.toVec2Float(), Vec2Float(0.1f, 0.1f), Color.GRAY.toColorFloat(0.4f))) }
            val collisionPoint = bulletPoints.find { p ->
                Global.wallsAsRectangles.plus(game.units).any { r ->
                    r.asRectangle.intersects(
                            p.toRectangleWithCenterInPoint(weaponParams.bullet.size))
                }
            } ?: return
            debug.draw(CustomData.Rect(collisionPoint.toVec2Float(), Vec2Float(0.5f, 0.5f), Color.YELLOW.toColorFloat(0.4f)))
            if (weaponParams.explosion != null) {
                val explosionRadiusRectangle = collisionPoint.toRectangleWithCenterInPoint(weaponParams.explosion!!.radius)
                debug.draw(CustomData.Rect(explosionRadiusRectangle.position.toVec2Float(), explosionRadiusRectangle.size.toVec2Float(), Color.RED.toColorFloat(0.4f)))
            }
        }
    }

    fun isCanShoot(): Boolean {
        return me.weapon != null && me.weapon!!.fireTimer ?: 0.0 == 0.0
    }

    fun myStartPosition(): Point {
        return Global.startPositions[me.id]!!
    }

    val myPlayer: Player by lazy { game.players.find { it.id == me.playerId }!! }
    val enemiesPlayers: List<Player> by lazy { game.players.filter { it.id != me.playerId } }
    val enemyId: Int by lazy { game.units.find { me.id != it.id }!!.id }
    fun enemy(): model.Unit { return game.units.find { me.id != it.id }!! }

    fun predictStepsByType(enemyType: EnemyType, steps: Int): List<Point> {
        return predictGamesByType(enemyType, steps).map { it.unitById(enemyId).position.toPoint() }
    }

    fun predictGamesByType(enemyType: EnemyType, steps: Int): List<Game> {
        val sim = WorldSimulation(game)
        val enemyStrategy = when (enemyType) {
            EnemyType.SmartGuy -> QuickStartStrategy()
            EnemyType.FastSmartGuy -> FastQuickStartStrategy()
            EnemyType.FastJumpySmartGuy -> FastJumpyQuickStartStrategy()
            EnemyType.Empty -> EmptyStrategy()
            EnemyType.Custom -> TODO()
        }
        val myTestStrategy = FastJumpyQuickStartStrategy()
        return (0..steps).fold(emptyList()) { games, i ->
            val lastGame = games.lastOrNull() ?: game
            games.plus(sim.tick(mapOf(me.id to myTestStrategy.getAction(lastGame.unitById(me.id), lastGame, debug),
                    enemyId to enemyStrategy.getAction(lastGame.unitById(enemyId), lastGame, debug))))
        }
    }

    fun canHitToTarget(enemyType: EnemyType): Point? {
        if (enemyType == EnemyType.Custom) return enemy().position.toPoint()
        return predictStepsByType(enemyType, (me.position.distanceTo(enemy().position)).toInt()).last()
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

fun model.Level.tilesToRectanglesWithBorders(tileType: model.Tile): Collection<Rectangle> {
    val tiles = this.tiles.mapIndexed { x, line ->
        line.mapIndexed { y, tile ->
            if (tile == tileType) {
                Rectangle(x, y, 1, 1)
            } else null
        }
    }.flatten().filterNotNull()

    val borders = mutableListOf<Rectangle>()
    for (y in -1..Global.level.tiles[0].size + 1) {
        borders.plus(Rectangle(-1, y, 1, 1))
    }
    for (x in -1..Global.level.tiles.size + 1) {
        borders.plus(Rectangle(x, -1, 1, 1))
    }
    return tiles.plus(borders)
}

fun model.Level.tilesToRectangles(tileType: model.Tile): Collection<Rectangle> {
    val tiles = this.tiles.mapIndexed { x, line ->
        line.mapIndexed { y, tile ->
            if (tile == tileType) {
                Rectangle(x, y, 1, 1)
            } else null
        }
    }.flatten().filterNotNull()

    return tiles
}

fun Point.toRectangleWithCenterInPoint(radius: Double): Rectangle {
    return Rectangle(this.x - radius, this.y - radius, radius * 2, radius * 2)
}

fun Unit.isStaysOnMe(unit: model.Unit): Boolean {
    return this.position.y - unit.topCenterPosition.y <= (Global.properties.unitMaxHorizontalSpeedPerTick )
}

fun java.awt.Color.toColorFloat(a: Float? = null): ColorFloat = ColorFloat(this.red.toFloat(), this.green.toFloat(), this.blue.toFloat(), a
        ?: this.alpha.toFloat())

fun Size.toVec2Float(): Vec2Float = Vec2Float(this.width.toFloat(), this.height.toFloat())
fun Game.unitById(unitId: Int): Unit = this.units.find { it.id == unitId }!!
fun Unit.isFalling(): Boolean {
    return this.jumpState == JumpState.Falling
}

fun Point.onTile(): Tile {
    return Global.level.tiles[this.x.toInt()][this.y.toInt()]
}

fun Unit.underMeTile(): Tile {
    return Global.level.tiles[this.positionInt.x][this.positionInt.y - 1]
}

fun Unit.upperMeTile(): Tile {
    return Global.level.tiles[this.positionInt.x][this.positionInt.y + 1]
}

fun Unit.bottomSide(): Collection<Point> {
    return listOf(Point(Math.round(x), y),
            Point(Math.round(x + width), y))
}

fun Unit.topSide(): Collection<Point> {
    return listOf(Point(Math.round(x), y + height),
            Point(Math.round(x + width), y + height))
}

fun Unit.leftSide(): Collection<Point> {
    return listOf(Point(x, Math.round(y)),
            Point(x, Math.round(y + height)))
}

fun Unit.rightSide(): Collection<Point> {
    return listOf(Point(x + width, Math.round(y)),
            Point(x + width, Math.round(y + height)))
}

fun Unit.centerAndBootom(): Collection<Point> {
    return listOf(Point(position.x, y + height + WorldSimulation.EPS), Point(position.x, y + WorldSimulation.EPS))
}
fun Unit.isOnLadder(): Boolean {
    return this.centerAndBootom().any { p -> Global.laddersAsRectangles.any { it.contains(p) } }
}