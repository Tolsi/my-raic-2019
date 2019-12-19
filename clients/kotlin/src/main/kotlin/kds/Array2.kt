package kds

inline fun <TGen : Any, RGen : Any> Array2<TGen>.map2(gen: (x: Int, y: Int, v: TGen) -> RGen): Array2<RGen> =
    Array2<RGen>(width, height) {
        val x = it % width
        val y = it / width
        gen(x, y, this[x, y])
    }

// AUTOGENERATED: DO NOT MODIFY MANUALLY!


@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class Array2<TGen>(val width: Int, val height: Int, val data: Array<TGen>) : Iterable<TGen> {
    companion object {
        inline operator fun <TGen : Any> invoke(width: Int, height: Int, fill: TGen): Array2<TGen> =
            Array2<TGen>(width, height, Array<Any>(width * height) { fill } as Array<TGen>)

        inline operator fun <TGen : Any> invoke(width: Int, height: Int, gen: (n: Int) -> TGen): Array2<TGen> =
            Array2<TGen>(width, height, Array<Any>(width * height) { gen(it) } as Array<TGen>)

        inline fun <TGen : Any> withGen(width: Int, height: Int, gen: (x: Int, y: Int) -> TGen): Array2<TGen> =
            Array2<TGen>(width, height, Array<Any>(width * height) { gen(it % width, it / width) } as Array<TGen>)

        inline operator fun <TGen : Any> invoke(rows: List<List<TGen>>): Array2<TGen> {
            val width = rows[0].size
            val height = rows.size
            val anyCell = rows[0][0]
            return (Array2<TGen>(width, height) { anyCell }).apply { set(rows) }
        }

        inline operator fun <TGen : Any> invoke(
            map: String,
            marginChar: Char = '\u0000',
            gen: (char: Char, x: Int, y: Int) -> TGen
        ): Array2<TGen> {
            val lines = map.lines()
                .map {
                    val res = it.trim()
                    if (res.startsWith(marginChar)) {
                        res.substring(0, res.length)
                    } else {
                        res
                    }
                }
                .filter { it.isNotEmpty() }
            val width = lines.map { it.length }.max() ?: 0
            val height = lines.size

            return Array2<TGen>(width, height) { n ->
                val x = n % width
                val y = n / width
                gen(lines.getOrNull(y)?.getOrNull(x) ?: ' ', x, y)
            }
        }

        inline operator fun <TGen : Any> invoke(
            map: String,
            default: TGen,
            transform: Map<Char, TGen>
        ): Array2<TGen> {
            return invoke(map) { c, x, y -> transform[c] ?: default }
        }

        inline fun <TGen : Any> fromString(
            maps: Map<Char, TGen>,
            default: TGen,
            code: String,
            marginChar: Char = '\u0000'
        ): Array2<TGen> {
            return invoke(code, marginChar = marginChar) { c, _, _ -> maps[c] ?: default }
        }
    }

    fun set(rows: List<List<TGen>>) {
        var n = 0
        for (y in rows.indices) {
            val row = rows[y]
            for (x in row.indices) {
                this.data[n++] = row[x]
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other is Array2<*/*TGen*/>) && this.width == other.width && this.height == other.height && this.data.contentEquals(
            other.data
        )
    }

    override fun hashCode(): Int = width + height + data.hashCode()

    private fun index(x: Int, y: Int): Int {
        if ((x !in 0 until width) || (y !in 0 until height)) throw IndexOutOfBoundsException()
        return y * width + x
    }

    operator fun get(x: Int, y: Int): TGen = data[index(x, y)]
    operator fun set(x: Int, y: Int, value: TGen): Unit = run { data[index(x, y)] = value }
    fun tryGet(x: Int, y: Int): TGen? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: TGen): Unit = run { if (inside(x, y)) data[index(x, y)] = value }

    fun inside(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < width && y < height

    operator fun contains(v: TGen): Boolean = this.data.contains(v)

    inline fun each(callback: (x: Int, y: Int, v: TGen) -> Unit) {
        var n = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                callback(x, y, data[n++])
            }
        }
    }

    inline fun fill(gen: (old: TGen) -> TGen) {
        var n = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                data[n] = gen(data[n])
                n++
            }
        }
    }

    fun getPositionsWithValue(value: TGen) =
        data.indices.filter { data[it] == value }.map { Pair(it % width, it / width) }

    fun clone() = Array2<TGen>(width, height, data.copyOf())

    fun dump() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                print(this[x, y])
            }
            println()
        }
    }

    override fun iterator(): Iterator<TGen> = data.iterator()

    fun toStringList(charMap: (TGen) -> Char, margin: String = ""): List<String> {
        return (0 until height).map { y ->
            margin + String(CharArray(width) { x -> charMap(this[x, y]) })
        }
    }

    fun toString(margin: String = "", charMap: (TGen) -> Char): String =
        toStringList(charMap, margin = margin).joinToString("\n")

    fun toString(map: Map<TGen, Char>, margin: String = ""): String = toString(margin = margin) { map[it] ?: ' ' }

    override fun toString(): String = (0 until height).map { y ->
        (0 until width).map { x -> this[x, y] }.joinToString(", ")
    }.joinToString("\n")
}


