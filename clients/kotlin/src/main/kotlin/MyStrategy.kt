import com.google.common.collect.EvictingQueue
import model.*
import java.io.OutputStream
import java.util.*

class MyStrategy {
    class Situation {
        public lateinit var me: model.Unit
        public lateinit var game: Game
        public lateinit var debug: Debug
        public val lastStepsUnits: Queue<Game> = EvictingQueue.create(10)

        constructor() {
            me = Unit()
            game = Game()
            debug = Debug(OutputStream.nullOutputStream())
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
            return game.units.findAmongBy({ it.playerId != me.playerId }, { -distanceToMe(it.position).toInt() })
        }

        public inline fun <reified T : model.Item> nearestItemType(): LootBox? {
            return game.lootBoxes.findAmongBy({ it.item is T }, { -distanceToMe(it.position).toInt() })
        }

        fun distanceToMe(smtn: Vec2Double): Double = distanceSqr(smtn, me.position)

        // todo remove after deikstra algo will be realized
        fun isStayOnPlaceLastMoves(n: Int): Boolean {
            return lastStepsUnits.take(n).map { it.units.find { it.id == me.id }!!.position }.toSet().size == 1
        }
    }

    private val s = Situation()
    fun getAction(me: model.Unit, game: Game, debug: Debug): UnitAction {
        s.update(me, game, debug)
        val targetToUnit: model.Unit? = s.nearestEnemy()
        val nearestWeapon: LootBox? = s.nearestItemType<Item.Weapon>()

        val goToPoint: Vec2Double = if (me.weapon == null && nearestWeapon != null) {
            nearestWeapon.position
        } else if (targetToUnit != null) {
            if (me.health < game.properties.unitMaxHealth * 0.2) {
                s.nearestItemType<Item.HealthPack>()?.position.let { me.position }
            } else {
                targetToUnit.position
            }
        } else {
            me.position
        }

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
        jump = jump || s.isStayOnPlaceLastMoves(5)

        val action = UnitAction()
        action.velocity = goToPoint.x - me.position.x
        action.jump = jump
        action.jumpDown = goToPoint.x < me.position.x
        action.aim = aim
        action.shoot = true
        action.reload = false
        action.swapWeapon = false
        action.plantMine = false

        s.lastStepsUnits.add(game)

        return action
    }

    companion object {
        fun distanceSqr(a: Vec2Double, b: Vec2Double): Double {
            return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
        }

        fun <T> Array<T>.findAmongBy(filter: (T) -> Boolean,
                                     compare: (T) -> Int): T? {
            return this.filter(filter).sortedBy(compare).firstOrNull()
        }
    }
}
