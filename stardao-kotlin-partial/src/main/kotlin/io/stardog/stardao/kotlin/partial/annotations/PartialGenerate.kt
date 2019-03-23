package io.stardog.stardao.kotlin.partial.annotations

@Target(AnnotationTarget.CLASS)
annotation class PartialGenerate(val types: Array<String> = ["Partial", "Update", "Create", "Dto"])