// Int

@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class IntArray2(val width: Int, val height: Int, val data: IntArray) : Iterable<Int> {
    companion object {
        inline operator fun  invoke(width: Int, height: Int, fill: Int): IntArray2 =
            IntArray2(width, height, IntArray(width * height) { fill } as IntArray)

        inline operator fun  invoke(width: Int, height: Int, gen: (n: Int) -> Int): IntArray2 =
            IntArray2(width, height, IntArray(width * height) { gen(it) } as IntArray)

        inline fun  withGen(width: Int, height: Int, gen: (x: Int, y: Int) -> Int): IntArray2 =
            IntArray2(width, height, IntArray(width * height) { gen(it % width, it / width) } as IntArray)

        inline operator fun  invoke(rows: List<List<Int>>): IntArray2 {
            val width = rows[0].size
            val height = rows.size
            val anyCell = rows[0][0]
            return (IntArray2(width, height) { anyCell }).apply { set(rows) }
        }

        inline operator fun  invoke(
            map: String,
            marginChar: Char = '\u0000',
            gen: (char: Char, x: Int, y: Int) -> Int
        ): IntArray2 {
            val lines = map.lines()
                .map {
                    val res = it.trim()
                    if (res.startsWith(marginChar)) {
                        res.substring(0, res.length)
                    } else {
                        res
                    }
                }
                .filter { it.isNotEmpty() }
            val width = lines.map { it.length }.max() ?: 0
            val height = lines.size

            return IntArray2(width, height) { n ->
                val x = n % width
                val y = n / width
                gen(lines.getOrNull(y)?.getOrNull(x) ?: ' ', x, y)
            }
        }

        inline operator fun  invoke(
            map: String,
            default: Int,
            transform: Map<Char, Int>
        ): IntArray2 {
            return invoke(map) { c, x, y -> transform[c] ?: default }
        }

        inline fun  fromString(
            maps: Map<Char, Int>,
            default: Int,
            code: String,
            marginChar: Char = '\u0000'
        ): IntArray2 {
            return invoke(code, marginChar = marginChar) { c, _, _ -> maps[c] ?: default }
        }
    }

    fun set(rows: List<List<Int>>) {
        var n = 0
        for (y in rows.indices) {
            val row = rows[y]
            for (x in row.indices) {
                this.data[n++] = row[x]
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other is IntArray2) && this.width == other.width && this.height == other.height && this.data.contentEquals(
            other.data
        )
    }

    override fun hashCode(): Int = width + height + data.hashCode()

    private fun index(x: Int, y: Int): Int {
        if ((x !in 0 until width) || (y !in 0 until height)) throw IndexOutOfBoundsException()
        return y * width + x
    }

    operator fun get(x: Int, y: Int): Int = data[index(x, y)]
    operator fun set(x: Int, y: Int, value: Int): Unit = run { data[index(x, y)] = value }
    fun tryGet(x: Int, y: Int): Int? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: Int): Unit = run { if (inside(x, y)) data[index(x, y)] = value }

    fun inside(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < width && y < height

    operator fun contains(v: Int): Boolean = this.data.contains(v)

    inline fun each(callback: (x: Int, y: Int, v: Int) -> Unit) {
        var n = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                callback(x, y, data[n++])
            }
        }
    }

    inline fun fill(gen: (old: Int) -> Int) {
        var n = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                data[n] = gen(data[n])
                n++
            }
        }
    }

    fun getPositionsWithValue(value: Int) =
        data.indices.filter { data[it] == value }.map { Pair(it % width, it / width) }

    fun clone() = IntArray2(width, height, data.copyOf())

    fun dump() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                print(this[x, y])
            }
            println()
        }
    }

    override fun iterator(): Iterator<Int> = data.iterator()

    fun toStringList(charMap: (Int) -> Char, margin: String = ""): List<String> {
        return (0 until height).map { y ->
            margin + String(CharArray(width) { x -> charMap(this[x, y]) })
        }
    }

    fun toString(margin: String = "", charMap: (Int) -> Char): String =
        toStringList(charMap, margin = margin).joinToString("\n")

    fun toString(map: Map<Int, Char>, margin: String = ""): String = toString(margin = margin) { map[it] ?: ' ' }

    override fun toString(): String = (0 until height).map { y ->
        (0 until width).map { x -> this[x, y] }.joinToString(", ")
    }.joinToString("\n")
}


