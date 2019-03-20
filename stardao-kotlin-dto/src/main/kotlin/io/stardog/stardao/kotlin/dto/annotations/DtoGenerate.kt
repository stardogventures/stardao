package io.stardog.stardao.kotlin.dto.annotations

@Target(AnnotationTarget.CLASS)
annotation class DtoGenerate(val types: Array<String> = ["Partial", "Update", "Create", "Dto"])