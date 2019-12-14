package strategies

import Debug
import Strategy
import extensions.GameDataExtension
import model.*

class FastJumpyQuickStartStrategy: Strategy() {
    private val s = GameDataExtension()

    override fun getAction(unit: model.Unit, game: Game, debug: Debug): UnitAction {
        var nearestEnemy: model.Unit? = null
        for (other in game.units) {
            if (other.playerId != unit.playerId) {
                if (nearestEnemy == null || distanceSqr(unit.position,
                                other.position) < distanceSqr(unit.position, nearestEnemy.position)) {
                    nearestEnemy = other
                }
            }
        }
        var nearestWeapon: LootBox? = null
        for (lootBox in game.lootBoxes) {
            if (lootBox.item is Item.Weapon) {
                if (nearestWeapon == null || distanceSqr(unit.position,
                                lootBox.position) < distanceSqr(unit.position, nearestWeapon.position)) {
                    nearestWeapon = lootBox
                }
            }
        }
        var targetPos: Vec2Double = unit.position
        if (unit.weapon == null && nearestWeapon != null) {
            targetPos = nearestWeapon.position
        } else if (nearestEnemy != null) {
            targetPos = nearestEnemy.position
        }
        debug.draw(CustomData.Log("Target pos: $targetPos"))
        var aim = Vec2Double(0.0, 0.0)
        if (nearestEnemy != null) {
            aim = Vec2Double(nearestEnemy.position.x - unit.position.x,
                    nearestEnemy.position.y - unit.position.y)
        }
        var jump = targetPos.y > unit.position.y;
        if (targetPos.x > unit.position.x && game.level.tiles[(unit.position.x + 1).toInt()][(unit.position.y).toInt()] == Tile.WALL) {
            jump = true
        }
        if (targetPos.x < unit.position.x && game.level.tiles[(unit.position.x - 1).toInt()][(unit.position.y).toInt()] == Tile.WALL) {
            jump = true
        }

        var jumpDown = targetPos.y < unit.position.y
        if (s.lastStepsUnits.isNotEmpty()) {
            val myLastPosition = s.lastStepsUnits.last().units.find { it.id == unit.id }!!.position
            val myLastXTileIndex = myLastPosition.x.toInt()
            val myLastYTileIndex = myLastPosition.y.toInt()
            if (game.level.tiles[myLastXTileIndex][myLastYTileIndex] == Tile.PLATFORM &&
                    unit.positionInt.x == myLastXTileIndex && unit.positionInt.y == myLastYTileIndex + 1) {
                jump = false
                jumpDown = false
            }
        }

        val action = UnitAction()
        action.velocity = (targetPos.x - unit.position.x) * Global.properties.unitMaxHorizontalSpeed
        action.jump = jump
        action.jumpDown = jumpDown
        action.aim = aim
        action.shoot = true
        action.reload = false
        action.swapWeapon = false
        action.plantMine = false
        return action
    }

    companion object {
        internal fun distanceSqr(a: Vec2Double, b: Vec2Double): Double {
            return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
        }
    }
}
