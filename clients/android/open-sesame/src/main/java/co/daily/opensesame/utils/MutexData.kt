package co.daily.opensesame.utils

class MutexData<E>(initial: E) {

    private val lock = Object()
    private val value: E = initial

    fun <R> lock(action: E.() -> R): R {
        return synchronized(lock) {
            action(value)
        }
    }
}

class MutexReplaceableData<E>(initial: E) {

    interface Scope<E> {
        var value: E
    }

    private val lock = Object()
    private var data: E = initial

    fun <R> lock(action: Scope<E>.() -> R): R {
        return synchronized(lock) {
            action(object : Scope<E> {
                override var value
                    get() = data
                    set(newValue) {
                        data = newValue
                    }
            })
        }
    }
}