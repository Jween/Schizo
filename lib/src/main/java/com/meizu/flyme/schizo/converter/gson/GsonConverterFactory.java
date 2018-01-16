package com.meizu.flyme.schizo.converter.gson;

import com.google.gson.Gson;
import com.meizu.flyme.schizo.converter.StringConverter;

import java.lang.reflect.Type;

/**
 * Created by Jwn on 2018/1/8.
 */

public class GsonConverterFactory extends StringConverter.Factory {

    private final Gson gson;

    public static GsonConverterFactory create() {
        return create(new Gson());
    }

    public static GsonConverterFactory create(Gson gson) {
        if(gson == null) {
            throw new NullPointerException("gson == null");
        } else {
            return new GsonConverterFactory(gson);
        }
    }

    private GsonConverterFactory(Gson gson) {
        this.gson = gson;
    }


//    @Override
//    public Converter<String, ?> outputConverter(Type type) {
//        TypeAdapter<?> adapter = this.gson.getAdapter(TypeToken.get(type));
//        return new GsonOutputConverter(this.gson, adapter);
//    }
//
//    @Override
//    public Converter<?, String> inputConverter(Type type) {
//        return new GsonInputConverter(this.gson, type);
//    }

    @Override
    public StringConverter<?> stringConverter(Type type) {
        return new GsonStringConverter<>(gson, type);
    }
}
