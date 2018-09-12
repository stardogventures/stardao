package io.stardog.stardao.auto.kotlin

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.*
import io.stardog.stardao.auto.annotations.DataPartial
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.platform.JavaToKotlinClassMap
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

@AutoService(Processor::class)
class DataPartialProcessor: AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(DataPartial::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
         return SourceVersion.latest()
    }

    override fun process(annotations: MutableSet<out TypeElement>, env: RoundEnvironment): Boolean {
//        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Starting processing...")
        env.getElementsAnnotatedWith(DataPartial::class.java).forEach {
            val className = "Partial" + it.simpleName.toString()
            val pack = processingEnv.elementUtils.getPackageOf(it).toString()
            val file = generateClass(className, pack, it as TypeElement)
            writeFile(className, file)
        }
        return true
    }

    fun generateClass(className: String, pack: String, type: TypeElement):FileSpec {
        val conBuilder = FunSpec.constructorBuilder()
        val typeBuilder = TypeSpec.classBuilder(className)
                .addModifiers(KModifier.DATA)

        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "Generating class $className...")

        type.enclosedElements.forEach {
            val propertyName = it.simpleName.toString()
            val annotations = it.annotationMirrors
                    .map { m -> AnnotationSpec.get(m) }
                    .filter { m -> !m.type.toString().endsWith(".NotNull") }

            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "Annotation mirrors: " + it.annotationMirrors.size + " " + it.annotationMirrors)

            if (it.kind == ElementKind.FIELD) {
                val propertyType = it.asType().asTypeName().javaToKotlinType().asNullable()
                annotations.forEach { processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "Annotation: $it") }

                conBuilder.addParameter(ParameterSpec.builder(propertyName, propertyType)
                        .defaultValue("null")
                        .addAnnotations(annotations)
                        .build())
                typeBuilder.addProperty(PropertySpec.builder(propertyName, propertyType)
                        .initializer(propertyName)
                        .build())
            }
        }

        typeBuilder.primaryConstructor(conBuilder.build())

        val file = FileSpec.builder(pack, className)
                .addType(typeBuilder.build())
                .build()

        return file
    }

    fun writeFile(className: String, file: FileSpec) {
        val kaptKotlinGeneratedDir = processingEnv.options["kapt.kotlin.generated"]
        file.writeTo(File(kaptKotlinGeneratedDir, "$className.kt"))
    }
}

// asTypeName() currently returns java classes for certain classes such as String when we need the Kotlin
// version. As a workaround use the following from https://github.com/square/kotlinpoet/issues/236#issuecomment-377784099
fun TypeName.javaToKotlinType(): TypeName {
    return if (this is ParameterizedTypeName) {
        (rawType.javaToKotlinType() as ClassName).parameterizedBy(*typeArguments.map { it.javaToKotlinType() }.toTypedArray())
    } else {
        val className =
                JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(toString()))
                        ?.asSingleFqName()?.asString()

        return if (className == null) {
            this
        } else {
            ClassName.bestGuess(className)
        }
    }
}
