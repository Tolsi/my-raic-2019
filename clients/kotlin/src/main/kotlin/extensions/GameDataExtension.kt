package extensions

import Debug
import Global
import korma_geom.*
import korma_geom.shape.*
import korma_geom.triangle.Triangle
import korma_geom.triangle.pointInsideTriangle
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

    public fun friends(): List<model.Unit> {
        return notMe().filter { it.playerId == me.playerId }
    }

    public inline fun <reified T : model.Item> nearestItemType(): LootBox? {
        return game.lootBoxes.findAmongBy({ it.item is T }, { distanceToMe(it.position).toInt() })
    }


    fun distanceSqr(a: Vec2Double, b: Vec2Double): Double {
        return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
    }

    fun distanceToMe(smtn: Vec2Double): Double = distanceSqr(smtn, me.position)

    // todo remove after deikstra korma_geom.algo will be realized
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

    fun debugDrawWalls() {
        for (r in Global.wallsAsRectangles) {
            debug.draw(CustomData.Rect(r.position.toVec2Float(), Vec2Float(r.size.width.toFloat(), r.size.height.toFloat()), Color.CYAN.toColorFloat(0.5f)))
        }
    }

    fun drawWallsPolygons() {
        for (p in Global.wallsAsPolygon) {
            drawPolygon(p)
        }
    }

    fun drawViewAreaAndWallsIntersections() {
        if (me.weapon != null && me.weapon!!.lastAngle != null) {
            val fromPoint = me.centerPosition.toPoint()
            val zeroAngleLine = fromPoint.toZeroAngleLine()
            val aimLine = zeroAngleLine.rotate(me.weapon!!.lastAngle!!.radians)
            val aimBoundsPoints = me.weapon!!.spreadRange().bounds().toList().map { angle ->
                Global.level.boundLines().asSequence().mapNotNull {
                    // todo intersectsDirected
                    zeroAngleLine.rotate(angle).intersectsInfiniteDirected(it)
                }.first()
            }

            val triangle = Triangle(aimBoundsPoints.get(0), fromPoint, aimBoundsPoints.get(1), fixOrientation = false, checkOrientation = false)

            val enemiesPolygons = enemies().map { it.asRectangle.toPolygon() }
            val friendsPolygons = friends().map { it.asRectangle.toPolygon() }

            val clippedWallsPolygons = Global.wallsAsPolygon.mapNotNull { p ->
                p.clip(triangle.toPolygon()).takeIf { it.points.isNotEmpty() }
            }
            val clippedEnemiesPolygons = enemiesPolygons.mapNotNull { p ->
                p.toPolygon()
            }
            val clippedFriendsPolygons = friendsPolygons.mapNotNull { p ->
                p.toPolygon()
            }

            var scanLine = aimLine.rotate(90.degrees)
            var leftAimLine = Line(fromPoint, aimBoundsPoints.get(1))
            var rightAimLine = Line(fromPoint, aimBoundsPoints.get(0))

//            drawPolygon(triangle.toPolygon())
            var iterateTriangle = triangle


            // todo нахождение лицевых граней полигонов, сейчас это все
            val allPoints = clippedWallsPolygons.plus(clippedEnemiesPolygons).plus(clippedFriendsPolygons).flatMap { poly -> poly.points.map { it to poly } }
            allPoints.sortedBy { fromPoint.distanceTo(it.first) }.forEach { (point, poly) ->
                if (!iterateTriangle.pointInsideTriangle(point)) {
                    return@forEach
                }
                val movedScanLine = scanLine.moveTo(point)
                val leftPoint = leftAimLine.infiniteIntersects(movedScanLine)!!
                val rightPoint = rightAimLine.infiniteIntersects(movedScanLine)!!
//                debug.draw(CustomData.Line(fromPoint.toVec2Float(), leftPoint.toVec2Float(), 0.1f, Color.GRAY.toColorFloat()))
//                debug.draw(CustomData.Line(fromPoint.toVec2Float(), rightPoint.toVec2Float(), 0.1f, Color.GRAY.toColorFloat()))
                val line = Line(leftPoint, rightPoint)
//                drawPolygon(poly)
                drawPolygon(iterateTriangle.toPolygon())
                debug.draw(CustomData.Line(leftPoint.toVec2Float(), rightPoint.toVec2Float(), 0.1f, Color.GRAY.toColorFloat()))
                val clipped = poly.lineView(line)?.points() ?: listOf()
                clipped.forEach {
                    debug.draw(CustomData.Rect(it.toVec2Float(), Vec2Float(0.2f, 0.2f), Color.YELLOW.toColorFloat()))
                }
                if (clipped.size >= 2) {
                    debug.draw(CustomData.Line(clipped.first().toVec2Float(), clipped.last().toVec2Float(), 0.2f, Color.MAGENTA.toColorFloat()))
                    val uniquePoint = clipped.epsRemove(line.points()).toSet().firstOrNull()
                    if (uniquePoint != null) {
                        if (clipped.epsContains(leftPoint)) {
                            leftAimLine = Line(fromPoint, uniquePoint)
                        } else if (clipped.epsContains(rightPoint)) {
                            rightAimLine = Line(fromPoint, uniquePoint)
                        } else {
                            val i = 0
                        }
                        //                    debug.draw(CustomData.Line(fromPoint.toVec2Float(), leftAimLine.to.toVec2Float(), 0.1f, Color.GREEN.toColorFloat()))
//                    debug.draw(CustomData.Line(fromPoint.toVec2Float(), rightAimLine.to.toVec2Float(), 0.1f, Color.GREEN.toColorFloat()))
                        iterateTriangle = Triangle(leftAimLine.times(10.0).to, fromPoint, rightAimLine.times(10.0).to, fixOrientation = false, checkOrientation = false)
                        val i = 0
                    }
                }
            }


        }
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
    fun enemy(): model.Unit {
        return game.units.find { me.id != it.id }!!
    }

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

    fun predictTarget(enemyType: EnemyType): Point? {
        if (enemyType == EnemyType.Custom) return enemy().position.toPoint()
        // 6 тиков = клетка
        val predictedPosition = predictStepsByType(enemyType, (me.position.distanceTo(enemy().position) / (me.weapon?.params?.bullet?.speedPerTick
                ?: 10.0)).toInt()).last()
        return predictedPosition.setTo(predictedPosition.x, predictedPosition.y + me.size.y / 2)
    }

    fun isCanHitTargetNow(targetUnit: Unit) {
        // todo 0 or PI? move direction?
        val lastWeaponAngle = me.weapon!!.lastAngle ?: Math.PI
        val spread = me.weapon!!.spread
        // todo подходящий угол и шанс попасть в стену не велик
        spread.minus(targetUnit.centricPoints.sumByDouble { p -> Angle.fromRadians(lastWeaponAngle).shortDistanceTo(me.centerPosition.angleTo(p)).radians }) < 2
    }

    companion object {
        fun drawDebugGrid(game: Game, debug: Debug) {
            for (x in 0..game.level.width.toInt()) {
                val color = if (x % 5 == 0) Color.YELLOW.toColorFloat(0.4f) else Color.WHITE.toColorFloat(0.4f)
                debug.draw(CustomData.Line(Vec2Float(x.toFloat(), 0.0f), Vec2Float(x.toFloat(), game.level.height.toFloat()), 0.1f, color))
                debug.draw(CustomData.PlacedText("$x", Vec2Float(x.toFloat(), -1f), TextAlignment.LEFT, 16f, Color.WHITE.toColorFloat()))
            }
            for (y in 0..game.level.height.toInt()) {
                val color = if (y % 5 == 0) Color.YELLOW.toColorFloat(0.4f) else Color.WHITE.toColorFloat(0.4f)
                debug.draw(CustomData.Line(Vec2Float(0.0f, y.toFloat()), Vec2Float(game.level.width.toFloat(), y.toFloat()), 0.1f, color))
                debug.draw(CustomData.PlacedText("$y", Vec2Float(-1f, y.toFloat() - 0.5f), TextAlignment.LEFT, 16f, Color.WHITE.toColorFloat()))
            }
        }

        fun drawPolygon(p: Shape2d.Polygon, alpha: Float = 0.5f) {
            val c = randomColor().toColorFloat(alpha)
            val pc = randomColor().toColorFloat(alpha)
            p.closedPoints.windowed(2).forEach { (f, s) ->
                Global.debug.draw(CustomData.Line(f.toVec2Float(), s.toVec2Float(), 0.2f, c))
            }
            for (point in p.points) {
                Global.debug.draw(CustomData.Rect(point.toVec2Float(), Vec2Float(0.2f, 0.2f), pc))
            }
        }
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
//
//fun model.Level.tilesToRectanglesWithoutBounds(tileType: model.Tile): Collection<Rectangle> {
//    val tiles = this.tiles.mapIndexed { x, line ->
//        line.mapIndexed { y, tile ->
//            if (x == Global.level.x.toInt() || x == Global.level.width.toInt() ||
//                    y == Global.level.y.toInt() || y == Global.level.height.toInt()) {
//                null
//            } else if (tile == tileType) {
//                Rectangle(x, y, 1, 1)
//            } else null
//        }
//    }.flatten().filterNotNull()
//
//    return tiles
//}

fun model.Level.tilesToPolygons(tileType: model.Tile): List<Shape2d.Polygon> {
    val result = mutableSetOf<Shape2d.Polygon>()

    this.tiles.forEachIndexed { x, line ->
        line.forEachIndexed { y, tile ->
            if (tile == tileType &&
                    x != 0 &&
                    x != Global.level.width.toInt() - 1 &&
//                    y != 0 &&
                    y != Global.level.height.toInt() - 1) {
                val rect = Shape2d.Rectangle(x, y, 1, 1)
                if (result.isEmpty()) {
                    result.add(rect.toPolygon())
                } else {
                    val mergedPolygonAndResults = result.asSequence().map { it to it.merge(rect) }.filter { it.second != null }.toList()
                    if (mergedPolygonAndResults.isEmpty()) {
                        result.add(rect.toPolygon())
                    } else {
                        val allMerged = mergedPolygonAndResults.map { it.second!! }.reduce { f, s -> f.merge(s)!! }
                        mergedPolygonAndResults.forEach { result.remove(it.first) }
                        result.add(allMerged)
                    }
                }
            }
        }
    }

    return result.sortedBy { it.points.size }.map { it.travellingSalesmanProblem() }.map { it.simplify() }.toList()
}

fun Point.toRectangleWithCenterInPoint(radius: Double): Rectangle {
    return Rectangle(this.x - radius, this.y - radius, radius * 2, radius * 2)
}

fun Unit.isStaysOnMe(unit: model.Unit): Boolean {
    return this.position.y - unit.topCenterPosition.y <= (Global.properties.unitMaxHorizontalSpeedPerTick)
}

fun java.awt.Color.toColorFloat(a: Float? = null): ColorFloat = ColorFloat(this.red.toFloat(), this.green.toFloat(), this.blue.toFloat(), a
        ?: this.alpha.toFloat())

fun Size.toVec2Float(): Vec2Float = Vec2Float(this.width.toFloat(), this.height.toFloat())
fun Game.unitById(unitId: Int): Unit = this.units.find { it.id == unitId }!!
fun Unit.isFalling(): Boolean {
    return this.jumpState == JumpState.Falling
}

fun Point.onTile(): Tile? {
    val x = this.x.toInt()
    val y = this.y.toInt()
    return if (y >= 0 && y <= Global.level.tiles[0].size
            && x >= 0 && x <= Global.level.tiles.size) {
        Global.level.tiles[x][y]
    } else {
        null
    }
}

fun Point.onTileOrEmpty(): Tile {
    return onTile() ?: Tile.EMPTY
}

fun Unit.underMeTile(): Tile? {
    return if (this.positionInt.y - 1 >= 0 && this.positionInt.y - 1 <= Global.level.tiles[0].size
            && this.positionInt.x >= 0 && this.positionInt.x <= Global.level.tiles.size) {
        Global.level.tiles[this.positionInt.x][this.positionInt.y - 1]
    } else {
        null
    }
}

fun Unit.upperMeTile(): Tile? {
    return if (this.positionInt.y + 1 >= 0 && this.positionInt.y + 1 <= Global.level.tiles[0].size
            && this.positionInt.x >= 0 && this.positionInt.x <= Global.level.tiles.size) {
        Global.level.tiles[this.positionInt.x][this.positionInt.y + 1]
    } else {
        null
    }
}

fun IRectangle.bottomSide(): Collection<Point> {
    return listOf(Point(Math.round(x), y),
            Point(Math.round(x + width), y))
}

fun IRectangle.topSide(): Collection<Point> {
    return listOf(Point(Math.round(x), y + height),
            Point(Math.round(x + width), y + height))
}

fun IRectangle.leftSide(): Collection<Point> {
    return listOf(Point(x, Math.round(y)),
            Point(x, Math.round(y + height)))
}

fun IRectangle.rightSide(): Collection<Point> {
    return listOf(Point(x + width, Math.round(y)),
            Point(x + width, Math.round(y + height)))
}

fun Unit.centerAndBootom(): Collection<Point> {
    return listOf(Point(position.x, y + height + WorldSimulation.EPS), Point(position.x, y + WorldSimulation.EPS))
}

fun Unit.isOnLadder(): Boolean {
    return this.centerAndBootom().any { p -> Global.laddersAsRectangles.any { it.contains(p) } }
}

fun Weapon.spreadRange(): ClosedRange<Angle> {
    return (Angle.fromRadians(this.lastAngle!! - this.spread)).rangeTo(Angle.fromRadians(this.lastAngle!! + this.spread))
}

fun IRectangle.boundLines(): Collection<Line> {
    return listOf(
            this.leftSide().toLine(),
            this.rightSide().toLine(),
            this.topSide().toLine(),
            this.bottomSide().toLine())
}

val allColors = listOf(Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY, Color.GRAY, Color.GREEN, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED, Color.WHITE, Color.YELLOW)

fun randomColor(): Color {
    return allColors.random()
}

fun <T> List<T>.permutations(): Sequence<List<T>> {
    if (this.size == 1) return sequenceOf(this)
    val list = this
    return sequence {
        val sub = list.get(0)
        for (perm in list.drop(1).permutations())
            for (i in 0..perm.size) {
                val newPerm = perm.toMutableList()
                newPerm.add(i, sub)
                yield(newPerm)
            }
    }
}

fun <T : Comparable<T>> ClosedRange<T>.bounds(): Pair<T, T> {
    return start to endInclusive
}

fun Point.toZeroAngleLine(): Line {
    return Line(this, this.right)
}

fun List<Point>.epsUnique(): List<Point> {
    return this.fold(listOf()) { res, p ->
        if (!res.any { it.compareTo(p) == 0 }) {
            res.plus(p)
        } else res
    }
}

fun List<Point>.epsRemove(remove: Point): List<Point> {
    return this.fold(mutableListOf()) { res, p ->
        if (p.compareTo(remove) != 0) {
            res.add(p)
            res
        } else {
            res
        }
    }
}

fun List<Point>.epsRemove(remove: List<Point>): List<Point> {
    return this.fold(mutableListOf()) { res, p ->
        if (remove.all { p.compareTo(it) != 0 }) {
            res.add(p)
            res
        } else {
            res
        }
    }
}

fun List<Point>.epsContains(p: Point): Boolean {
    return this.any { it.compareTo(p) == 0 }
}

fun Point.projectTo(l: Line): Point {
    // get dot product of e1, e2
    val e1 = Point(l.to.x - l.from.x, l.to.y - l.from.y)
    val e2 = Point(x - l.from.x, y - l.from.y)
    val valDp = e1.dot(e2)
    // get squared length of e1
    val len = e1.dot(e1)
    return Point(l.from.x + valDp * e1.x / len,
            l.from.y + valDp * e1.y / len)
}

fun Line.moveTo(point: Point): Line {
    val normalized = this.toLenghtOneLine().normalize()
    return Line(point, point.plus(normalized.to).mutable)
}

fun Shape2d.Polygon.edgeIntersections(clipper: Line): List<Point> {
    return this.closedPoints.windowed(2)
            .mapNotNull {
                clipper.intersectsDirected(Line(it.get(0), it.get(1)))
            }
}

fun Shape2d.Polygon.lineView(clipper: Line): Line? {
    val pointsInsidePoly = clipper.points().filter { isPointInside(it) }
    return when (pointsInsidePoly.size) {
        1 -> {
            val edgeIntersection = this.closedPoints.windowed(2).mapNotNull { Line(it.get(0), it.get(1)).intersects(clipper) }
            if (edgeIntersection.isNotEmpty()) {
                val fromLeft = edgeIntersection.map { it to clipper.from.distanceTo(it) }
                val fromRight = edgeIntersection.map { it to clipper.to.distanceTo(it) }
                val smallestFromLeft = fromLeft.minBy { it.second }!!.second
                val smallestFromRight = fromRight.minBy { it.second }!!.second
                val point = if (smallestFromLeft < smallestFromRight) {
                    fromLeft.maxBy { it.second }?.first
                } else {
                    fromRight.maxBy { it.second }?.first
                }
                // todo omg
                if (point.let { it != pointsInsidePoly.first() }) {
                    point?.let { Line(pointsInsidePoly.first(), it) }
                } else null
            } else null
        }
        2 -> clipper
        0 -> {
            val clipped = this.clip(Shape2d.Line(clipper.from, clipper.to)).points.epsUnique()
            if (clipped.size == 2) {
                Line(clipped[0], clipped[1])
            } else null
        }
        else -> null
    }
}

fun Shape2d.Polygon.isPointInside(point: Point): Boolean {
    // todo 100?!
    // todo пересекает ребро или вершину
    val isIntersectsLine = this.closedPoints.windowed(2).asSequence()
            .any { Line(it.get(0), it.get(1)).intersects(point) }
    return isIntersectsLine || edgeIntersections(Line(point, point.right).times(100.0)).toList().size % 2 != 0
}