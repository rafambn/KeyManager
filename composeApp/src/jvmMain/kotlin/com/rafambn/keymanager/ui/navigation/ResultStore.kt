package com.rafambn.keymanager.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

object LocalResultStore {
    private val LocalResultStore: ProvidableCompositionLocal<ResultStore?> =
        compositionLocalOf { null }

    val current: ResultStore
        @Composable
        get() = LocalResultStore.current ?: error("No ResultStore has been provided")

    infix fun provides(
        store: ResultStore
    ): ProvidedValue<ResultStore?> {
        return LocalResultStore.provides(store)
    }
}

@Composable
fun rememberResultStore(): ResultStore {
    return rememberSaveable(saver = ResultStoreSaver()) {
        ResultStore()
    }
}

class ResultStore {

    val resultStateMap = mutableStateMapOf<String, Any?>()

    inline fun <reified T> getResultState(resultKey: String = T::class.toString()): T? =
        resultStateMap[resultKey] as? T

    inline fun <reified T> setResult(resultKey: String = T::class.toString(), result: T) {
        resultStateMap[resultKey] = result
    }

    inline fun <reified T> removeResult(resultKey: String = T::class.toString()) {
        resultStateMap.remove(resultKey)
    }
}

private fun ResultStoreSaver(): Saver<ResultStore, *> =
    Saver(
        save = { it.resultStateMap.toMap() },
        restore = { ResultStore().apply { resultStateMap.putAll(it) } },
    )
