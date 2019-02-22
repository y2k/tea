@file:Suppress("unused")

package y2k.tea

import java.io.Closeable

fun <T, R> Cmd<T>.map(f: (T) -> R): Cmd<R> =
    object : Cmd<R> {
        override suspend fun execute(dispatch: (R) -> Unit) =
            this@map.execute { dispatch(f(it)) }
    }

fun <T, R> Sub<T>.map(f: (T) -> R): Sub<R> =
    object : Sub<R> {
        override fun attach(dispatch: (R) -> Unit) =
            this@map.attach { dispatch(f(it)) }

        override fun detach() = this@map.detach()
    }

fun <T, R> (((T) -> Unit) -> Closeable).toCmd(f: (T) -> R): Sub<R> =
    object : Sub<R> {

        private lateinit var closeable: Closeable

        override fun attach(dispatch: (R) -> Unit) {
            closeable = this@toCmd { dispatch(f(it)) }
        }

        override fun detach() = closeable.close()
    }
