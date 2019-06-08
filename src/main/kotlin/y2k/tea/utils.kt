@file:Suppress("unused")

package y2k.tea

fun <T, R> Cmd<T>.map(f: (T) -> R): Cmd<R> =
    Cmd(dispatchers.map<Dispatch<T>, Dispatch<R>> { old ->
        { d -> old { x -> d(f(x)) } }
    })

fun <T, R> Sub<T>.map(f: (T) -> R): Sub<R> =
    object : Sub<R> {
        override fun attach(dispatch: (R) -> Unit) =
            this@map.attach { dispatch(f(it)) }
    }
