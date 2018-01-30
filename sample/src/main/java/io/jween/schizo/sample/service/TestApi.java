package io.jween.schizo.sample.service;

import android.content.Context;

import io.jween.schizo.component.ComponentManager;
import io.jween.schizo.sample.constant.Actions;
import io.jween.schizo.sample.service.bean.Book;
import io.jween.schizo.sample.service.bean.Person;

import io.reactivex.Single;

/**
 * An example class for annotation generating
 * Do not touch.
 *
 * Created by Jwn on 2018/1/18.
 */
@Deprecated
/* api example class */ class TestApi {
    public static final String ACTION = Actions.TEST;

    public static void attach(Context context) {
        ComponentManager.attach(context, ACTION);
    }

    public static void detach() {
        ComponentManager.detach(ACTION);
    }

    public static Single<Person> person(String para1) {
        return ComponentManager.get(ACTION).process("person", para1, Person.class);
    }

    public static Single<Book> book(String title) {
        return ComponentManager.get(ACTION).process("book", title, Book.class);
    }
}
