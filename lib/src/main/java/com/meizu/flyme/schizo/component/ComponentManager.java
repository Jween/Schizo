package com.meizu.flyme.schizo.component;

import android.content.Context;

import java.lang.reflect.Constructor;
import java.util.HashMap;

/**
 * Created by Jwn on 2017/9/15.
 */

public class ComponentManager {

    private static HashMap<String, ServiceComponent> singletonMap = new HashMap<>();
    private static final Object singletonLock = new Object[0];

    public static <T extends ServiceComponent> void attach(Context ctx, Class<T> cls) {
        try {
            Constructor<T> cons = cls.getConstructor(Context.class);
            T instance = cons.newInstance(ctx.getApplicationContext() );
            synchronized (singletonLock) {
                singletonMap.put(cls.getName(), instance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <T extends ServiceComponent> void detach(Class<T> cls) {
        ServiceComponent component;
        synchronized (singletonLock) {
            component = singletonMap.remove(cls.getName());
        }
        if (component != null) {
            component.unbindService();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends ServiceComponent> T get(Class<T> cls) {
        T component;
        synchronized (singletonLock) {
            component = (T) singletonMap.get(cls.getName());
        }
        return component;
    }
}
