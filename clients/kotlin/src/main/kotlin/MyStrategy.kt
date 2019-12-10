import model.*

class MyStrategy {

    class Ext(val me: model.Unit, val game: Game, val debug: Debug) {
        fun <T> findBest(all: Array<T>,
                         filter: (T) -> Boolean,
                         compare: (T) -> Int): T? {
            return all.filter(filter).sortedBy(compare).firstOrNull()
        }

        public fun nearestEnemy(): model.Unit? {
            return findBest(game.units, { it.playerId != me.playerId }, { -distanceSqr(it.position, me.position).toInt() })
        }

        public inline fun <reified T : model.Item> nearestItemType(): LootBox? {
            return findBest(game.lootBoxes, { it.item is T }, { -distanceSqr(it.position, me.position).toInt() })
        }

        fun distanceToMe(smtn: model.Unit): Double = distanceSqr(smtn.position, me.position)
    }

    fun isBackToBack(unit: model.Unit, game: Game): Boolean {
        for (other in game.units) {
            if (other.playerId != unit.playerId) {
                if (distanceSqr(unit.position, other.position) < 20.0) {
                    return true
                }
            }
        }
        return false
    }

    fun getAction(me: model.Unit, game: Game, debug: Debug): UnitAction {
        val c = Ext(me, game, debug)
        val targetToUnit: model.Unit? = c.nearestEnemy()

        val goToPoint: Vec2Double = {
            val nearestWeapon: LootBox? = c.nearestItemType<Item.Weapon>()

            if (me.weapon == null && nearestWeapon != null) {
                nearestWeapon.position
            } else if (targetToUnit != null) {
                if (me.health < game.properties.unitMaxHealth * 0.2) {
                    c.nearestItemType<Item.HealthPack>()?.position.let { me.position }
                } else {
                    targetToUnit.position
                }
            } else {
                me.position
            }
        }()

        debug.draw(CustomData.Log("Target pos: $goToPoint"))
        var aim = Vec2Double(0.0, 0.0)
        if (targetToUnit != null) {
            aim = Vec2Double(
                    targetToUnit.position.x - me.position.x,
                    targetToUnit.position.y - me.position.y)
        }
        var jump = goToPoint.y > me.position.y;
        if (goToPoint.x > me.position.x &&
                game.level.tiles[(me.position.x + 1).toInt()][(me.position.y).toInt()] == Tile.WALL) {
            jump = true
        }
        if (goToPoint.x < me.position.x &&
                game.level.tiles[(me.position.x - 1).toInt()][(me.position.y).toInt()] == Tile.WALL) {
            jump = true
        }
        jump = jump || isBackToBack(me, game)
        val action = UnitAction()
        action.velocity = goToPoint.x - me.position.x
        action.jump = jump
        action.jumpDown = goToPoint.x < me.position.x
        action.aim = aim
        action.shoot = true
        action.reload = false
        action.swapWeapon = false
        action.plantMine = false
        return action
    }

    companion object {
        public fun distanceSqr(a: Vec2Double, b: Vec2Double): Double {
            return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
        }
    }
}
