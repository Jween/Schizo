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

package io.jween.schizo.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import io.jween.schizo.ISchizoBridgeInterface;
import io.jween.schizo.SchizoException;
import io.jween.schizo.SchizoRequest;
import io.jween.schizo.SchizoResponse;
import io.jween.schizo.annotation.Api;
import io.jween.schizo.converter.StringConverter;
import io.jween.schizo.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jwn on 2017/12/29.
 */

public class SchizoService extends Service {
    private static final String TAG = "Schizo";
    private static Map<String, Method> API_METHODS;

    private StringConverter.Factory converterFactory;

    /* public */ void setConverterFactory(StringConverter.Factory factory) {
        this.converterFactory = factory;
    }

    public StringConverter.Factory getConverterFactory() {
        if (converterFactory == null) {
            converterFactory = defaultConverterFactory();
        }
        return converterFactory;
    }

    private static StringConverter.Factory defaultConverterFactory() {
        return GsonConverterFactory.create();
    }

    private Map<String, Method> getApiMethods() {
        if (API_METHODS == null) {
            Log.v(TAG, "getApiMethods from " + getClass().getSimpleName());
            API_METHODS = getMethodsAnnotatedWithAPI(getClass());
        }
        return API_METHODS;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private ISchizoBridgeInterface.Stub binder = new ISchizoBridgeInterface.Stub() {
        @Override
        public SchizoResponse single(SchizoRequest request) throws RemoteException {
            return dispatchRequest(request);
        }

//        @Override
//        public void observe(SchizoRequest request, SchizoCallback callback) throws RemoteException {
//
//        }
    };

    SchizoResponse dispatchRequest(SchizoRequest request) {
        String api = request.getApi();
        String requestBody = request.getBody();

        SchizoResponse schizoResponse = new SchizoResponse();
        Method method = getApiMethods().get(api);

        if (method != null) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            Class<?> returnType = method.getReturnType();
            if (parameterTypes != null) {

                StringConverter<?> responseConverter = getConverterFactory().stringConverter(returnType);

                try {
                    method.setAccessible(true);
                    Object response;
                    boolean hasParameters = parameterTypes.length > 0;
                    if (hasParameters) {
                        Class<?> requestType = parameterTypes[0];
                        StringConverter<?> requestConverter = getConverterFactory().stringConverter(requestType);

                        response = method.invoke(this, requestConverter.fromString(requestBody));
                    } else {
                        response = method.invoke(this);
                    }
                    String responseBody = responseConverter.toString( response);
                    schizoResponse.setBody(responseBody);
                    schizoResponse.setCode(SchizoResponse.CODE.SUCCESS);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    int statusCode = SchizoResponse.CODE.ILLEGAL_ACCESS;
                    schizoResponse.setCode(statusCode);

                    SchizoException exception;
                    Throwable cause = e.getCause();
                    if (cause instanceof SchizoException) {
                        exception = (SchizoException) cause;
                    } else {
                        exception = new SchizoException(statusCode, cause.getMessage());
                    }
                    schizoResponse.setBody(exception.toSchizoErrorBody());

                } catch (IOException e) {
                    e.printStackTrace();
                    int statusCode = SchizoResponse.CODE.ILLEGAL_ACCESS;
                    schizoResponse.setCode(statusCode);

                    SchizoException exception;
                    Throwable cause = e.getCause();
                    if (cause instanceof SchizoException) {
                        exception = (SchizoException) cause;
                    } else {
                        exception = new SchizoException(statusCode, cause.getMessage());
                    }
                    schizoResponse.setBody(exception.toSchizoErrorBody());
                }
            }
        }
        return schizoResponse;
    }

    public static Map<String, Method> getMethodsAnnotatedWithAPI(final Class<?> type) {
//        final List<Method> methods = new ArrayList<Method>();
        final Map<String, Method> apiMethods = new HashMap<>();
        Class<?> klass = type;
        while (klass != SchizoService.class) { // need to iterated thought hierarchy in order to retrieve methods from above the current instance
            // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
            final List<Method> allMethods = new ArrayList<Method>(Arrays.asList(klass.getDeclaredMethods()));
            for (final Method method : allMethods) {
                if (method.isAnnotationPresent(Api.class)) {
                    Api annotInstance = method.getAnnotation(Api.class);
                    apiMethods.put(annotInstance.value(), method);
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }

        return apiMethods;
    }
}
