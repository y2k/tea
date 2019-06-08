package y2k.tea

import java.io.Closeable

class TeaRuntime<Model, Msg>(
    private val component: TeaComponent<Model, Msg>,
    private val view: TeaView<Model>,
    private val scheduler: (suspend () -> Unit) -> Unit
) {
    private var model: Model? = null
    private var closeSubs: Closeable? = null

    fun attach() {
        val (model, cmd) = component.initialize()
        view.view(model)
        this.model = model
        scheduler {
            cmd.dispatchers.forEach { it(::dispatch) }
        }

        closeSubs = component.sub().attach(::dispatch)
    }

    fun detach() {
        closeSubs?.close()
        closeSubs = null
    }

    fun dispatch(msg: Msg) {
        val (model, cmd) = component.update(model!!, msg)
        view.view(model)
        this.model = model

        scheduler {
            cmd.dispatchers.forEach { it(::dispatch) }
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
