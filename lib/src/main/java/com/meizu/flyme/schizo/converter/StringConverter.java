package com.meizu.flyme.schizo.converter;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Created by Jwn on 2018/1/16.
 */

public interface StringConverter<T> {
    String toString(Object input) throws IOException;
    T fromString(String str) throws IOException;

    public abstract static class Factory {
        public Factory() {

        }

        public abstract StringConverter<?> stringConverter(Type type);
    }
}
