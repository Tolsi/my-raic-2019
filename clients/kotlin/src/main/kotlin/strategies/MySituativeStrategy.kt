package strategies

import Debug
import Global
import Strategy
import extensions.*
import korma_geom.*
import model.*
import model.Unit
import java.awt.Color

enum class EnemyType {
    SmartGuy,
    FastSmartGuy,
    FastJumpySmartGuy,
    Custom,
    Empty
}

open class MySituativeStrategy : Strategy() {
    private val s = GameDataExtension()

    //    private var enemyType: EnemyType = EnemyType.Custom
    private var enemyPredictionsTypes: MutableMap<Int, Map<EnemyType, List<Point>>> = mutableMapOf()
    private var enemyPredictedType: MutableMap<Int, EnemyType> = mutableMapOf()

    override fun getAction(me: model.Unit, game: Game, debug: Debug): UnitAction {
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

        Global.init(game)

        s.update(me, game, debug)

        if (game.units.size > 2) {
            return UnitAction.Empty
        }

        val simSteps = 70
        if (enemyPredictionsTypes.isEmpty() && enemyPredictedType.isEmpty()) {
            enemyPredictionsTypes.put(s.enemy().id,
                    listOf(EnemyType.SmartGuy, EnemyType.FastSmartGuy, EnemyType.FastJumpySmartGuy, EnemyType.Empty).map { type ->
                        type to s.predictStepsByType(type, simSteps)
                    }.toMap()
            )
        } else {
            if (enemyPredictedType.isEmpty()) {
                val typesAndPoints = enemyPredictionsTypes[s.enemy().id]!!
                val looksLikeStrategies = typesAndPoints.filter { (enemyType, points) ->
                    val point = points.get(game.currentTick - 1)
                    debug.draw(CustomData.Rect(point.toVec2Float(), Vec2Float(0.3f, 0.3f), Color.PINK.toColorFloat()))
                    point.distanceTo(s.enemy().position) < 2
                }
                if (looksLikeStrategies.isEmpty()) {
                    enemyPredictedType.put(s.enemy().id, EnemyType.Custom)
                    enemyPredictionsTypes.remove(s.enemy().id)
                } else if (looksLikeStrategies.size == 1) {
                    enemyPredictedType.put(s.enemy().id, looksLikeStrategies.keys.first())
                    enemyPredictionsTypes.remove(s.enemy().id)
                } else if (game.currentTick - 1 >= simSteps) {
                    enemyPredictedType.put(s.enemy().id, looksLikeStrategies.keys.sortedBy { it.ordinal }.last())
                    enemyPredictionsTypes.remove(s.enemy().id)
                } else {
                    enemyPredictionsTypes.put(s.enemy().id, looksLikeStrategies)
                }
            }
        }

        val targetToUnit: Unit = s.enemy()
        // enemy should be be always
        val nearestWeapon: LootBox? = s.nearestItemType<Item.Weapon>()
        val nearestHealthPack = s.nearestItemType<Item.HealthPack>()

        val goToPoint: Vec2Double = if (me.health < game.properties.unitMaxHealth *
                if (targetToUnit.weapon?.typ == WeaponType.ROCKET_LAUNCHER) 0.8 else 0.45) {
            nearestHealthPack?.position ?: s.myStartPosition().toVec2Double()
        } else if (me.weapon == null && nearestWeapon != null) {
            nearestWeapon.centricPoints.farPoint(me.position.toPoint())!!.toVec2Double()
        } else if (nearestHealthPack != null) {
            val healthPackCloserToMeThanEnemy =
                    nearestHealthPack.position.distanceTo(me.position) < targetToUnit.position.distanceTo(me.position)
            val endOfGameAndILose =
                    game.currentTick > Global.properties.maxTickCount * 0.7 &&
                            s.myPlayer.score <= s.enemiesPlayers.maxBy { it.score }!!.score
            if (healthPackCloserToMeThanEnemy || endOfGameAndILose) {
                targetToUnit.topCenterPosition
            } else {
                nearestHealthPack.position
            }
        } else targetToUnit.topCenterPosition ?: s.myStartPosition().toVec2Double()

        // todo стрелять наперед - надо доделать!
        val mayBeAimTo = s.predictTarget(enemyPredictedType.getOrDefault(s.enemy().id, EnemyType.SmartGuy))
        val targetPoint = mayBeAimTo?.toVec2Double() ?: targetToUnit.centerPosition
//        val targetPoint = targetToUnit.centerPosition
        debug.draw(CustomData.Line(me.centerPosition.toVec2Float(), targetPoint.toVec2Float(), 0.1f, ColorFloat.Red))

        val aim = targetPoint.minus(me.centerPosition)
        val shoot = s.isCanShoot() &&
                !s.isCanHitMyselfOrWithEnemies(targetToUnit.centerPosition.toPoint())
                && me.weapon!!.spread.radians.degrees < 3
//                && mayBeAimTo != null

        var jump = game.currentTick <= s.jumpUntil || goToPoint.y > me.position.y
        if (goToPoint.x > me.position.x &&
                game.level.tiles[(me.position.x + 1).toInt()][(me.position.y).toInt()] == Tile.WALL) {
            jump = true
        }
        if (goToPoint.x < me.position.x &&
                game.level.tiles[(me.position.x - 1).toInt()][(me.position.y).toInt()] == Tile.WALL) {
            jump = true
        }
        if (!jump && s.isStayOnPlaceLastMoves(5)) {
            jump = true
            s.jumpUntil = game.currentTick + 10
        }

        var jumpDown = goToPoint.y < me.position.y
        if (s.lastStepsUnits.isNotEmpty()) {
            val myLastPosition = s.lastStepsUnits.last().units.find { it.id == me.id }!!.position
            val myLastXTileIndex = myLastPosition.x.toInt()
            val myLastYTileIndex = myLastPosition.y.toInt()
            if (game.level.tiles[myLastXTileIndex][myLastYTileIndex] == Tile.PLATFORM &&
                    me.positionInt.x == myLastXTileIndex && me.positionInt.y == myLastYTileIndex + 1) {
                jump = false
                jumpDown = false
            }
        }

//        if (nearestWeapon!= null) {
//            debug.draw(CustomData.Rect(nearestWeapon.position.toVec2Float(), nearestWeapon.size.toVec2Float(), ColorFloat.Green))
//        }
//        line to go to point
//        debug.draw(CustomData.Line(me.position.toVec2Float(), goToPoint.toVec2Float(), 0.2f, ColorFloat.Green))
//        debug level
        for (r in Global.wallsAsRectangles) {
            debug.draw(CustomData.Rect(r.position.toVec2Float(), Vec2Float(r.size.width.toFloat(), r.size.height.toFloat()), Color.CYAN.toColorFloat(0.5f)))
        }
//        for (p in Global.wallsAsPolygon) {
//            debug.draw(CustomData.Polygon(p.points.map {
//                ColoredVertex(it.toVec2Float(), Color.BLUE.toColorFloat(0.3f))
//            }.toTypedArray()))
//        }
        for (p in Global.wallsAsPolygon) {
            for (point in p.points) {
                debug.draw(CustomData.Rect(point.toVec2Float(), Vec2Float(0.2f, 0.2f), ColorFloat.Blue))
            }
        }
//        s.debugAllBullets()
//        targetToUnit?.let {
//            debug.draw(CustomData.Rect(it.position.toVec2Float(), Vec2Float(0.3f, 0.3f), Color.RED.toColorFloat(0.5f)))
//            s.debugIfIShootNow(it.position.toPoint())
//        }

        val action = UnitAction()
        action.velocity = (goToPoint.x - me.position.x) * Global.properties.unitMaxHorizontalSpeed
        action.jump = jump
        action.jumpDown = jumpDown
        action.aim = aim
        action.shoot = shoot
        action.reload = false
        action.swapWeapon = false
        action.plantMine = false

        s.lastStepsUnits.add(game)
        if (s.lastStepsUnits.size == 11) {
            s.lastStepsUnits.removeAt(0)
        }

        return action
    }
}