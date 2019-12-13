package simulation

import extensions.*
import korma_geom.asRectangle
import model.UnitAction
import kotlin.test.*
import org.junit.Test as test

class WorldSimulationTest() {
    val assertEPS = 1f/6
    fun equalsInEPS(value: Double, correct: Double): Boolean {
        val res = Math.abs(value - correct) <= assertEPS
        return res
    }

    @test
    fun simulateGame() {
        val unitId = 4
        val games = SimulationWorlds.games
        val actions = SimulationTestActions.actions
        Global.init(games.first())
        val simulation = WorldSimulation(games.first())
        actions.zip(games.drop(1)).fold(games.first()) { currentGame, (action, correctNextGame) ->
            val currentUnit = currentGame.unitById(unitId)
            val correctUnit = correctNextGame.unitById(unitId)
            val newGame = simulation.tick(mapOf(3 to UnitAction.Empty, 4 to action))
//                for (correctNextGame.units)
            assertEquals(correctNextGame.currentTick, newGame.currentTick)
            val calculatedUnit = newGame.unitById(unitId)

            assertTrue(equalsInEPS(calculatedUnit.position.x, correctUnit.position.x))
            assertTrue(equalsInEPS(calculatedUnit.position.y, correctUnit.position.y))

            assertEquals(calculatedUnit.onGround, correctUnit.onGround)
            assertEquals(calculatedUnit.onLadder, correctUnit.onLadder)
            assertEquals(calculatedUnit.jumpState.canCancel, correctUnit.jumpState.canCancel)
            assertEquals(calculatedUnit.jumpState.canJump, correctUnit.jumpState.canJump)
            assertTrue(equalsInEPS(calculatedUnit.jumpState.maxTime, correctUnit.jumpState.maxTime))
            assertTrue(equalsInEPS(calculatedUnit.jumpState.speed, correctUnit.jumpState.speed))

            if (correctUnit.weapon != null) {
                assertTrue(calculatedUnit.weapon != null)
                assertEquals(calculatedUnit.weapon!!.typ, correctUnit.weapon!!.typ)
                // todo !!!
//                if (correctUnit.weapon!!.lastAngle != null) {
//                    assertTrue(equalsInEPS(calculatedUnit.weapon!!.lastAngle!!, correctUnit.weapon!!.lastAngle!!))
//                } else {
//                    assertNull(calculatedUnit.weapon!!.lastAngle)
//                }
                // todo !!!
//                if (correctUnit.weapon!!.fireTimer != null) {
//                    assertTrue(equalsInEPS(calculatedUnit.weapon!!.fireTimer!!, correctUnit.weapon!!.fireTimer!!))
//                } else {
//                    assertNull(calculatedUnit.weapon!!.fireTimer)
//                }
            }
            assertEquals(calculatedUnit.health, correctUnit.health)
            assertEquals(correctNextGame.lootBoxes.size, newGame.lootBoxes.size)
            assertEquals(correctNextGame.bullets.size, newGame.bullets.size)
            // todo velocity is random in spread value!!
            // todo рассчитывать вероятность попадания?!
//            assertTrue(correctNextGame.bullets.all { correct ->
//                newGame.bullets.any {
//                    equalsInEPS(correct.position.x, it.position.x) &&
//                    equalsInEPS(correct.position.y, it.position.y) &&
//                    equalsInEPS(correct.velocity.x, it.velocity.x) &&
//                    equalsInEPS(correct.velocity.y, it.velocity.y)
//                }
//            })
//                assertEquals(newGame.bullets, correctNextGame.bullets)
            // todo pass newGame?
            newGame
        }
    }
}