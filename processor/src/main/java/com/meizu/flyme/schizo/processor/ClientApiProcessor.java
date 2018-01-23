package com.meizu.flyme.schizo.processor;

import android.content.Context;

import com.meizu.flyme.schizo.annotation.Action;
import com.meizu.flyme.schizo.annotation.Api;
import com.meizu.flyme.schizo.component.ComponentManager;
import com.meizu.flyme.schizo.processor.util.ElementUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashSet;
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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;


@SupportedAnnotationTypes({"com.meizu.flyme.schizo.annotation.Action", "com.meizu.flyme.schizo.annotation.Api"})
public class ClientApiProcessor extends AbstractProcessor{
    private Messager messager;
    private Filer filer;
    private Types typeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
        typeUtils = processingEnvironment.getTypeUtils();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(Action.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied to class.");
                return true;
            }
            Action actionAnnotation = element.getAnnotation(Action.class);
            String actionValue = actionAnnotation.value();

            TypeElement typeElement = (TypeElement) element;
            String serviceClassName = typeElement.getSimpleName().toString();

            Elements elements = processingEnv.getElementUtils();
            String servicePackageName = elements.getPackageOf(typeElement).getQualifiedName().toString();

            ClassName.get(typeElement).simpleName();
            ClassName.get(typeElement).packageName();


            // build class
            TypeSpec.Builder apiClassBuilder = TypeSpec
                    .classBuilder(serviceClassName +"Api")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

            // private static final String ACTION = "$targetAction"
            FieldSpec actionField = FieldSpec.builder(String.class, "ACTION")
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", actionValue)
                    .build();
            apiClassBuilder.addField(actionField);


            // generate attach method
            MethodSpec attachMethod = MethodSpec.methodBuilder("attach")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(void.class)
                    .addParameter(Context.class, "context")
                    .addStatement("$T.attach(context, ACTION)", ComponentManager.class)
                    .build();
            apiClassBuilder.addMethod(attachMethod);

            // generate detach method
            MethodSpec detachMethod = MethodSpec.methodBuilder("detach")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(void.class)
                    .addStatement("$T.detach(ACTION)", ComponentManager.class)
                    .build();
            apiClassBuilder.addMethod(detachMethod);

            // generate client api methods based on the service method annotated with Api.class
            Set<ExecutableElement> methodElements = getApiElements(elements, typeElement);
            for (ExecutableElement e : methodElements) {
                messager.printMessage(Diagnostic.Kind.WARNING, e.toString());

                Api apiAnnotation = e.getAnnotation(Api.class);
                String apiString = apiAnnotation.value();
                TypeMirror returnArgTypeMirror = e.getReturnType();

                TypeName argTypeName = TypeName.get(returnArgTypeMirror);
                ClassName singleClassName = ClassName.get("io.reactivex", "Single");
                TypeName returnTypeName = ParameterizedTypeName.get(singleClassName, argTypeName);


                MethodSpec.Builder apiMethodBuilder = MethodSpec.methodBuilder(apiString)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(returnTypeName/*TypeName.get(returnTypeMirror)*/);

                List<? extends VariableElement> parameterElements = e.getParameters();
                if (parameterElements.size() < 1) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Method " + e.getSimpleName() + " missing request parameter");
                }
                VariableElement requestParameterElement = parameterElements.get(0);
                ParameterSpec requestParameterSpec = ParameterSpec.get(requestParameterElement);
                apiMethodBuilder.addParameter( requestParameterSpec);

                apiMethodBuilder.addStatement(
                        "return $T.get(ACTION).process($S, $S, $T.class)",
                        ComponentManager.class, apiString, requestParameterSpec.name, argTypeName);

                apiClassBuilder.addMethod(apiMethodBuilder.build());
            }

            // write to file
            try {
                JavaFile.builder(servicePackageName, apiClassBuilder.build())
                        .build()
                        .writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private static Set<ExecutableElement> getApiElements(Elements elements, TypeElement type) {
        Set<ExecutableElement> found = new HashSet<>();

        for (Element e : ElementUtil.getAnnotatedElements(elements, type, Api.class)) {
            if (e.getKind() == ElementKind.METHOD) {
                found.add((ExecutableElement)e);
            }
        }
        return found;
    }
}
