package io.stardog.stardao.kotlin.partial.processor

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import kotlin.reflect.jvm.internal.impl.name.FqName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.stardog.stardao.kotlin.partial.annotations.PartialFieldRequired
import io.stardog.stardao.kotlin.partial.annotations.PartialDataObjects
import java.util.*
import javax.lang.model.element.*
import kotlin.math.log
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap

class PartialDataObjectsProcessor: AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(PartialDataObjects::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun process(annotations: MutableSet<out TypeElement>, env: RoundEnvironment): Boolean {
        env.getElementsAnnotatedWith(PartialDataObjects::class.java).forEach {
            val baseClassName = it.simpleName.toString()
            val packageName = processingEnv.elementUtils.getPackageOf(it).toString()
            val partialGenerateAnnotation = it.getAnnotation(PartialDataObjects::class.java)
            for (partialType in partialGenerateAnnotation.types) {
                val includeToPartial = partialType != "Partial" && partialGenerateAnnotation.types.contains("Partial")
                val file = generateClass(it as TypeElement, packageName, baseClassName, partialType, includeToPartial)
                writeFile("$partialType$baseClassName", file)
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

    fun getPartialFieldAnnotationRequired(field: Element, partialType: String): PartialFieldRequired? {
        val partialFieldAnnotations = field.annotationMirrors.filter { it.annotationType.toString() == "io.stardog.stardao.kotlin.partial.annotations.PartialField" }
        for (ann in partialFieldAnnotations) {
            val map = toAnnotationMap(ann)
            if (map["type()"].toString() == "\"$partialType\"") {
                val required = map["required()"]?.value
                if (required != null) {
                    return PartialFieldRequired.valueOf(required.toString())
                } else {
                    return null
                }
            }
        }
        return null
    }

    fun getPartialFieldAnnotationClassName(field: Element, partialType: String): String? {
        val partialFieldAnnotations = field.annotationMirrors.filter { it.annotationType.toString() == "io.stardog.stardao.kotlin.partial.annotations.PartialField" }
        for (ann in partialFieldAnnotations) {
            val map = toAnnotationMap(ann)
            if (map["type()"].toString() == "\"$partialType\"") {
                val className = map["className()"]?.value
                if (className != null) {
                    return className.toString()
                } else {
                    return null
                }
            }
        }
        return null
    }

    fun getFieldRequired(field: Element, partialType: String): PartialFieldRequired {
        // check for the @PartialField annotation
        val explicitRequired = getPartialFieldAnnotationRequired(field, partialType)
        if (explicitRequired != null) {
            return explicitRequired
        }

        // magical behaviors for Create and Update
        if (partialType == "Create") {
            // for the Create type, any field marked @Creatable or @Updatable is included;
            // if the field is non-nullable with no default, the field is required
            val isUpdatable = field.annotationMirrors.filter { it.annotationType.toString() == "io.stardog.stardao.annotations.Updatable" }.isNotEmpty()
            val isCreatable = field.annotationMirrors.filter { it.annotationType.toString() == "io.stardog.stardao.annotations.Creatable" }.isNotEmpty()
            val isNullable = field.annotationMirrors.filter { it.annotationType.toString().endsWith(".Nullable") }.isNotEmpty()

            if (!isUpdatable && !isCreatable) {
                return PartialFieldRequired.ABSENT
            }
            if (isNullable) {
                return PartialFieldRequired.OPTIONAL
            }
            return PartialFieldRequired.REQUIRED

        } else if (partialType == "Update") {
            // for the Update type, any field marked @Updatable is included; all fields are always optional
            val isUpdatable =
                field.annotationMirrors.filter { it.annotationType.toString() == "io.stardog.stardao.annotations.Updatable" }
                    .isNotEmpty()
            if (isUpdatable) {
                return PartialFieldRequired.OPTIONAL
            } else {
                return PartialFieldRequired.ABSENT
            }
        } else if (partialType == "Dto") {
            // for the Dto type, any field marked @DtoRequired is required; any field marked @DtoAbsent is absent; all other fields are always optional
            var isDtoRequired = field.annotationMirrors.filter { it.annotationType.toString() == "io.stardog.stardao.kotlin.partial.annotations.DtoRequired" }.isNotEmpty()

            // the Dto type will also pick up the "required" property from swagger @ApiModelProperty automatically
            val apiModelProperty = field.annotationMirrors.find { it.annotationType.toString() == "io.swagger.annotations.ApiModelProperty" }
            if (apiModelProperty != null) {
                val propertyMap = toAnnotationMap(apiModelProperty)
                if (propertyMap["required()"]?.toString() == "true") {
                    isDtoRequired = true
                }
            }

            // the @DtoAbsent property will automatically mark a property as not present for Dtos
            val isDtoAbsent = field.annotationMirrors.filter { it.annotationType.toString() == "io.stardog.stardao.kotlin.partial.annotations.DtoAbsent" }.isNotEmpty()

            if (isDtoAbsent) {
                return PartialFieldRequired.ABSENT
            } else if (isDtoRequired) {
                return PartialFieldRequired.REQUIRED
            } else {
                return PartialFieldRequired.OPTIONAL
            }

        } else {
            // for other types, default is OPTIONAL for all fields unless specified otherwise
            return PartialFieldRequired.OPTIONAL
        }
    }

    fun generateClass(baseClass: TypeElement, packageName: String, baseClassName: String, partialType: String, includeToPartial: Boolean): FileSpec {
        val partialClassName = "$partialType$baseClassName"
        val classNameLc = baseClassName.toLowerCase()
        val typeBuilder = TypeSpec.classBuilder(partialClassName)
                .addModifiers(KModifier.DATA)
        val primaryConBuilder = FunSpec.constructorBuilder()
        val dataParams = StringJoiner(", ")
        val partialDataParams = StringJoiner(", ")

        baseClass.enclosedElements.forEach {
            val propertyName = it.simpleName.toString()
            val annotations = it.annotationMirrors
                    .map { m -> AnnotationSpec.get(m).toBuilder().useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD).build() }
                    .filter { m -> !m.className.toString().endsWith(".NotNull") && !m.className.toString().endsWith(".Nullable") && !m.className.toString().endsWith(".PartialField") }

            val required = getFieldRequired(it, partialType)

            if (it.kind == ElementKind.FIELD && !it.modifiers.contains(Modifier.STATIC) && required != PartialFieldRequired.ABSENT) {
                val isNullable = required == PartialFieldRequired.OPTIONAL
                val className = getPartialFieldAnnotationClassName(it, partialType)
                val propertyType: TypeName
                if (className != null) {
                    propertyType = ClassName(packageName, className).copy(nullable = isNullable)
                } else {
                    propertyType = javaToKotlinType(it.asType().asTypeName(), annotations).copy(nullable = isNullable)
                }
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
                partialDataParams.add("$propertyName = $propertyName")
            }
        }
        // copy all annotations from the base type other than @PartialDataObjects itself and kotlin metadata
        baseClass.annotationMirrors.forEach {
            if (it.annotationType.toString() != "io.stardog.stardao.kotlin.partial.annotations.PartialDataObjects"
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
                    .returns(ClassName(packageName, "Partial$baseClassName"))
                    .addCode("return Partial$baseClassName($partialDataParams)")
                    .build())
        }

        val file = FileSpec.builder(packageName, partialClassName)
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
            val hasNullableValues = annotations.filter { it.className.toString().endsWith(".HasNullableValues") }.isNotEmpty()
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
