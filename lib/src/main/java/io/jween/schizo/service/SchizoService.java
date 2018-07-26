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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import io.jween.schizo.ISchizoBridgeInterface;
import io.jween.schizo.SchizoCallback;
import io.jween.schizo.SchizoException;
import io.jween.schizo.SchizoRequest;
import io.jween.schizo.SchizoResponse;
import io.jween.schizo.annotation.Api;
import io.jween.schizo.converter.StringConverter;
import io.jween.schizo.converter.gson.GsonConverterFactory;
import io.jween.schizo.exception.CallbackRemovedException;
import io.jween.schizo.util.SchizoExceptions;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

/**
 * Created by Jwn on 2017/12/29.
 */

public class SchizoService extends Service {
    private static final String TAG = "SchizoService";
    private static Map<String, Method> API_METHODS;

    private StringConverter.Factory converterFactory;
    RemoteCallbackMap<SchizoCallback> callbacks = new RemoteCallbackMap<>();
    private Hashtable<SchizoRequest, Disposable> pendingDisposables = new Hashtable<>();

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

        @Override
        public void observe(SchizoRequest request, SchizoCallback callback) throws RemoteException {
            dispatchRequestWithCallback(request, callback);
        }

        @Override
        public void dispose(SchizoCallback callback) throws RemoteException {
            callbacks.unregister(callback);
        }
    };


    private void dispatchRequestWithCallback(
            final SchizoRequest request, SchizoCallback callback) throws SchizoException{
        callbacks.register(callback, request);
        String api = request.getApi();
        String requestBody = request.getBody();

        Method method = getApiMethods().get(api);


        if (method != null) {
            Class<?>[] parameterTypes = method.getParameterTypes();
//            Class<?> returnType = method.getReturnType();
            Type returnType = method.getGenericReturnType();
            Type genericReturnType = ((ParameterizedType)returnType).getActualTypeArguments()[0];
            final StringConverter<?> responseConverter =
                    getConverterFactory().stringConverter(genericReturnType);

            if (parameterTypes != null) {
                method.setAccessible(true);
                final Observable<?> responseObserver;
                boolean hasParameters = parameterTypes.length > 0;
                if (hasParameters) {
                    Class<?> requestType = parameterTypes[0];
                    StringConverter<?> requestConverter = getConverterFactory().stringConverter(requestType);

                    try {
                        responseObserver = (Observable<?>)method.invoke(this, requestConverter.fromString(requestBody));
                    } catch (IllegalAccessException|InvocationTargetException|IOException e) {
                        e.printStackTrace();
                        try {
                            callback.onError(SchizoExceptions.from(e));
                        } catch (RemoteException shitHappened) {
                            shitHappened.printStackTrace();
                            throw SchizoExceptions.from(shitHappened);
                        }
                        return;
                    }
                } else {
                    try {
                        responseObserver = (Observable<?>)method.invoke(this);
                    } catch (IllegalAccessException|InvocationTargetException e) {
                        e.printStackTrace();
                        try {
                            callback.onError(SchizoExceptions.from(e));
                        } catch (RemoteException shitHappened) {
                            shitHappened.printStackTrace();
                            throw SchizoExceptions.from(shitHappened);
                        }
                        return;
                    }
                }

                final Disposable disposable =
                        responseObserver.share()
                                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object next) throws Exception {
                        SchizoResponse schizoResponse = new SchizoResponse();
                        String responseBody = responseConverter.toString(next);
                        schizoResponse.setBody(responseBody);
                        schizoResponse.setCode(SchizoResponse.CODE.ON_NEXT);
                        SchizoCallback schizoCallback = callbacks.getCallback(request);
                        if (schizoCallback != null) {
                            schizoCallback.onNext(schizoResponse);
                        } else {
                            // callback has been removed by the client.
                            // jump to Throwable Consumer to dispose this subscription,
                            throw new CallbackRemovedException();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if (throwable instanceof CallbackRemovedException) {
                            removePendingDisposable(request);
                            Log.w(TAG, "callback has been removed.");
                        } else {
                            throwable.printStackTrace();
                            SchizoCallback schizoCallback = callbacks.getCallback(request);
                            if (schizoCallback != null) {
                                schizoCallback.onError(SchizoExceptions.from(throwable));
                            } else {
                                removePendingDisposable(request);
                            }

                        }
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
//                        SchizoResponse schizoResponse = new SchizoResponse();
//                        schizoResponse.setCode(SchizoResponse.CODE.COMPLETE);
                        SchizoCallback schizoCallback = callbacks.getCallback(request);
                        if (schizoCallback != null) {
                            schizoCallback.onComplete();
                        } else {
                            removePendingDisposable(request);
                        }
                    }
                });
                addPendingDisposable(request, disposable);
            }
        }
    }

    void addPendingDisposable(SchizoRequest request, Disposable disposable) {
        pendingDisposables.put(request, disposable);
    }

    void removePendingDisposable(SchizoRequest request) {
        Disposable disposable = pendingDisposables.remove(request);
        if (disposable != null && !disposable.isDisposed() ) {
            disposable.dispose();
        }
    }

    void clearPendingDisposables() {
        Collection<Disposable> disposables = pendingDisposables.values();
        pendingDisposables.clear();
        for (Disposable d : disposables) {
            if (d != null && d.isDisposed()) {
                d.dispose();
            }
        }
    }

    SchizoResponse dispatchRequest(SchizoRequest request) throws SchizoException {
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

    @Override
    public void onDestroy() {
        clearPendingDisposables();
        super.onDestroy();
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
