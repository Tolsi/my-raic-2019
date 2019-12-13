package strategies

import Debug
import Global
import Strategy
import extensions.*
import korma_geom.distanceTo
import korma_geom.farPoint
import korma_geom.points
import model.*
import model.Unit

open class MySituativeStrategy : Strategy() {
    private val s = GameDataExtension()

    override fun getAction(me: model.Unit, game: Game, debug: Debug): UnitAction {
        Global.init(game)

        s.update(me, game, debug)

        if (game.units.size != 2) {
            return UnitAction.Empty
        }

        val targetToUnit: Unit = s.nearestEnemy() ?: return UnitAction()
        // enemy should be be always
        val nearestWeapon: LootBox? = s.nearestItemType<Item.Weapon>()
        val nearestHealthPack = s.nearestItemType<Item.HealthPack>()

        val goToPoint: Vec2Double = if (me.health < game.properties.unitMaxHealth *
                if (targetToUnit.weapon?.typ == WeaponType.ROCKET_LAUNCHER) 0.8 else 0.45) {
            nearestHealthPack?.position ?: s.myStartPosition().toVec2Double()
        } else if (me.weapon == null && nearestWeapon != null) {
            nearestWeapon.points.farPoint(me.position.toPoint())!!.toVec2Double()
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

        var aim = Vec2Double(0.0, 0.0)
        var shoot = false
        debug.draw(CustomData.Line(me.centerPosition.toVec2Float(), targetToUnit.centerPosition.toVec2Float(), 0.1f, ColorFloat.Red))
        aim = Vec2Double(
                targetToUnit.centerPosition.x - me.centerPosition.x,
                targetToUnit.centerPosition.y - me.centerPosition.y)
        shoot = s.isCanShoot() && !s.isCanHitMyselfOrWithEnemies(targetToUnit.centerPosition.toPoint())

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
//        for (r in Global.wallsAsRectangles) {
//            debug.draw(CustomData.Rect(r.position.toVec2Float(), Vec2Float(r.size.width.toFloat(), r.size.height.toFloat()), ColorFloat.Blue))
//        }
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