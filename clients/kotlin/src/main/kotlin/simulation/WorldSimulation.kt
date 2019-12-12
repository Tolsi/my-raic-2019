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

object WorldSimulation {
    // 9e-1?
    val EPS = 9e-1

    fun simulate(startGame: Game, unitsStrategies: Map<model.Unit, Strategy>): Game {
        // todo до или после movedBullets?
        val gameAfterPlayersCollision = playersCollisionSteps(startGame)

        val movedBullets = gameAfterPlayersCollision.bullets.map { it.nextPosition() }

        val gameAfterBulletsCollision = bulletsCollisionSteps(gameAfterPlayersCollision, movedBullets)

        val gameAfterPlayersSteps = gameAfterBulletsCollision.units.fold(gameAfterBulletsCollision) { game, unit ->
            userStep(game, unit to unitsStrategies[unit]!!.getAction(unit, game, Debug.Mock))
        }

        val finalGame = wallsCollisionSteps(gameAfterPlayersSteps)
        finalGame.currentTick += 1
        return finalGame
    }

    fun userStep(game: Game, step: Pair<model.Unit, UnitAction>): Game {
        // create bullet
        val resultGame = game.copyOf()
        val (unit, action) = step
        val s = GameDataExtension(unit, game, Debug.Mock)
        if (action.shoot && unit.weapon != null && unit.weapon?.fireTimer ?: 0.0 == 0.0) {
            val newBullet = Bullet(unit.weapon!!.typ, unit.id, unit.playerId, unit.position, Bullet.velocity(action.aim.toPoint(), unit.weapon!!.params.bullet.speed).toVec2Double(), unit.weapon!!.params.bullet.damage, unit.weapon!!.params.bullet.size, unit.weapon!!.params.explosion)
            resultGame.bullets.plus(newBullet)
        }
        // todo move unit
        val updatedUnit = unit.copyOf()
        updatedUnit.position.x = unit.position.x + action.velocity
        // todo collide jump pad?
        // todo take bonuses and weapon if allowed
        if (action.jump) {
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
            } else {
                // todo jump after JUMP_PAD tile
                if (updatedUnit.jumpState.maxTime > 0) {
                    updatedUnit.jumpState.canCancel = true
                    updatedUnit.jumpState.canJump = false
                    updatedUnit.jumpState.speed = 0.0
                    updatedUnit.jumpState.maxTime -= (Global.properties.unitJumpTime / 60)
                }
                // todo падение?
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
        return resultGame
    }

    fun playersCollisionSteps(game: Game): Game {
        // todo упереться и упасть на другого игрока
        return game
    }

    fun wallsCollisionSteps(game: Game): Game {
        // todo platforms and walls collision
        throw NotImplementedError("TODO")
    }

    fun bulletsCollisionSteps(game: Game, movedBullets: Collection<Bullet>): Game {
        // todo activate mines
        // todo damage
        // todo die
        // todo count scores?
        throw NotImplementedError("TODO")
    }
}