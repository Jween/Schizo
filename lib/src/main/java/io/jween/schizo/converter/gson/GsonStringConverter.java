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
