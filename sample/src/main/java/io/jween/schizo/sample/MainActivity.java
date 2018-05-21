package io.jween.schizo.sample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import io.jween.schizo.sample.service.TestServiceApi;
import io.jween.schizo.sample.service.bean.Book;
import io.jween.schizo.sample.service.bean.Person;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {
    int counter = 0;
    CompositeDisposable cd = new CompositeDisposable();

    Consumer<Throwable> eatException;
    TextView console;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        console = findViewById(R.id.console);
        console.setMovementMethod(new ScrollingMovementMethod());
        eatException = new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Snackbar.make(console, throwable.toString(), Snackbar.LENGTH_LONG).show();
                console.append("\n" + throwable.toString());
            }
        };
    }

    public void onApiPersonClicked(View v) {
        console.append("\nRequest: person/String(name=hi)");
        cd.add(TestServiceApi.person("hi")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Person>() {
                    @Override
                    public void accept(Person person) throws Exception {
                        console.append("\nResponse: Person[" + person.name + " " + person.surname + "]");
                    }
                }, eatException)
        );
    }

    public void onApiBookClicked(View v) {
        console.append("\nRequest: book/String(title=logic)");
        cd.add(TestServiceApi.book("logic")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Book>() {
                    @Override
                    public void accept(Book book){
                        console.append("\nResponse: Book[" + book.getTitle() + " " + book.getAuthor() + "]");
                    }
                }, eatException)
        );
    }
    public void onApiBook1Clicked(View v) {
        console.append("\nRequest: book/Person(name:Maogan,surname:Tao)");
        cd.add(TestServiceApi.book1(new Person("Maogan", "Tao"))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Book>() {
                    @Override
                    public void accept(Book book) throws Exception {
                        console.append("\nResponse: Person -> Book[" + book.getTitle() + " " + book.getAuthor() + "]");
                    }
                }, eatException)
        );
    }

    public void onApiNoParameterClicked(View v) {
        console.append("\nRequest: noParameter/");
        cd.add(TestServiceApi.noParameter()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        console.append("\nResponse: noParameter -> " + s);
                    }
                }, eatException)
        );
    }

    public void onApiTestExceptionClicked(View v) {
        console.append("\nRequest: testException/");
        cd.add(TestServiceApi.testException()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        console.append("\nResponse: testException -> " + s);
                    }
                }, eatException)
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cd.clear();
    }
}
