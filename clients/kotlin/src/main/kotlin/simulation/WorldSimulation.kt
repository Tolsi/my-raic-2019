package simulation

import Strategy
import extensions.GameDataExtension
import extensions.isStaysOnMe
import extensions.toPoint
import extensions.toVec2Double
import model.Bullet
import model.Game
import model.UnitAction
import model.Vec2Double

class WorldSimulation(startGame: Game) {
    companion object {
        // 9e-1?
        val EPS = 9e-1
    }
//    fun predictStrategies(): Game {
//        val gameAfterPlayersSteps = gameAfterBulletsCollision.units.fold(gameAfterBulletsCollision) { game, unit ->
//            userStep(game, unit to unitsStrategies[unit.id]!!.getAction(unit, game, Debug.Mock))
//        }
//
//    }
    var lastGame: Game = startGame
    fun tick(unitsSteps: Map<Int, UnitAction>): Game {
        // todo до или после movedBullets?
        val gameAfterPlayersCollision = playersCollisionSteps(lastGame)

        val movedBullets = gameAfterPlayersCollision.bullets.map { it.nextPosition() }

        val gameAfterBulletsCollision = bulletsCollisionSteps(gameAfterPlayersCollision, movedBullets)

        val gameAfterPlayersSteps = gameAfterBulletsCollision.units.fold(gameAfterBulletsCollision) {
            game, unit -> userStep(game, unit to unitsSteps[unit.id]!! )
        }

        val finalGame = wallsCollisionSteps(gameAfterPlayersSteps)
        finalGame.currentTick += 1
        lastGame = finalGame
        return finalGame
    }

    private var userBulletOnNextTick: MutableMap<Int, Bullet> = mutableMapOf()

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
        if (action.shoot && unit.weapon != null && unit.weapon?.fireTimer ?: 0.0 < EPS) {
            val newBullet = Bullet(unit.weapon!!.typ, unit.id, unit.playerId, unit.position, Bullet.velocity(action.aim.toPoint(), unit.weapon!!.params.bullet.speed).toVec2Double(), unit.weapon!!.params.bullet.damage, unit.weapon!!.params.bullet.size, unit.weapon!!.params.explosion)
            userBulletOnNextTick.put(unit.id, newBullet)
        }
        // todo move unit
        val updatedUnit = unit.copyOf()
        updatedUnit.position.x = unit.position.x + action.velocity.coerceIn(-Global.properties.unitMaxHorizontalSpeedPerTick, Global.properties.unitMaxHorizontalSpeedPerTick)
        // todo collide jump pad?
        // todo take bonuses and weapon if allowed
        if (action.jump) {
            if (unit.onLadder) {
                updatedUnit.position.y += Global.properties.unitJumpSpeedPerTick
                updatedUnit.jumpState.canCancel = true
                updatedUnit.jumpState.canJump = true
                updatedUnit.jumpState.speed = Global.properties.unitJumpSpeed
                updatedUnit.jumpState.maxTime = Global.properties.unitJumpTime
                updatedUnit.onGround = true
            } else if (unit.onGround) {
                updatedUnit.position.y += Global.properties.unitJumpSpeedPerTick
                updatedUnit.jumpState.canCancel = true
                updatedUnit.jumpState.canJump = false
                updatedUnit.jumpState.speed = Global.properties.unitJumpSpeed
                updatedUnit.jumpState.maxTime = Global.properties.unitJumpTime
                // todo кто-то стоит на мне или потолок
                updatedUnit.onGround = s.notMe().any { unit.isStaysOnMe(it) }
            } else {
                // todo jump after JUMP_PAD tile
                if (updatedUnit.jumpState.maxTime > EPS) {
                    updatedUnit.jumpState.canCancel = true
                    updatedUnit.jumpState.canJump = false
                    updatedUnit.jumpState.speed = 0.0
                    updatedUnit.position.y -= Global.properties.unitFallSpeedPerTick
                    updatedUnit.jumpState.maxTime -= Global.properties.unitJumpTimePerTick
                } else {
                    updatedUnit.jumpState.maxTime = 0.0
                    updatedUnit.position.y -= Global.properties.unitFallSpeedPerTick
                }
            }
        } else {
            if (unit.onLadder) {
                updatedUnit.jumpState.canCancel = true
                updatedUnit.jumpState.canJump = true
                updatedUnit.jumpState.speed = Global.properties.unitJumpSpeed
                updatedUnit.jumpState.maxTime = Global.properties.unitJumpTime
                updatedUnit.onGround = true
            } else if (unit.onGround) {
                updatedUnit.jumpState.canCancel = true
                updatedUnit.jumpState.canJump = false
                updatedUnit.jumpState.speed = Global.properties.unitJumpSpeed
                updatedUnit.jumpState.maxTime = Global.properties.unitJumpTime
                // кто-то стоит на мне
                updatedUnit.onGround = s.notMe().any { unit.isStaysOnMe(it) }
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

    fun bulletsCollisionSteps(game: Game, movedBullets: Collection<Bullet>): Game {
        // todo activate mines
        // todo damage
        // todo die
        // todo count scores?
//        throw NotImplementedError("TODO")
        return game
    }
}