package com.computernerd1101.goban.desktop

import kotlinx.coroutines.*
import kotlinx.coroutines.internal.MainDispatcherFactory
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext

object SwingUtilitiesDispatcher: MainCoroutineDispatcher() {

    @InternalCoroutinesApi
    internal class Factory: MainDispatcherFactory {

        override val loadPriority: Int
            get() = 0

        override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher {
            return SwingUtilitiesDispatcher
        }

    }

    override val immediate: MainCoroutineDispatcher
        get() = this

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        SwingUtilities.invokeLater(block)
    }

}