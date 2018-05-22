package io.jween.schizo.sample.service;

import android.util.Log;

import io.jween.schizo.SchizoException;
import io.jween.schizo.SchizoResponse;
import io.jween.schizo.annotation.Action;
import io.jween.schizo.annotation.Api;
import io.jween.schizo.sample.constant.Actions;
import io.jween.schizo.sample.service.bean.Book;
import io.jween.schizo.sample.service.bean.Person;
import io.jween.schizo.service.SchizoService;

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

    @Api("noParameter")
    String getNothing() {
        return "Nothing!";
    }

    @Api("testException")
    String testException() throws Exception{
        Thread.sleep(10 * 1000);
        throw new SchizoException(SchizoResponse.CODE.ILLEGAL_ACCESS, "Test Exception from Remote [TestService]");
    }
}
