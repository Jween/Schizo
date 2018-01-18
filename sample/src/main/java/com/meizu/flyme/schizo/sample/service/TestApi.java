package com.meizu.flyme.schizo.sample.service;

import android.content.Context;
import android.support.design.widget.Snackbar;

import com.meizu.flyme.schizo.ISchizoBridgeInterface;
import com.meizu.flyme.schizo.SchizoException;
import com.meizu.flyme.schizo.SchizoRequest;
import com.meizu.flyme.schizo.SchizoResponse;
import com.meizu.flyme.schizo.component.ComponentManager;
import com.meizu.flyme.schizo.component.ServiceComponent;
import com.meizu.flyme.schizo.converter.StringConverter;
import com.meizu.flyme.schizo.sample.constant.Actions;
import com.meizu.flyme.schizo.sample.service.bean.Person;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

/**
 * Created by Jwn on 2018/1/18.
 */

public class TestApi {
    public static final String ACTION = Actions.TEST;

    public static void attach(Context context) {
        ComponentManager.attach(context, ACTION);
    }

    public static void detach() {
        ComponentManager.detach(ACTION);
    }

    public static Single<Person> person(final String para1) {
        return ComponentManager.get(ACTION).process(para1, Person.class);
    }

}
