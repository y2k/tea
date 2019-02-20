package y2k.tea

import java.io.Closeable

class TeaRuntime<Model, Msg>(
    private val component: TeaComponent<Model, Msg>,
    private val view: TeaView<Model>,
    private val scheduler: (suspend () -> Unit) -> Unit
) {

    private var model: Model? = null
    private var sub: Sub<Msg>? = null

    fun attach() {
        val (model, cmd) = component.initialize()
        view.view(model)
        this.model = model
        scheduler {
            cmd.execute(::dispatch)
        }

        sub = component.sub()
        sub?.attach(::dispatch)
    }

    fun detach() {
        sub?.detach()
    }

    fun dispatch(msg: Msg) {
        val (model, cmd) = component.update(model!!, msg)
        view.view(model)
        this.model = model

        scheduler {
            cmd.execute(::dispatch)
        }
    }
}

fun <T, R> Cmd<T>.map(f: (T) -> R): Cmd<R> =
    object : Cmd<R> {
        override suspend fun execute(dispatch: (R) -> Unit) =
            this@map.execute { dispatch(f(it)) }
    }

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

fun <T, R> Sub<T>.map(f: (T) -> R): Sub<R> =
    object : Sub<R> {
        override fun attach(dispatch: (R) -> Unit) =
            this@map.attach { dispatch(f(it)) }

        override fun detach() = this@map.detach()
    }

interface Sub<out T> {

    fun attach(dispatch: (T) -> Unit)
    fun detach()

    companion object {
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

interface TeaComponent<Model, Msg> {
    fun initialize(): Pair<Model, Cmd<Msg>>
    fun update(model: Model, msg: Msg): Pair<Model, Cmd<Msg>>
    fun sub(): Sub<Msg>
}

interface TeaView<T> {
    fun view(model: T)
}

fun <T, R> (((T) -> Unit) -> Closeable).toCmd(f: (T) -> R): Sub<R> =
    object : Sub<R> {

        private lateinit var closeable: Closeable

        override fun attach(dispatch: (R) -> Unit) {
            closeable = this@toCmd { dispatch(f(it)) }
        }

        override fun detach() = closeable.close()
    }
