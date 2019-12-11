import java.io.IOException
import java.io.OutputStream

class Debug(private val stream: OutputStream) {

    fun draw(customData: model.CustomData) {
        try {
            model.PlayerMessageGame.CustomDataMessage(customData).writeTo(stream)
            stream.flush()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        val Mock = Debug(object : OutputStream() {override fun write(p0: Int) {}})
    }
}