// Double

@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class DoubleArray2(val width: Int, val height: Int, val data: DoubleArray) : Iterable<Double> {
    companion object {
        inline operator fun  invoke(width: Int, height: Int, fill: Double): DoubleArray2 =
            DoubleArray2(width, height, DoubleArray(width * height) { fill } as DoubleArray)

        inline operator fun  invoke(width: Int, height: Int, gen: (n: Int) -> Double): DoubleArray2 =
            DoubleArray2(width, height, DoubleArray(width * height) { gen(it) } as DoubleArray)

        inline fun  withGen(width: Int, height: Int, gen: (x: Int, y: Int) -> Double): DoubleArray2 =
            DoubleArray2(width, height, DoubleArray(width * height) { gen(it % width, it / width) } as DoubleArray)

        inline operator fun  invoke(rows: List<List<Double>>): DoubleArray2 {
            val width = rows[0].size
            val height = rows.size
            val anyCell = rows[0][0]
            return (DoubleArray2(width, height) { anyCell }).apply { set(rows) }
        }

        inline operator fun  invoke(
            map: String,
            marginChar: Char = '\u0000',
            gen: (char: Char, x: Int, y: Int) -> Double
        ): DoubleArray2 {
            val lines = map.lines()
                .map {
                    val res = it.trim()
                    if (res.startsWith(marginChar)) {
                        res.substring(0, res.length)
                    } else {
                        res
                    }
                }
                .filter { it.isNotEmpty() }
            val width = lines.map { it.length }.max() ?: 0
            val height = lines.size

            return DoubleArray2(width, height) { n ->
                val x = n % width
                val y = n / width
                gen(lines.getOrNull(y)?.getOrNull(x) ?: ' ', x, y)
            }
        }

        inline operator fun  invoke(
            map: String,
            default: Double,
            transform: Map<Char, Double>
        ): DoubleArray2 {
            return invoke(map) { c, x, y -> transform[c] ?: default }
        }

        inline fun  fromString(
            maps: Map<Char, Double>,
            default: Double,
            code: String,
            marginChar: Char = '\u0000'
        ): DoubleArray2 {
            return invoke(code, marginChar = marginChar) { c, _, _ -> maps[c] ?: default }
        }
    }

    fun set(rows: List<List<Double>>) {
        var n = 0
        for (y in rows.indices) {
            val row = rows[y]
            for (x in row.indices) {
                this.data[n++] = row[x]
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other is DoubleArray2) && this.width == other.width && this.height == other.height && this.data.contentEquals(
            other.data
        )
    }

    override fun hashCode(): Int = width + height + data.hashCode()

    private fun index(x: Int, y: Int): Int {
        if ((x !in 0 until width) || (y !in 0 until height)) throw IndexOutOfBoundsException()
        return y * width + x
    }

    operator fun get(x: Int, y: Int): Double = data[index(x, y)]
    operator fun set(x: Int, y: Int, value: Double): Unit = run { data[index(x, y)] = value }
    fun tryGet(x: Int, y: Int): Double? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: Double): Unit = run { if (inside(x, y)) data[index(x, y)] = value }

    fun inside(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < width && y < height

    operator fun contains(v: Double): Boolean = this.data.contains(v)

    inline fun each(callback: (x: Int, y: Int, v: Double) -> Unit) {
        var n = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                callback(x, y, data[n++])
            }
        }
    }

    inline fun fill(gen: (old: Double) -> Double) {
        var n = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                data[n] = gen(data[n])
                n++
            }
        }
    }

    fun getPositionsWithValue(value: Double) =
        data.indices.filter { data[it] == value }.map { Pair(it % width, it / width) }

    fun clone() = DoubleArray2(width, height, data.copyOf())

    fun dump() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                print(this[x, y])
            }
            println()
        }
    }

    override fun iterator(): Iterator<Double> = data.iterator()

    fun toStringList(charMap: (Double) -> Char, margin: String = ""): List<String> {
        return (0 until height).map { y ->
            margin + String(CharArray(width) { x -> charMap(this[x, y]) })
        }
    }

    fun toString(margin: String = "", charMap: (Double) -> Char): String =
        toStringList(charMap, margin = margin).joinToString("\n")

    fun toString(map: Map<Double, Char>, margin: String = ""): String = toString(margin = margin) { map[it] ?: ' ' }

    override fun toString(): String = (0 until height).map { y ->
        (0 until width).map { x -> this[x, y] }.joinToString(", ")
    }.joinToString("\n")
}


