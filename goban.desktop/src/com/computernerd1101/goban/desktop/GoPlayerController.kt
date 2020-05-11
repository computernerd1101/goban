package com.computernerd1101.goban.desktop

abstract class GoPlayerController: Cloneable {

    protected open fun onClone() = Unit

    public final override fun clone(): GoPlayerController {
        return (super.clone() as GoPlayerController).apply {
            onClone()
        }
    }

}