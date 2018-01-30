package io.jween.schizo.component;

import android.content.Context;

import java.util.Hashtable;

/**
 * Created by Jwn on 2017/9/15.
 */

public class ComponentManager {

    private static Hashtable<String, ServiceComponent> singletonMap = new Hashtable<>();

    public static void attach(Context ctx, String action) {
        ServiceComponent comp = new ServiceComponent(ctx.getApplicationContext(), action);
        singletonMap.put(action, comp);
    }

    public static void detach(String action) {
        ServiceComponent component;
        component = singletonMap.remove(action);
        if (component != null) {
            component.unbindService();
        }
    }

    public static ServiceComponent get(String action) {
        return singletonMap.get(action);
    }
}
