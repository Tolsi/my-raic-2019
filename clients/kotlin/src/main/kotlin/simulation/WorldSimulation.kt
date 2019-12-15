package simulation

import extensions.*
import korma_geom.*
import model.*

class WorldSimulation {
    companion object {
        val EPS = 1e-9
    }

    constructor() {}

    constructor(startGame: Game) {
        this.lastGame = startGame
    }

    //    fun predictStrategies(): Game {
//        val gameAfterPlayersSteps = gameAfterBulletsCollision.units.fold(gameAfterBulletsCollision) { game, unit ->
//            userStep(game, unit to unitsStrategies[unit.id]!!.getAction(unit, game, Debug.Mock))
//        }
//
//    }
    lateinit var lastGame: Game
    private var userLastActionAndState: MutableMap<Int, Pair<model.Unit, UnitAction>> = mutableMapOf()

    fun tick(unitsSteps: Map<Int, UnitAction>): Game {
        // todo до или после movedBullets?
        val gameAfterPlayersCollision = playersCollisionSteps(lastGame)

        val movedBullets = gameAfterPlayersCollision.bullets.map { it.nextPosition() }.toTypedArray()

        val gameAfterBulletsCollision = bulletsCollisionSteps(gameAfterPlayersCollision, movedBullets)

        val gameAfterPlayersSteps = gameAfterBulletsCollision.units.filter { unitsSteps.containsKey(it.id) }
                .fold(gameAfterBulletsCollision) { game, unit ->
                    userStep(game, unit to unitsSteps[unit.id]!!)
                }

        val finalGame = wallsCollisionSteps(gameAfterPlayersSteps)
        finalGame.currentTick += 1
        lastGame = finalGame
        unitsSteps.forEach { (id, action) -> userLastActionAndState.put(id, finalGame.unitById(id) to action) }
        return finalGame
    }

