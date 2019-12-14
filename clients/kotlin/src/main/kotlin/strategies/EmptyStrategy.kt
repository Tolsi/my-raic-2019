package strategies

import Debug
import Strategy
import model.Game
import model.Unit
import model.UnitAction

class EmptyStrategy: Strategy() {
    override fun getAction(me: Unit, game: Game, debug: Debug): UnitAction {
        return UnitAction.Empty
    }

}