package com.meizu.flyme.schizo.sample.service;

import android.util.Log;

import com.meizu.flyme.schizo.annotation.Api;
import com.meizu.flyme.schizo.annotation.Action;
import com.meizu.flyme.schizo.sample.constant.Actions;
import com.meizu.flyme.schizo.sample.service.bean.Book;
import com.meizu.flyme.schizo.sample.service.bean.Person;
import com.meizu.flyme.schizo.service.SchizoService;

/**
 * Created by Jwn on 2018/1/16.
 */

@Action(Actions.TEST)
public class TestService extends SchizoService {

    @Api("person")
    Person getPerson(String name) {
        Log.i("SCHIZO", "api person accept request: name is " + name);
        return new Person("Hello", "Schizo");
    }

    @Api("book")
    Book getBook(String title) {
        return new Book(title, "Nobody");
    }


    @Api("book1")
    Book getBook(Person person) {
        Log.i("SCHIZO", "Person is [" + person.name + ",,," + person.surname + "]");
        return new Book(person.name, "Nobody");
    }
}
