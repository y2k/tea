@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package y2k.tea

import java.io.Closeable

data class Cmd<out T>(val dispatchers: List<Dispatch<T>>) {

    companion object {
        fun <T> none(): Cmd<T> = Cmd(emptyList())
        fun <T> batch(vararg xs: Cmd<T>): Cmd<T> = Cmd(xs.flatMap { it.dispatchers })
        fun <T> ofMsg(msg: T): Cmd<T> = ofFunc { msg }
        fun <T> ofFunc(f: () -> T): Cmd<T> =
            Cmd(listOf { dispatch -> dispatch(f()) })

        fun ofAction(f: () -> Unit): Cmd<Nothing> =
            Cmd(listOf { _ -> f() })
    }
}

typealias Dispatch<T> = ((T) -> Unit) -> Unit

interface Sub<out T> {

    fun attach(dispatch: (T) -> Unit): Closeable

    companion object {

        fun <T> empty(): Sub<T> = batch()

        fun <T> ofFunc(f: ((T) -> Unit) -> Closeable): Sub<T> =
            object : Sub<T> {
                override fun attach(dispatch: (T) -> Unit) =
                    f(dispatch)
            }

        fun <T> batch(vararg xs: Sub<T>): Sub<T> =
            object : Sub<T> {
                override fun attach(dispatch: (T) -> Unit): Closeable {
                    val closeList = xs.map { it.attach(dispatch) }
                    return Closeable { closeList.forEach { it.close() } }
                }
            }
    }
}
