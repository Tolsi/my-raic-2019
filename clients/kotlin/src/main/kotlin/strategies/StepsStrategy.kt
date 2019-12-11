package strategies

import Debug
import HorizontalDirection
import Strategy
import korma_geom.*
import model.Game
import model.Unit
import model.UnitAction
import model.Vec2Double
import simulation.WorldSimulation

abstract class Step {
    protected abstract val me: model.Unit
    protected var applied: Boolean = false
    open fun apply(action: UnitAction) {
        applied = true
    }
    abstract fun isFinished(): Boolean
    open fun notFinishedSteps(): Step? {
        return this.takeIf { !isFinished() }
    }
    fun combine(vararg steps: Step): CombinedStep {
        return CombinedStep(steps.toList(), me)
    }
}

abstract class AimStep: Step()
abstract class MoveStep: Step()
abstract class JumpStep: Step()

class CombinedStep(val steps: Collection<Step>, override val me: model.Unit) : Step() {
    override fun apply(action: UnitAction) {
        steps.forEach { step ->
            step.apply(action)
        }
    }

    override fun isFinished(): Boolean {
        return applied && steps.all { it.isFinished() }
    }

    override fun notFinishedSteps(): Step? {
        return CombinedStep(steps.filter { !it.isFinished() }, me).takeIf { !isFinished() }
    }

    inline fun <reified T>finishByType(): Step? {
        val newSteps = steps.filterNot { it is T }
        return CombinedStep(newSteps, me).takeIf { newSteps.isNotEmpty() }
    }
}

class InfiniteStepsToDirection(direction: HorizontalDirection, override val me: Unit): MoveStep() {
    private val velocity: Double by lazy { direction.point.x * Global.properties.unitMaxHorizontalSpeed }
    override fun apply(action: model.UnitAction) {
        action.velocity = velocity
        super.apply(action)
    }

    override fun isFinished(): Boolean {
        return false
    }
}

class StepToDirection(direction: HorizontalDirection, override val me: Unit): MoveStep() {
    private val velocity: Double by lazy { direction.point.x * Global.properties.unitMaxHorizontalSpeed }
    override fun apply(action: model.UnitAction) {
        action.velocity = velocity
        super.apply(action)
    }
    override fun isFinished(): Boolean {
        return applied
    }
}

class StepTo(val target: Point, override val me: Unit, val distance: Double = WorldSimulation.EPS) : MoveStep() {
    private val velocity: Double by lazy { (target.x - me.position.x).coerceIn(-Global.properties.unitMaxHorizontalSpeed, Global.properties.unitMaxHorizontalSpeed) }
    private val nextPoint: Point by lazy { target.copy(x = target.x + velocity) }

    override fun apply(action: UnitAction) {
        action.velocity = velocity
        super.apply(action)
    }

    override fun isFinished(): Boolean {
        return applied && target.distanceTo(nextPoint) < distance
    }
}

class InfiniteAimTo(val target: model.Unit, override val me: Unit, val difference: Double = 30.degrees.radians) : AimStep() {
    override fun apply(action: UnitAction) {
        action.aim = Vec2Double(
                target.centerPosition.x - me.centerPosition.x,
                target.centerPosition.y - me.centerPosition.y)
        super.apply(action)
    }

    override fun isFinished(): Boolean {
//        val lastAngle = me.weapon?.lastAngle?.let {lastAngle ->
//            me.centerPosition.angleTo(target.position).radians < difference
//        }
        return false
    }
}

class StepsStrategy(var notFinishedSteps: Step) : Strategy() {
    override fun getAction(me: Unit, game: Game, debug: Debug): UnitAction {
        val action = UnitAction()
        action.aim = Vec2Double(0.0, 0.0)
        notFinishedSteps.apply(action)
        // step self of without finished ones
        notFinishedSteps = notFinishedSteps.notFinishedSteps() ?: notFinishedSteps
        return action
    }

}