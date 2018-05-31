/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.jween.io/licenses/APACHE-LICENSE-2.0.md
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jween.schizo.processor;

import android.content.Context;

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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import io.jween.schizo.annotation.Action;
import io.jween.schizo.annotation.Api;
import io.jween.schizo.component.ComponentManager;
import io.jween.schizo.processor.util.ElementUtil;

/**
 * Generates the client-end api class for the given SchizoService
 * annotated by {@linkplain Action} and it's server methods annotated by {@linkplain Api}.
 *
 * The class name it generates follows the ${ServiceName}Api pattern,
 * Server service name suffixed by Api.
 *
 * The methods it generates are consist of two parts.
 * <ul>
 * <li>Part I: method attach and method detach.</li>
 * <li>Part II: the api methods, from the methods annotated by {@linkplain Api}
 * from the custom SchizoService</li>
 * </ul>
 */
@SupportedAnnotationTypes({"io.jween.schizo.annotation.Action", "io.jween.schizo.annotation.Api"})
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


            // generate `void attach(Context context)` method
            MethodSpec attachMethod = MethodSpec.methodBuilder("attach")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(void.class)
                    .addParameter(Context.class, "context")
                    .addStatement("$T.attach(context, ACTION)", ComponentManager.class)
                    .build();
            apiClassBuilder.addMethod(attachMethod);

            // generate `void detach()` method
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
                DeclaredType declaredReturnType = (DeclaredType) returnArgTypeMirror;

                // indicates that the original server api method return type is Observable<NextType>
                TypeMirror nextType;    // get the SomeClass

                try {
                    // indicates the method returns an Observable<NextType>
                    nextType = declaredReturnType.getTypeArguments().get(0);
                } catch (Exception ep) {
                    // indicates the method returns a non observable result.
                    nextType = null;
                }

                TypeName returnArgTypeName = TypeName.get(returnArgTypeMirror);


                if (nextType != null ) { // build an Observable<?> client api method
                    MethodSpec.Builder apiMethodBuilder = MethodSpec.methodBuilder(apiString)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(returnArgTypeName/*TypeName.get(returnArgTypeMirror)*/);
                    List<? extends VariableElement> parameterElements = e.getParameters();


                    if (parameterElements.size() < 1) {

                        apiMethodBuilder.addStatement(
                                "return $T.get(ACTION).processObserver($S, $L, $T.class)",
                                ComponentManager.class, apiString, "null", nextType);
                    } else {
                        VariableElement requestParameterElement = parameterElements.get(0);
                        ParameterSpec requestParameterSpec = ParameterSpec.get(requestParameterElement);
                        apiMethodBuilder.addParameter(requestParameterSpec);

                        apiMethodBuilder.addStatement(
                                "return $T.get(ACTION).processObserver($S, $L, $T.class)",
                                ComponentManager.class, apiString, requestParameterSpec.name, nextType);
                    }

                    apiClassBuilder.addMethod(apiMethodBuilder.build());
                } else { // build a Single<?> client api method

                    ClassName singleClassName = ClassName.get("io.reactivex", "Single");
                    TypeName returnTypeName = ParameterizedTypeName.get(singleClassName, returnArgTypeName);


                    MethodSpec.Builder apiMethodBuilder = MethodSpec.methodBuilder(apiString)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(returnTypeName/*TypeName.get(returnTypeMirror)*/);

                    List<? extends VariableElement> parameterElements = e.getParameters();
                    if (parameterElements.size() < 1) {
                        apiMethodBuilder.addStatement(
                                "return $T.get(ACTION).process($S, $L, $T.class)",
                                ComponentManager.class, apiString, "null", returnArgTypeName);
                    } else {
                        VariableElement requestParameterElement = parameterElements.get(0);
                        ParameterSpec requestParameterSpec = ParameterSpec.get(requestParameterElement);
                        apiMethodBuilder.addParameter(requestParameterSpec);

                        apiMethodBuilder.addStatement(
                                "return $T.get(ACTION).process($S, $L, $T.class)",
                                ComponentManager.class, apiString, requestParameterSpec.name, returnArgTypeName);
                    }

                    apiClassBuilder.addMethod(apiMethodBuilder.build());
                }
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

    /**
     *  Get the api methods(service methods annotated with {@linkplain Api)
     *
     * @param elements the element utils from the processing environment.
     * @param type the given class type element, indicates to the sub-class of
     *              SchizoService annotated with {@linkplain Action}
     * @return the Api methods elements annotated with {@linkplain Api}
     * inside the given type
     */
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
