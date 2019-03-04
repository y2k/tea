@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package y2k.tea

interface Cmd<out T> {

    suspend fun execute(dispatch: (T) -> Unit)

    companion object {

        fun <T> ofFunc(f: suspend () -> T): Cmd<T> =
            object : Cmd<T> {
                override suspend fun execute(dispatch: (T) -> Unit) {
                    dispatch(f())
                }
            }

        fun <T> ofAction(f: suspend () -> Unit): Cmd<T> =
            object : Cmd<T> {
                override suspend fun execute(dispatch: (T) -> Unit) {
                    f()
                }
            }

        fun <T> batch(vararg xs: Cmd<T>): Cmd<T> =
            object : Cmd<T> {
                override suspend fun execute(dispatch: (T) -> Unit) =
                    xs.forEach { it.execute(dispatch) }
            }

        fun <T> none(): Cmd<T> = ofAction { }
        fun <T> ofMsg(msg: T): Cmd<T> = ofFunc { msg }
    }
}

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
