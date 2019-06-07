@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package y2k.tea

data class Cmd<out T>(val dispatchers: List<Dispatch<T>>) {

    companion object {
        fun <T> none(): Cmd<T> = Cmd(emptyList())
        fun <T> batch(vararg xs: Cmd<T>): Cmd<T> = Cmd(xs.flatMap { it.dispatchers })
        fun <T> ofMsg(msg: T): Cmd<T> = ofFunc { msg }
        fun <T> ofFunc(f: suspend () -> T): Cmd<T> =
            Cmd(listOf { dispatch -> dispatch(f()) })
        fun ofAction(f: suspend () -> Unit): Cmd<Nothing> =
            Cmd(listOf { _ -> f() })
    }
}

typealias Dispatch<T> = suspend ((T) -> Unit) -> Unit

interface Sub<out T> {

    fun attach(dispatch: (T) -> Unit)
    fun detach()

    companion object {

        fun <T> empty(): Sub<T> = batch()

        fun <T> ofFunc(f: ((T) -> Unit) -> Unit, dispose: () -> Unit): Sub<T> =
            object : Sub<T> {
                override fun attach(dispatch: (T) -> Unit) = f(dispatch)
                override fun detach() = dispose()
            }

        fun <T> batch(vararg xs: Sub<T>): Sub<T> =
            object : Sub<T> {
                override fun attach(dispatch: (T) -> Unit) = xs.forEach { it.attach(dispatch) }
                override fun detach() = xs.forEach { it.detach() }
            }
    }
}
