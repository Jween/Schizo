package io.jween.schizo.app;

import android.app.Application;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.jween.schizo.annotation.Api;
import io.jween.schizo.service.SchizoService;

/**
 * Created by Jwn on 2018/2/7.
 */

public class SchizoApplication extends Application {

    private static final String TAG = "Schizo";

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

        Log.i(TAG, "List Apis: ");
        for (Map.Entry pair : apiMethods.entrySet()) {
            Log.i(TAG, pair.getKey() + " = " + pair.getValue());
//            System.out.println(pair.getKey() + " = " + pair.getValue());
        }
        return apiMethods;
    }
}
