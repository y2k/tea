package y2k.tea

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
            cmd.dispatchers.forEach { it(::dispatch) }
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
