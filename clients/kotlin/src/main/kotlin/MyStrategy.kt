import model.*
import strategies.MySituativeStrategy

abstract class Strategy {
    abstract fun getAction(me: model.Unit, game: Game, debug: Debug): UnitAction
}

class MyStrategy: MySituativeStrategy() {}