    fun userStep(game: Game, step: Pair<model.Unit, UnitAction>): Game {
        // create bullet
        // todo on the next tick!
        val resultGame = game.copyOf()
        val (unit, action) = step
        action.calculateFields()
        val s = GameDataExtension(unit, game, Debug.Mock)

//        userBulletOnNextTick.get(unit.id)?.let {
//            resultGame.bullets = resultGame.bullets.plus(it.nextPosition())
//            userBulletOnNextTick.remove(unit.id)
//        }
        val updatedUnit = unit.copyOf()

        if (unit.weapon != null && unit.weapon!!.fireTimer ?: 0.0 > 0.0) {
            updatedUnit.weapon!!.fireTimer = updatedUnit.weapon!!.fireTimer!! - unit.weapon!!.params.fireRatePerTick
            if (updatedUnit.weapon!!.fireTimer ?: 0.0 < 0.0) {
                updatedUnit.weapon!!.fireTimer = null
            }
        }
        if (action.shoot && unit.weapon != null && unit.weapon?.fireTimer == null && unit.weapon!!.magazine > 0) {
            val newBullet = Bullet(unit.weapon!!.typ, unit.id, unit.playerId, unit.position, Bullet.velocity(action.aim.toPoint(), unit.weapon!!.params.bullet.speed).toVec2Double(), unit.weapon!!.params.bullet.damage, unit.weapon!!.params.bullet.size, unit.weapon!!.params.explosion)
            unit.weapon!!.lastFireTick = game.currentTick + 1
            unit.weapon!!.magazine -= 1
            unit.weapon!!.fireTimer = unit.weapon!!.params.fireRate
            resultGame.bullets = resultGame.bullets.plus(newBullet)
//            userBulletOnNextTick.put(unit.id, newBullet)
        }

        if (unit.weapon != null && (action.reload || unit.weapon!!.magazine == 0)) {
            updatedUnit.weapon!!.fireTimer = unit.weapon!!.params.reloadTime
            unit.weapon!!.magazine = unit.weapon!!.params.magazineSize
        }

        val reducedVelocity = action.velocityPerTick.coerceIn(-Global.properties.unitMaxHorizontalSpeedPerTick, Global.properties.unitMaxHorizontalSpeedPerTick)
        updatedUnit.position.x = unit.position.x + reducedVelocity
        // todo collide jump pad?
        val isIFall = unit.isFalling()
        var alreadyJump = false
        val jump = action.jump && updatedUnit.jumpState.canJump
        if (jump) {
            if (unit.onLadder) {
                alreadyJump = true
                updatedUnit.position.y += Global.properties.unitJumpSpeedPerTick
            } else if (unit.onGround) {
                updatedUnit.jumpState.maxTime = Global.properties.unitJumpTime - Global.properties.unitJumpTimePerTick
                updatedUnit.position.y += Global.properties.unitJumpSpeedPerTick
                alreadyJump = true
            } else {
                if (updatedUnit.jumpState.maxTime - Global.properties.unitJumpTimePerTick > 0) {
                    alreadyJump = true
                    updatedUnit.jumpState.maxTime -= Global.properties.unitJumpTimePerTick
                    updatedUnit.position.y += Global.properties.unitJumpSpeedPerTick
                } else {
                    updatedUnit.jumpState = JumpState.Falling
                    updatedUnit.position.y -= Global.properties.unitFallSpeedPerTick
                }
            }
        } else {
            if (unit.onLadder && !isIFall) {
                if (unit.bottomSide().any { it.onTile() != Tile.WALL }) {
                    updatedUnit.position.y += Global.properties.unitFallSpeedPerTick
                }
                updatedUnit.jumpState = JumpState.Simple.copyOf()
            } else if (isIFall) {
                updatedUnit.jumpState = if (unit.onLadder) JumpState.Simple.copyOf() else JumpState.Falling
                updatedUnit.position.y -= Global.properties.unitFallSpeedPerTick
            }
        }

        updatedUnit.calculateFields()

        // collide with unit
        val collideLeft = s.notMe().find { enemy -> updatedUnit.leftSide().any { enemy.asRectangle.contains(it) } }
        if (collideLeft != null) {
            updatedUnit.position.x = collideLeft.right + updatedUnit.size.x / 2 + EPS
        }
        val collideRight = s.notMe().find { enemy -> updatedUnit.rightSide().any { enemy.asRectangle.contains(it) } }
        if (collideRight != null) {
            updatedUnit.position.x = collideRight.left - updatedUnit.size.x / 2 - EPS
        }
        val collideUp = s.notMe().find { enemy -> updatedUnit.topSide().any { enemy.asRectangle.contains(it) } }
        if (collideUp != null) {
            updatedUnit.position.y = collideUp.bottom - updatedUnit.size.y - EPS
            updatedUnit.jumpState = JumpState.Falling.copyOf()
        }
        val collideDown = s.notMe().find { enemy -> updatedUnit.bottomSide().any { enemy.asRectangle.contains(it) } }
        if (collideDown != null) {
            updatedUnit.position.y = collideDown.top + EPS
        }

        updatedUnit.calculateFields()

        if (updatedUnit.leftSide().any { it.onTile() == Tile.WALL } ||
                updatedUnit.rightSide().any { it.onTile() == Tile.WALL }) {
            updatedUnit.position.x = Math.round(updatedUnit.position.x).toDouble() + updatedUnit.size.x / 2 + EPS
        }
        if (updatedUnit.bottomSide().any { it.onTile() == Tile.WALL } ||
                updatedUnit.bottomSide().any { it.onTile() == Tile.PLATFORM } && !action.jumpDown ||
                updatedUnit.topSide().any { it.onTile() == Tile.WALL }) {
            updatedUnit.position.y = Math.round(updatedUnit.position.y).toDouble() + EPS
        }

        updatedUnit.calculateFields()

        // todo fix onGround!!!1
        val underTile = updatedUnit.underMeTile()
        if (updatedUnit.bottomSide().any { it.y.rem(1) <= 1f / 6 }) {
            when (underTile) {
                Tile.PLATFORM, Tile.WALL -> {
                    updatedUnit.onLadder = false
                    updatedUnit.jumpState = JumpState.Simple.copyOf()
                }
                Tile.JUMP_PAD -> {
                    // todo jump after JUMP_PAD tile
                    updatedUnit.onLadder = false
                    updatedUnit.jumpState = JumpState.JumpPad.copyOf()
                }
            }
        }

        if (updatedUnit.bottomSide().any { it.onTile() == Tile.WALL }) {
            updatedUnit.onLadder = false
        } else if (underTile == Tile.LADDER && updatedUnit.isOnLadder()) {
            updatedUnit.onLadder = true
            updatedUnit.jumpState = JumpState.Simple.copyOf()
        } else {
            updatedUnit.onLadder = false
        }

        updatedUnit.onGround = underTile != Tile.EMPTY &&
                updatedUnit.y.rem(1) <= 1f / 6 &&
                (updatedUnit.bottomSide().any { p -> Global.wallsAsRectangles.any { it.contains(p) } })

        if (!updatedUnit.isFalling() && !updatedUnit.onGround && !jump) {
            updatedUnit.jumpState = JumpState.Falling
            updatedUnit.position.y -= Global.properties.unitFallSpeedPerTick
        }

        if (action.jump && !alreadyJump && updatedUnit.jumpState.canJump) {
            alreadyJump = true
            updatedUnit.jumpState = JumpState.Simple.copyOf()
            updatedUnit.position.y += Global.properties.unitJumpSpeedPerTick
            updatedUnit.jumpState.maxTime -= Global.properties.unitJumpTimePerTick
        }

        if (updatedUnit.onGround && !alreadyJump != updatedUnit.onGround) {
            updatedUnit.onGround = updatedUnit.onGround && !alreadyJump
        } else if (!updatedUnit.onGround && !jump) updatedUnit.jumpState = JumpState.Falling

        game.lootBoxes.filter {
            it.asRectangle.intersects(updatedUnit.asRectangle)
        }.forEach { collidedLootBox ->
            val wasUsed = when (collidedLootBox.item) {
                is Item.HealthPack -> {
                    val isNotFull = updatedUnit.health < Global.properties.unitMaxHealth
                    if (updatedUnit.health < Global.properties.unitMaxHealth) {
                        val healthPack = collidedLootBox.item as Item.HealthPack
                        updatedUnit.health += healthPack.health
                    }
                    isNotFull
                }
                is Item.Weapon -> {
                    val swapOrEmpty = action.swapWeapon || unit.weapon == null
                    if (swapOrEmpty) {
                        val weapon = collidedLootBox.item as Item.Weapon
                        val weaponParams = Global.properties.weaponParams[weapon.weaponType]!!
                        // todo spread?
                        // todo angle?
                        updatedUnit.weapon = Weapon(weapon.weaponType, weaponParams, weaponParams.magazineSize, false, 0.0, weaponParams.reloadTime, null, null)
                    }
                    swapOrEmpty
                }
                is Item.Mine -> {
                    updatedUnit.mines += 1
                    true
                }
                else -> false
            }
            if (wasUsed) {
                resultGame.lootBoxes = resultGame.lootBoxes.filter { !collidedLootBox.equals(it) }.toTypedArray()
            }
        }

        val unitId = resultGame.units.indexOfFirst { it.id == unit.id }
        resultGame.units[unitId] = updatedUnit
        return resultGame
    }

    fun playersCollisionSteps(game: Game): Game {
        // todo упереться и упасть на другого игрока
        return game
    }

    fun wallsCollisionSteps(game: Game): Game {
        // todo platforms and walls collision
//        throw NotImplementedError("TODO")
        return game
    }

    fun bulletsCollisionSteps(game: Game, movedBullets: Array<Bullet>): Game {
        // todo activate mines
        // todo damage
        // todo die
        // todo count scores?
        // урон-аптечка-проверка смерти
        game.bullets = movedBullets.filter { bullet ->
            bullet.x >= 0 && bullet.x <= Global.level.tiles.size &&
                    bullet.y >= 0 && bullet.y <= Global.level.tiles[0].size &&
                    !Global.wallsAsRectangles.plus(game.units.filter { it.id != bullet.unitId }.map { it.asRectangle }).any { it.asRectangle.intersects(bullet.asRectangle) }
        }.toTypedArray()
        // todo collides!
//        movedBullets.map { bullet ->
//            Global.wallsAsRectangles.plus(game.units.filter { it.id != bullet.unitId }.map { it.asRectangle }).find { it.asRectangle.intersects(bullet.asRectangle) }
//        }.toTypedArray()
        return game
    }
}