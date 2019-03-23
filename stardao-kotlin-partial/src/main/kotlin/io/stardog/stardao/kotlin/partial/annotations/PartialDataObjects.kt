package io.stardog.stardao.kotlin.partial.annotations

@Target(AnnotationTarget.CLASS)
annotation class PartialDataObjects(val types: Array<String> = ["Partial"])