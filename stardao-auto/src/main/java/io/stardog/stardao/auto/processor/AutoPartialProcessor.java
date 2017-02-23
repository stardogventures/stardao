package io.stardog.stardao.auto.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.service.AutoService;
import com.google.auto.value.AutoValue;
import com.squareup.javapoet.*;
import io.stardog.stardao.auto.annotations.AutoPartial;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"io.stardog.stardao.auto.annotations.AutoPartial"})
public class AutoPartialProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Collection<? extends Element> annotatedElements =
                roundEnv.getElementsAnnotatedWith(AutoPartial.class);
        List<TypeElement> types = ElementFilter.typesIn(annotatedElements);
        for (TypeElement type : types) {
            try {
                processType(type);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }
        return false;
    }

    private void processType(TypeElement type) {
        AutoPartial autoValue = type.getAnnotation(AutoPartial.class);
        if (autoValue == null) {
            throw new ProcessorException("attempting to process @AutoPartial when no annotation present; probably compiler bug", type);
        }
        if (type.getKind() != ElementKind.CLASS) {
            throw new ProcessorException("@AutoPartial may only be used on classes", type);
        }

        String packageName = processingEnv.getElementUtils().getPackageOf(type).toString();
        String className = type.getSimpleName().toString();
        String generateClassName = "Partial" + className;

        List<MethodSpec> generatedMethods = new ArrayList<>();
        List<MethodSpec> builderMethods = new ArrayList<>();

        for (Element e : type.getEnclosedElements()) {
            if (isGetter(e)) {
                generatedMethods.add(toGetterMethodSpec(e));
                builderMethods.add(toBuilderMethodSpec(e));
                builderMethods.add(toBuilderOptMethodSpec(e));
            }
        }
        MethodSpec toBuilderMethod = MethodSpec.methodBuilder("toBuilder")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.bestGuess("Builder"))
                .build();
        MethodSpec builderMethod = MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.bestGuess(generateClassName + ".Builder"))
                .addStatement("return new AutoValue_" + generateClassName + ".Builder()")
                .build();
        MethodSpec innerBuildMethod = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.bestGuess(generateClassName))
                .build();
        TypeSpec builderClass = TypeSpec.classBuilder("Builder")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.STATIC)
                .addMethods(builderMethods)
                .addMethod(innerBuildMethod)
                .addAnnotation(AnnotationSpec.builder(AutoValue.Builder.class).build())
                .addAnnotation(AnnotationSpec.builder(JsonPOJOBuilder.class).addMember("withPrefix", "\"\"").build())
                .build();
        TypeSpec partialClass = TypeSpec.classBuilder(generateClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addMethods(generatedMethods)
                .addMethod(builderMethod)
                .addMethod(toBuilderMethod)
                .addType(builderClass)
                .addAnnotation(AnnotationSpec.builder(AutoValue.class).build())
                .addAnnotation(AnnotationSpec.builder(JsonInclude.class).addMember("value", "JsonInclude.Include.NON_ABSENT").build())
                .addAnnotation(AnnotationSpec.builder(JsonDeserialize.class).addMember("builder", "AutoValue_" + generateClassName + ".Builder.class").build())
                .build();
        JavaFile javaFile = JavaFile.builder(packageName, partialClass).build();
//        System.out.println(javaFile.toString());

        writeSourceFile(packageName + "." + generateClassName, javaFile.toString(), type);
    }

    protected boolean isGetter(Element method) {
        // must be a non-static method
        if (method.getKind() != ElementKind.METHOD) {
            return false;
        }
        if (method.getModifiers().contains(Modifier.STATIC)) {
            return false;
        }
        // must start with "get" or "is"
        String name = method.getSimpleName().toString();
        if (!name.startsWith("get") && !name.startsWith("is")) {
            return false;
        }
        // must have no parameters
        ExecutableType methodType = (ExecutableType)method.asType();
        if (methodType.getParameterTypes().size() > 0) {
            return false;
        }
        // ignore @JsonIgnore methods
        if (method.getAnnotation(JsonIgnore.class) != null) {
            return false;
        }
        return true;
    }

    protected TypeName getFieldType(Element getter) {
        ExecutableType methodType = (ExecutableType)getter.asType();
        TypeName fieldType = TypeName.get(methodType.getReturnType());

        // if using either java or guava optional, extract the contained type
        if (fieldType instanceof ParameterizedTypeName) {
            ParameterizedTypeName paramFieldType = (ParameterizedTypeName)fieldType;
            if (paramFieldType.rawType.toString().endsWith(".Optional")) {
                fieldType = paramFieldType.typeArguments.get(0);
            }
        }
        return fieldType;
    }

    protected MethodSpec toGetterMethodSpec(Element getter) {
        String name = getter.getSimpleName().toString();
        TypeName fieldType = getFieldType(getter);
        TypeName optReturnType = ParameterizedTypeName.get(ClassName.get(Optional.class), fieldType.box());
        List<? extends AnnotationMirror> annotationMirrors = getter.getAnnotationMirrors();
        List<AnnotationSpec> annotations = annotationMirrors.stream()
                .map(m -> AnnotationSpec.get(m))
                .collect(Collectors.toList());

        return MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(optReturnType)
                .addAnnotations(annotations)
                .build();
    }

    protected MethodSpec toBuilderMethodSpec(Element getter) {
        String name = getter.getSimpleName().toString();
        String fieldName = getterToFieldName(name);
        TypeName fieldType = getFieldType(getter);

        return MethodSpec.methodBuilder(fieldName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.bestGuess("Builder"))
                .addParameter(fieldType, fieldName)
                .build();
    }

    protected MethodSpec toBuilderOptMethodSpec(Element getter) {
        String name = getter.getSimpleName().toString();
        String fieldName = getterToFieldName(name);
        TypeName fieldType = getFieldType(getter);
        TypeName optFieldType = ParameterizedTypeName.get(ClassName.get(Optional.class), fieldType.box());

        return MethodSpec.methodBuilder(fieldName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.bestGuess("Builder"))
                .addParameter(optFieldType, fieldName)
                .build();
    }

    protected String getterToFieldName(String getterName) {
        if (getterName.startsWith("get")) {
            String field = getterName.substring(3);
            return field.substring(0, 1).toLowerCase() + field.substring(1);
        } else if (getterName.startsWith("is")) {
            String field = getterName.substring(2);
            return field.substring(0, 1).toLowerCase() + field.substring(1);
        } else {
            return null;
        }
    }

    private void writeSourceFile(String className, String text, TypeElement originatingType) {
        try {
            JavaFileObject sourceFile =
                    processingEnv.getFiler().createSourceFile(className, originatingType);
            Writer writer = sourceFile.openWriter();
            try {
                writer.write(text);
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            // This should really be an error, but we make it a warning in the hope of resisting Eclipse
            // bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=367599 (same as AutoValue code)
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "Could not write generated class " + className + ": " + e);
        }
    }
}
