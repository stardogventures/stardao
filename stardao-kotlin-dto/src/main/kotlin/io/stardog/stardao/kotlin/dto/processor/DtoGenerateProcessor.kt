package io.stardog.stardao.kotlin.dto.processor

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import kotlin.reflect.jvm.internal.impl.name.FqName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.stardog.stardao.kotlin.dto.annotations.DtoFieldRequired
import io.stardog.stardao.kotlin.dto.annotations.DtoGenerate
import java.util.*
import javax.lang.model.element.*
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap

class DtoGenerateProcessor: AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(DtoGenerate::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun process(annotations: MutableSet<out TypeElement>, env: RoundEnvironment): Boolean {
        env.getElementsAnnotatedWith(DtoGenerate::class.java).forEach {
            val baseClassName = it.simpleName.toString()
            val packageName = processingEnv.elementUtils.getPackageOf(it).toString()
            val dtoGenerateAnnotation = it.getAnnotation(DtoGenerate::class.java)
            for (dtoType in dtoGenerateAnnotation.types) {
                val includeToPartial = dtoType != "Partial" && dtoGenerateAnnotation.types.contains("Partial")
                val file = generateClass(it as TypeElement, packageName, baseClassName, dtoType, includeToPartial)
                writeFile("$dtoType$baseClassName", file)
            }
        }
        return true
    }

    fun toAnnotationMap(ann: AnnotationMirror): Map<String,AnnotationValue?> {
        val result = mutableMapOf<String,AnnotationValue?>()
        for ((k,v) in ann.elementValues) {
            result[k.toString()] = v
        }
        return result
    }

    fun getDtoFieldAnnotationRequired(field: Element, dtoType: String): DtoFieldRequired? {
        val dtoFieldAnnotations = field.annotationMirrors.filter { it.annotationType.toString() == "io.stardog.stardao.kotlin.dto.annotations.DtoField" }
        for (ann in dtoFieldAnnotations) {
            val map = toAnnotationMap(ann)
            if (map["type()"].toString() == "\"$dtoType\"") {
                return DtoFieldRequired.valueOf(map["required()"]!!.value.toString())
            }
        }
        return null
    }

    fun getFieldRequired(field: Element, dtoType: String): DtoFieldRequired {
        // check for the @DtoField annotation
        val explicitRequired = getDtoFieldAnnotationRequired(field, dtoType)
        if (explicitRequired != null) {
            return explicitRequired
        }

        // magical behaviors for Create and Update
        if (dtoType == "Create") {
            // for the Create type, any field marked @Creatable or @Updatable is included;
            // if the field is non-nullable with no default, the field is required
            val isUpdatable = field.annotationMirrors.filter { it.annotationType.toString() == "io.stardog.stardao.annotations.Updatable" }.isNotEmpty()
            val isCreatable = field.annotationMirrors.filter { it.annotationType.toString() == "io.stardog.stardao.annotations.Creatable" }.isNotEmpty()
            val isNullable = field.annotationMirrors.filter { it.annotationType.toString().endsWith(".Nullable") }.isNotEmpty()

            if (!isUpdatable && !isCreatable) {
                return DtoFieldRequired.ABSENT
            }
            if (isNullable) {
                return DtoFieldRequired.OPTIONAL
            }
            return DtoFieldRequired.REQUIRED

        } else if (dtoType == "Update") {
            // for the Update type, any field marked @Updatable is included; all fields are always optional
            val isUpdatable = field.annotationMirrors.filter { it.annotationType.toString() == "io.stardog.stardao.annotations.Updatable" }.isNotEmpty()
            if (isUpdatable) {
                return DtoFieldRequired.OPTIONAL
            } else {
                return DtoFieldRequired.ABSENT
            }
        } else {
            // for other types, default is OPTIONAL for all fields unless specified otherwise
            return DtoFieldRequired.OPTIONAL
        }
    }

    fun generateClass(baseClass: TypeElement, packageName: String, baseClassName: String, dtoType: String, includeToPartial: Boolean): FileSpec {
        val dtoClassName = "$dtoType$baseClassName"
        val classNameLc = baseClassName.toLowerCase()
        val typeBuilder = TypeSpec.classBuilder(dtoClassName)
                .addModifiers(KModifier.DATA)
        val primaryConBuilder = FunSpec.constructorBuilder()
        val dataParams = StringJoiner(", ")

        baseClass.enclosedElements.forEach {
            val propertyName = it.simpleName.toString()
            val annotations = it.annotationMirrors
                    .map { m -> AnnotationSpec.get(m).toBuilder().useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD).build() }
                    .filter { m -> !m.className.toString().endsWith(".NotNull") && !m.className.toString().endsWith(".Nullable") && !m.className.toString().endsWith(".DtoField") }

            val required = getFieldRequired(it, dtoType)

            if (it.kind == ElementKind.FIELD && propertyName != "Companion" && required != DtoFieldRequired.ABSENT) {
                val isNullable = required == DtoFieldRequired.OPTIONAL
                val propertyType = javaToKotlinType(it.asType().asTypeName(), annotations).copy(nullable = isNullable)
                if (isNullable) {
                    primaryConBuilder.addParameter(ParameterSpec.builder(propertyName, propertyType)
                            .defaultValue("null")
                            .addAnnotations(annotations)
                            .build())
                } else {
                    primaryConBuilder.addParameter(ParameterSpec.builder(propertyName, propertyType)
                            .addAnnotations(annotations)
                            .build())
                }
                typeBuilder.addProperty(PropertySpec.builder(propertyName, propertyType)
                        .initializer(propertyName)
                        .build())
                dataParams.add("$propertyName = $classNameLc.$propertyName")
            }
        }
        // copy all annotations from the base type other than @DtoGenerate itself and kotlin metadata
        baseClass.annotationMirrors.forEach {
            if (it.annotationType.toString() != "io.stardog.stardao.kotlin.dto.annotations.DtoGenerate"
                    && it.annotationType.toString() != "kotlin.Metadata") {
                typeBuilder.addAnnotation(AnnotationSpec.get(it))
            }
        }
        typeBuilder.primaryConstructor(primaryConBuilder.build())
        typeBuilder.addFunction(FunSpec.constructorBuilder()
                .addParameter(ParameterSpec.builder(classNameLc, baseClass.asType().asTypeName()).build())
                .callThisConstructor(dataParams.toString())
                .build())
        if (includeToPartial) {
            typeBuilder.addFunction(FunSpec.builder("toPartial")
                    .build())
        }

        val file = FileSpec.builder(packageName, dtoClassName)
                .addType(typeBuilder.build())
                .build()

        return file
    }

    // asTypeName() currently returns java classes for certain classes such as String when we need the Kotlin
    // version. As a workaround use the following adapted from https://github.com/square/kotlinpoet/issues/236#issuecomment-377784099
    fun javaToKotlinType(javaType: TypeName, annotations: List<AnnotationSpec>): TypeName {
        if (javaType is ParameterizedTypeName) {
            val rawTypeClass = javaToKotlinType(javaType.rawType, annotations) as ClassName
            val typedArgs = javaType.typeArguments.map { javaToKotlinType(it, emptyList()) }.toMutableList()
            val hasNullableValues = annotations.filter { it.className.toString() == "io.stardog.stardao.auto.annotations.HasNullableValues" }.isNotEmpty()
            if (hasNullableValues) {
                typedArgs[typedArgs.size-1] = typedArgs[typedArgs.size-1].copy(nullable = true)
            }
            return rawTypeClass.parameterizedBy(*typedArgs.toTypedArray())
        } else {
            val className =
                    JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(javaType.toString()))
                            ?.asSingleFqName()?.asString()

            return if (className == null) {
                javaType
            } else {
                ClassName.bestGuess(className)
            }
        }
    }

    fun writeFile(className: String, file: FileSpec) {
        val kaptKotlinGeneratedDir = processingEnv.options["kapt.kotlin.generated"]
        file.writeTo(File(kaptKotlinGeneratedDir, "$className.kt"))
    }
}