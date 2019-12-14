package test

import extensions.*
import model.Game

object CompareGames {
    val assertEPS = 1

    fun equalsInEPS(value: Double, correct: Double): Boolean {
        val res = Math.abs(value - correct) <= assertEPS
        return res
    }

    fun equals(myUnitId: Int, calculatedGame: Game, correctGame: Game): Boolean {
        val calculatedUnit = calculatedGame.unitById(myUnitId)
        val correctUnit = correctGame.unitById(myUnitId)

        var result = true
        result = result && equalsInEPS(calculatedUnit.position.x, correctUnit.position.x)
        result = result && equalsInEPS(calculatedUnit.position.y, correctUnit.position.y)

        result = result && calculatedUnit.onGround == correctUnit.onGround
        result = result && calculatedUnit.onLadder == correctUnit.onLadder
        result = result && calculatedUnit.jumpState.canCancel == correctUnit.jumpState.canCancel
        result = result && calculatedUnit.jumpState.canJump == correctUnit.jumpState.canJump
        result = result && equalsInEPS(calculatedUnit.jumpState.maxTime, correctUnit.jumpState.maxTime)
        result = result && equalsInEPS(calculatedUnit.jumpState.speed, correctUnit.jumpState.speed)

        if (correctUnit.weapon != null) {
            result = result && calculatedUnit.weapon != null
            result = result && calculatedUnit.weapon!!.typ == correctUnit.weapon!!.typ
            // todo!
//            if (correctUnit.weapon!!.lastAngle != null) {
//                result = result && calculatedUnit.weapon?.lastAngle != null && (equalsInEPS(calculatedUnit.weapon?.lastAngle!!, correctUnit.weapon?.lastAngle!!))
//            } else {
//                result = result && (calculatedUnit.weapon!!.lastAngle == null)
//            }
            if (correctUnit.weapon!!.fireTimer != null) {
                result = result && calculatedUnit.weapon != null && Math.abs(calculatedUnit.weapon?.fireTimer!! - correctUnit.weapon?.fireTimer!!) < 0.001
            } else {
                result = result && (calculatedUnit.weapon!!.fireTimer == null)
            }
        }
        result = result && calculatedUnit.health == correctUnit.health
        //  it's ok because we don't control enemy
//        result = result && correctGame.lootBoxes.size == calculatedGame.lootBoxes.size
        // it's ok because we don't control enemy
//        result = result && correctGame.bullets.size == calculatedGame.bullets.size
        // bullets use random, we can't use it
//        var bulletsPos = true
//        correctGame.bullets.sortedBy { it.x + it.y * 100 }.zip(calculatedGame.bullets.sortedBy { it.x + it.y * 100 }).forEach { (f,s) ->
//            bulletsPos = bulletsPos && equalsInEPS(f.position.x, s.position.x)
//            bulletsPos = bulletsPos && equalsInEPS(f.position.y, s.position.y)
//        }
//        result = result && bulletsPos

        return result
    }
}