import korma_geom.*
import model.*
import java.io.OutputStream

object Global {
    var properties: model.Properties = model.Properties()
    var level: model.Level = model.Level()
    var levelAsRectangles: Collection<Rectangle> = emptyList()

    fun init(game: Game) {
        if (!Global.init) {
            level = game.level
            properties = game.properties
            levelAsRectangles = level.toRectangles(game)
            init = true
        }
    }

    public var init: Boolean = false
}

class MyStrategy {
    class Situation {
        public lateinit var me: model.Unit
        public lateinit var game: Game
        public lateinit var debug: Debug
        public val lastStepsUnits: MutableList<Game> = mutableListOf()

        constructor() {
            me = Unit()
            game = Game()
            debug = Debug(object : OutputStream() {override fun write(p0: Int) {}})
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

        fun distanceToMe(smtn: Vec2Double): Double = distanceSqr(smtn, me.position)

        // todo remove after deikstra algo will be realized
        fun isStayOnPlaceLastMoves(n: Int): Boolean {
            return lastStepsUnits.reversed().take(n).map { it.units.find { it.id == me.id }!!.position }.toSet().size == 1
        }

        // todo что будет если я выстрелю сейчас? попаду ли я в себя?
        // todo предсказывать движения себя и противка и пуль и смотреть куда стрелять
        // todo стрелять на опережение
        fun isCanHitMyself(target: Point): Boolean {
            val weaponParams = me.weapon!!.params
            val collisionPoint = Line.createFromPointAimAndSpeed(me.centerPosition.toPoint(), target, weaponParams.bullet.speed).find { p ->
                Global.levelAsRectangles.plus(enemies().map { it.toRectangle() }).any { r -> r.intersects(p.toRectangle(weaponParams.bullet.size)) }
            } ?: return false
            if (weaponParams.explosion?.radius ?: 0.0 > 0) {
                val explosionRadiusRectangle = collisionPoint.toRectangle(weaponParams.explosion!!.radius)
                return explosionRadiusRectangle.intersects(me.toRectangle()) &&
                        !notMe().any { explosionRadiusRectangle.intersects(it.toRectangle()) }
            }
            return false
        }

        fun isCanShoot(): Boolean {
            return me.weapon != null && me.weapon!!.fireTimer ?: 0.0 == 0.0
        }
    }

    private val s = Situation()
    fun getAction(me: model.Unit, game: Game, debug: Debug): UnitAction {
        Global.init(game)

        s.update(me, game, debug)

        val targetToUnit: model.Unit? = s.nearestEnemy()
        val nearestWeapon: LootBox? = s.nearestItemType<Item.Weapon>()

        val goToPoint: Vec2Double = if (me.health < game.properties.unitMaxHealth * 0.45) {
            s.nearestItemType<Item.HealthPack>()?.position.let { me.position }
        } else if (me.weapon == null && nearestWeapon != null) {
            nearestWeapon.position
        } else if (targetToUnit != null) {
             targetToUnit.topCenterPosition
        } else {
            me.position
        }
        debug.draw(CustomData.Line(me.position.toVec2Float(), goToPoint.toVec2Float(), 0.2f, ColorFloat.Green))

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

    companion object {
        fun distanceSqr(a: Vec2Double, b: Vec2Double): Double {
            return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
        }

//        public infix fun Vec2Double.to(that: Vec2Double): Vector = Pair(this, that)
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

fun model.Level.toRectangles(game: Game): Collection<Rectangle> {
    val tiles = this.tiles.mapIndexed { x, line ->
        line.mapIndexed { y, tile ->
            if (tile == model.Tile.WALL) {
                Rectangle(x, y, 2, 2)
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

fun Point.toRectangle(size: Double): Rectangle {
    return Rectangle(this.x - size, this.y - size, size * 2, size * 2)
}

fun model.Unit.toRectangle(): Rectangle {
    return Rectangle(this.position.x - this.size.x / 2, this.position.y - this.size.y / 2, this.size.x, this.size.y)
}