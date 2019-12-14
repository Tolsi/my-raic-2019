import model.Game
import model.UnitAction
import simulation.WorldSimulation
import strategies.FastJumpyQuickStartStrategy
import strategies.QuickStartStrategy
import test.CompareGames
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.HashMap
import util.StreamUtil

class DebugSimulationApp @Throws(IOException::class)
internal constructor(host: String, port: Int, token: String) {
    private val inputStream: InputStream
    private val outputStream: OutputStream

    init {
        val socket = Socket(host, port)
        socket.tcpNoDelay = true
        inputStream = BufferedInputStream(socket.getInputStream())
        outputStream = BufferedOutputStream(socket.getOutputStream())
        StreamUtil.writeString(outputStream, token)
        outputStream.flush()
    }

    lateinit var lastGame: Game
    lateinit var predictedNextGame: Game
    lateinit var lastAction: UnitAction

    @Throws(IOException::class)
    internal fun run() {
        val myStrategy = FastJumpyQuickStartStrategy()
        val debug = Debug(outputStream)
        val simulator = WorldSimulation()
        var first = true
        while (true) {
            val message = model.ServerMessageGame.readFrom(inputStream)
            val playerView = message.playerView ?: break
            val myUnit = playerView.game.units.find { it.playerId == playerView.myId }!!
            val actions = HashMap<Int, model.UnitAction>()
            val currentGame = playerView.game
            if (first) {
                first = false
                Global.init(currentGame)
                simulator.lastGame = currentGame
            } else {
                assert(CompareGames.equals(myUnit.id, predictedNextGame, playerView.game),
                        {"Games are not equals on tick ${predictedNextGame.currentTick}"})
            }
            // only for one unit now
            simulator.lastGame = playerView.game
            actions[myUnit.id] = myStrategy.getAction(myUnit, playerView.game, debug)
            predictedNextGame = simulator.tick(actions)
            lastAction = actions[myUnit.id]!!
            lastGame = playerView.game
            model.PlayerMessageGame.ActionMessage(model.Versioned(actions)).writeTo(outputStream)
            outputStream.flush()
        }
    }

    companion object {

        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val host = if (args.size < 1) "127.0.0.1" else args[0]
            val port = if (args.size < 2) 31001 else Integer.parseInt(args[1])
            val token = if (args.size < 3) "0000000000000000" else args[2]
            DebugSimulationApp(host, port, token).run()
        }
    }
}
