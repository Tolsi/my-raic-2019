package simulation

import extensions.*
import korma_geom.Point
import korma_geom.points
import model.*

class WorldSimulation(startGame: Game) {
    companion object {
        val EPS = 1e-9
    }

    //    fun predictStrategies(): Game {
//        val gameAfterPlayersSteps = gameAfterBulletsCollision.units.fold(gameAfterBulletsCollision) { game, unit ->
//            userStep(game, unit to unitsStrategies[unit.id]!!.getAction(unit, game, Debug.Mock))
//        }
//
//    }
    private var lastGame: Game = startGame
    private var userBulletOnNextTick: MutableMap<Int, Bullet> = mutableMapOf()
    private var userFallOnNextTick: MutableMap<Int, Boolean> = mutableMapOf()
    private var userLastActionAndState: MutableMap<Int, Pair<model.Unit, UnitAction>> = mutableMapOf()

    fun tick(unitsSteps: Map<Int, UnitAction>): Game {
        // todo до или после movedBullets?
        val gameAfterPlayersCollision = playersCollisionSteps(lastGame)

        val movedBullets = gameAfterPlayersCollision.bullets.map { it.nextPosition() }.toTypedArray()

        val gameAfterBulletsCollision = bulletsCollisionSteps(gameAfterPlayersCollision, movedBullets)

        val gameAfterPlayersSteps = gameAfterBulletsCollision.units.fold(gameAfterBulletsCollision) { game, unit ->
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
        val s = GameDataExtension(unit, game, Debug.Mock)

        userBulletOnNextTick.get(unit.id)?.let {
            resultGame.bullets = resultGame.bullets.plus(it.nextPosition())
            userBulletOnNextTick.remove(unit.id)
        }
        // todo eps or other value?
        val updatedUnit = unit.copyOf()

        if (action.shoot && unit.weapon != null && (unit.weapon?.fireTimer
                        ?: 0.0) - unit.weapon!!.params.fireRatePerTick < 0.0) {
            val newBullet = Bullet(unit.weapon!!.typ, unit.id, unit.playerId, unit.position, Bullet.velocity(action.aim.toPoint(), unit.weapon!!.params.bullet.speed).toVec2Double(), unit.weapon!!.params.bullet.damage, unit.weapon!!.params.bullet.size, unit.weapon!!.params.explosion)
            userBulletOnNextTick.put(unit.id, newBullet)
        } else if (unit.weapon != null && unit.weapon!!.fireTimer ?: 0.0 > 0.0) {
            updatedUnit.weapon!!.fireTimer = updatedUnit.weapon!!.fireTimer!! - unit.weapon!!.params.fireRatePerTick
        }

        updatedUnit.position.x = unit.position.x + (action.velocity.coerceIn(-Global.properties.unitMaxHorizontalSpeedPerTick, Global.properties.unitMaxHorizontalSpeedPerTick))
        // todo collide jump pad?
        // todo take bonuses and weapon if allowed
        val isIFall = unit.isFalling() || userFallOnNextTick.getOrDefault(unit.id, false)
        if (action.jump && updatedUnit.jumpState.canJump && !isIFall) {
            if (unit.onLadder) {
                updatedUnit.position.y += Global.properties.unitJumpSpeedPerTick
//                updatedUnit.jumpState.canCancel = true
//                updatedUnit.jumpState.canJump = true
//                updatedUnit.jumpState.speed = Global.properties.unitJumpSpeed
//                updatedUnit.jumpState.maxTime = Global.properties.unitJumpTime
                // todo написать onGround и onLadder
                updatedUnit.onGround = true
            } else if (unit.onGround) {
//                updatedUnit.jumpState.canCancel = true
//                updatedUnit.jumpState.canJump = true
//                updatedUnit.jumpState.speed = Global.properties.unitJumpSpeed
                updatedUnit.jumpState.maxTime = Global.properties.unitJumpTime
                // todo кто-то стоит на мне или потолок
                // todo или тут неправильно
//                updatedUnit.onGround = s.notMe().any { unit.isStaysOnMe(it) } && unit.y.rem(1) < 0.166666667
//                if (!s.notMe().any { unit.isStaysOnMe(it) }) {
                updatedUnit.jumpState.maxTime -= Global.properties.unitJumpTimePerTick
                updatedUnit.position.y += Global.properties.unitJumpSpeedPerTick
//                }
            } else {
                // todo jump after JUMP_PAD tile
                if (updatedUnit.jumpState.maxTime - Global.properties.unitJumpTimePerTick > 0) {
                    updatedUnit.jumpState.canCancel = true
                    updatedUnit.jumpState.canJump = true
                    updatedUnit.jumpState.speed = Global.properties.unitJumpSpeed
                    updatedUnit.position.y += Global.properties.unitJumpSpeedPerTick
                    updatedUnit.jumpState.maxTime -= Global.properties.unitJumpTimePerTick
                } else {
                    updatedUnit.jumpState.maxTime = 0.0
                    updatedUnit.jumpState.speed = Global.properties.unitFallSpeed
                    updatedUnit.jumpState.canJump = false
                    updatedUnit.jumpState.canCancel = false
                    updatedUnit.position.y -= Global.properties.unitFallSpeedPerTick
                }
            }
        } else {
            if (unit.onLadder) {
                updatedUnit.jumpState = JumpState.Simple
                updatedUnit.onGround = true
                updatedUnit.position.y -= Global.properties.unitFallSpeedPerTick
            } else if (isIFall) {
                updatedUnit.position.y -= Global.properties.unitFallSpeedPerTick
                updatedUnit.jumpState = JumpState.Falling
            } else if (!unit.onGround) {
                userFallOnNextTick.put(unit.id, true)
                updatedUnit.position.y += Global.properties.unitJumpSpeedPerTick
                updatedUnit.jumpState.canCancel = true
                updatedUnit.jumpState.canJump = true
                updatedUnit.jumpState.speed = Global.properties.unitJumpSpeed
                updatedUnit.jumpState.maxTime -= Global.properties.unitJumpTimePerTick
            }
        }
        updatedUnit.calculateFields()

        var tile = updatedUnit.position.toPoint().onTile()
        // todo jumpad speed?
        if (updatedUnit.bottomCenter().onTile() == Tile.WALL ||
                updatedUnit.bottomCenter().onTile() == Tile.PLATFORM && !action.jumpDown ||
                updatedUnit.topCenter().onTile() == Tile.WALL) {
            updatedUnit.position.y = Math.round(updatedUnit.position.y).toDouble()
        }
        if (updatedUnit.leftCenter().onTile() == Tile.WALL ||
                updatedUnit.rightCenter().onTile() == Tile.WALL) {
            updatedUnit.position.x = Math.round(updatedUnit.position.x).toDouble()
        }

        // todo Считается, что юнит находится на лестнице, если отрезок от центра юнита до середины нижней границе юнита пересекается с тайлом.
        when (tile) {
            // todo use it?
            Tile.EMPTY -> {
                updatedUnit.onGround = updatedUnit.y.rem(1) < 0.15 &&
                        (updatedUnit.underMeTile() == Tile.PLATFORM || updatedUnit.underMeTile() == Tile.WALL)
                updatedUnit.onLadder = false
            }
            Tile.LADDER -> {
                updatedUnit.onGround = true
                updatedUnit.onLadder = true
                updatedUnit.jumpState = JumpState.Simple
            }
            Tile.PLATFORM, Tile.WALL -> {
                updatedUnit.onGround = true
                updatedUnit.onLadder = false
                updatedUnit.jumpState = JumpState.Simple
            }
            Tile.JUMP_PAD -> {
                updatedUnit.onGround = false
                updatedUnit.onLadder = false
                updatedUnit.jumpState = JumpState.JumpPad
                // todo ?!
            }
        }

        // todo on the ground or ladder remove userFallOnNextTick
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
//        throw NotImplementedError("TODO")
        game.bullets = movedBullets
        return game
    }
}