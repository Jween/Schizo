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

package io.jween.schizo.converter.gson;

import com.google.gson.Gson;
import io.jween.schizo.converter.StringConverter;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Created by Jwn on 2018/1/16.
 */
public class GsonStringConverter<T> implements StringConverter<T> {
    private final Gson gson;
    private final Type type;

    GsonStringConverter(Gson gson, Type type) {
        this.gson = gson;
        this.type = type;
    }

    @Override
    public String toString(Object input) throws IOException {
        if (type == String.class) {
            return (String)input;
        }
        return gson.toJson(input);
    }

    @Override
    public T fromString(String str) throws IOException {
        if (type == String.class) {
            return (T)str;
        }
        return gson.fromJson(str, type);
    }
}
