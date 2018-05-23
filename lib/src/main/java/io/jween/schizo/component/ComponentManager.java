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