// Float

@Suppress("NOTHING_TO_INLINE", "RemoveExplicitTypeArguments")
data class FloatArray2(val width: Int, val height: Int, val data: FloatArray) : Iterable<Float> {
    companion object {
        inline operator fun  invoke(width: Int, height: Int, fill: Float): FloatArray2 =
            FloatArray2(width, height, FloatArray(width * height) { fill } as FloatArray)

        inline operator fun  invoke(width: Int, height: Int, gen: (n: Int) -> Float): FloatArray2 =
            FloatArray2(width, height, FloatArray(width * height) { gen(it) } as FloatArray)

        inline fun  withGen(width: Int, height: Int, gen: (x: Int, y: Int) -> Float): FloatArray2 =
            FloatArray2(width, height, FloatArray(width * height) { gen(it % width, it / width) } as FloatArray)

        inline operator fun  invoke(rows: List<List<Float>>): FloatArray2 {
            val width = rows[0].size
            val height = rows.size
            val anyCell = rows[0][0]
            return (FloatArray2(width, height) { anyCell }).apply { set(rows) }
        }

        inline operator fun  invoke(
            map: String,
            marginChar: Char = '\u0000',
            gen: (char: Char, x: Int, y: Int) -> Float
        ): FloatArray2 {
            val lines = map.lines()
                .map {
                    val res = it.trim()
                    if (res.startsWith(marginChar)) {
                        res.substring(0, res.length)
                    } else {
                        res
                    }
                }
                .filter { it.isNotEmpty() }
            val width = lines.map { it.length }.max() ?: 0
            val height = lines.size

            return FloatArray2(width, height) { n ->
                val x = n % width
                val y = n / width
                gen(lines.getOrNull(y)?.getOrNull(x) ?: ' ', x, y)
            }
        }

        inline operator fun  invoke(
            map: String,
            default: Float,
            transform: Map<Char, Float>
        ): FloatArray2 {
            return invoke(map) { c, x, y -> transform[c] ?: default }
        }

        inline fun  fromString(
            maps: Map<Char, Float>,
            default: Float,
            code: String,
            marginChar: Char = '\u0000'
        ): FloatArray2 {
            return invoke(code, marginChar = marginChar) { c, _, _ -> maps[c] ?: default }
        }
    }

    fun set(rows: List<List<Float>>) {
        var n = 0
        for (y in rows.indices) {
            val row = rows[y]
            for (x in row.indices) {
                this.data[n++] = row[x]
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other is FloatArray2) && this.width == other.width && this.height == other.height && this.data.contentEquals(
            other.data
        )
    }

    override fun hashCode(): Int = width + height + data.hashCode()

    private fun index(x: Int, y: Int): Int {
        if ((x !in 0 until width) || (y !in 0 until height)) throw IndexOutOfBoundsException()
        return y * width + x
    }

    operator fun get(x: Int, y: Int): Float = data[index(x, y)]
    operator fun set(x: Int, y: Int, value: Float): Unit = run { data[index(x, y)] = value }
    fun tryGet(x: Int, y: Int): Float? = if (inside(x, y)) data[index(x, y)] else null
    fun trySet(x: Int, y: Int, value: Float): Unit = run { if (inside(x, y)) data[index(x, y)] = value }

    fun inside(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < width && y < height

    operator fun contains(v: Float): Boolean = this.data.contains(v)

    inline fun each(callback: (x: Int, y: Int, v: Float) -> Unit) {
        var n = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                callback(x, y, data[n++])
            }
        }
    }

    inline fun fill(gen: (old: Float) -> Float) {
        var n = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                data[n] = gen(data[n])
                n++
            }
        }
    }

    fun getPositionsWithValue(value: Float) =
        data.indices.filter { data[it] == value }.map { Pair(it % width, it / width) }

    fun clone() = FloatArray2(width, height, data.copyOf())

    fun dump() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                print(this[x, y])
            }
            println()
        }
    }

    override fun iterator(): Iterator<Float> = data.iterator()

    fun toStringList(charMap: (Float) -> Char, margin: String = ""): List<String> {
        return (0 until height).map { y ->
            margin + String(CharArray(width) { x -> charMap(this[x, y]) })
        }
    }

    fun toString(margin: String = "", charMap: (Float) -> Char): String =
        toStringList(charMap, margin = margin).joinToString("\n")

    fun toString(map: Map<Float, Char>, margin: String = ""): String = toString(margin = margin) { map[it] ?: ' ' }

    override fun toString(): String = (0 until height).map { y ->
        (0 until width).map { x -> this[x, y] }.joinToString(", ")
    }.joinToString("\n")
}
