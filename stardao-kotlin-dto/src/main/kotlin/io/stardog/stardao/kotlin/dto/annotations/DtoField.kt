package io.stardog.stardao.kotlin.dto.annotations

@Target(AnnotationTarget.FIELD)
annotation class DtoField(val type: String = "Dto", val required: DtoFieldRequired = DtoFieldRequired.OPTIONAL)

enum class DtoFieldRequired { REQUIRED, OPTIONAL, ABSENT }