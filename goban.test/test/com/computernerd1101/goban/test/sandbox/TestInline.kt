package com.computernerd1101.goban.test.sandbox

fun main() {
    testInline()
    testLambda()
    testLambdaWithReference()
    testLambdaWithBody()
}

fun regularFunction(a: Int, b: Int): Int = a + b

@Suppress("NOTHING_TO_INLINE")
inline fun inlineFunction(a: Int, b: Int): Int = regularFunction(a, b)

inline fun inlineFunctionWithLambda(a: Int, lambda: (Int, Int) -> Int, b: Int): Int = lambda(a, b)

fun testInline() {
    println("inlineFunction(3, 4) = " + inlineFunction(3, 4))
}

fun testLambda() {
    println("inlineFunctionWithLambda(3, ::regularFunction, 4) = " +
            inlineFunctionWithLambda(3, ::regularFunction, 4))
}

fun testLambdaWithReference() {
    val functionReference: (Int, Int) -> Int = ::inlineFunction
    println("inlineFunctionWithLambda(3, functionReference, 4) = " +
            inlineFunctionWithLambda(3, functionReference, 4))
}

fun testLambdaWithBody() {
    println("inlineFunctionWithLambda(3, { a, b -> a * b }, 4) = " +
            inlineFunctionWithLambda(3, { a, b -> a * b }, 4))
}