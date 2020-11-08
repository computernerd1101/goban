package com.computernerd1101.goban

public var foobar = 42

private fun `access$getFoobar$p`(): Int {
    return foobar
}

private fun `access$setFoobar$p`(value: Int) {
    foobar = value
}

fun accessFoobar() {
    //mainFoobar = ::foobar
}