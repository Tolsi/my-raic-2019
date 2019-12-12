package strategies

import Debug
import Global
import Strategy
import extensions.GameDataExtension
import extensions.toPoint
import extensions.toVec2Double
import extensions.toVec2Float
import korma_geom.distanceTo
import korma_geom.farPoint
import korma_geom.points
import model.*

open class MySituativeStrategy : Strategy() {
    private val s = GameDataExtension()

    override fun getAction(me: model.Unit, game: Game, debug: Debug): UnitAction {
        Global.init(game)

        s.update(me, game, debug)

        val targetToUnit: model.Unit? = s.nearestEnemy()
        val nearestWeapon: LootBox? = s.nearestItemType<Item.Weapon>()
        val nearestHealthPack = s.nearestItemType<Item.HealthPack>()

        val goToPoint: Vec2Double = if (me.health < game.properties.unitMaxHealth * 0.45) {
            nearestHealthPack?.position ?: s.myStartPosition().toVec2Double()
        } else if (me.weapon == null && nearestWeapon != null) {
            nearestWeapon.points.farPoint(me.position.toPoint())!!.toVec2Double()
        } else if (nearestHealthPack != null && targetToUnit != null) {
            val healthPackCloserToMeThanEnemy =
                    nearestHealthPack.position.distanceTo(me.position) < targetToUnit.position.distanceTo(me.position)
            val endOfGameAndILose =
                    game.currentTick > Global.properties.maxTickCount * 0.7 &&
                            s.myPlayer.score > s.enemiesPlayers.maxBy { it.score }!!.score
            if (healthPackCloserToMeThanEnemy || endOfGameAndILose) {
                targetToUnit.topCenterPosition
            } else {
                nearestHealthPack.position
            }
        } else if (targetToUnit != null) {
            targetToUnit.topCenterPosition
        } else {
            s.myStartPosition().toVec2Double()
        }
        debug.draw(CustomData.Line(me.position.toVec2Float(), goToPoint.toVec2Float(), 0.2f, ColorFloat.Green))

//        debug level
//        for (r in Global.levelAsRectangles) {
//            debug.draw(CustomData.Rect(r.position.toVec2Float(), Vec2Float(r.size.width.toFloat(), r.size.height.toFloat()), ColorFloat.Blue))
//        }

        // todo если противник рядом, ставить мину и убегать

        debug.draw(CustomData.Log("Target pos: $goToPoint"))
        var aim = Vec2Double(0.0, 0.0)
        var shoot = false
        if (targetToUnit != null) {
            debug.draw(CustomData.Line(me.centerPosition.toVec2Float(), targetToUnit.centerPosition.toVec2Float(), 0.1f, ColorFloat.Red))
            aim = Vec2Double(
                    targetToUnit.centerPosition.x - me.centerPosition.x,
                    targetToUnit.centerPosition.y - me.centerPosition.y)
            shoot = s.isCanShoot() && !s.isCanHitMyself(targetToUnit.centerPosition.toPoint())
        }
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

        val action = UnitAction()
        action.velocity = (goToPoint.x - me.position.x) * Global.properties.unitMaxHorizontalSpeed
        action.jump = jump
        action.jumpDown = goToPoint.y < me.position.y
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