package com.meizu.flyme.schizo.sample.service;

import android.util.Log;

import com.meizu.flyme.schizo.annotation.API;
import com.meizu.flyme.schizo.sample.service.bean.Person;
import com.meizu.flyme.schizo.service.SchizoService;

/**
 * Created by Jwn on 2018/1/16.
 */

public class TestService extends SchizoService {

    @API("person")
    Person getPerson(String name) {
        Log.e("SCHIZO", "api person accept request: name is " + name);
        return new Person("Hello", "Schizo");
    }
}
