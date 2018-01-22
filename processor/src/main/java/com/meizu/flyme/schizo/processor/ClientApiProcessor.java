package com.meizu.flyme.schizo.processor;

import com.meizu.flyme.schizo.annotation.Action;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes({"com.meizu.flyme.schizo.annotation.Action", "com.meizu.flyme.schizo.annotation.Api"})
public class ClientApiProcessor extends AbstractProcessor{
    private Messager messager;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        messager.printMessage(Diagnostic.Kind.WARNING, "ClientApiProcessor is processing.");
        for (Element element : roundEnvironment.getElementsAnnotatedWith(Action.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.");
                return true;
            }
            Action actionAnnotation = element.getAnnotation(Action.class);
            String action = actionAnnotation.value();

            TypeElement typeElement = (TypeElement) element;
            String serviceClassName = typeElement.getSimpleName().toString();

            Elements elements = processingEnv.getElementUtils();
            String servicePackageName = elements.getPackageOf(typeElement).getQualifiedName().toString();

            ClassName.get(typeElement).simpleName();
            ClassName.get(typeElement).packageName();


            // build class
            TypeSpec.Builder apiClass = TypeSpec
                    .classBuilder(serviceClassName +"Api")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL);


            try {
                JavaFile.builder(servicePackageName, apiClass.build())
                        .build()
                        .writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
