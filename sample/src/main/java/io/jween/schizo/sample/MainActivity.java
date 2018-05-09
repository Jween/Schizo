package io.jween.schizo.sample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import io.jween.schizo.sample.service.TestServiceApi;
import io.jween.schizo.sample.service.bean.Book;
import io.jween.schizo.sample.service.bean.Person;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {
    int counter = 0;
    CompositeDisposable cd = new CompositeDisposable();

    FloatingActionButton fab;
    Consumer<Throwable> eatException;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TestServiceApi.attach(this);
        fab = findViewById(R.id.fab);
        eatException = new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Snackbar.make(fab, throwable.toString(), Snackbar.LENGTH_LONG).show();
            }
        };

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                int ret = counter++;
                if (ret % 4 == 0) {
                    cd.add(TestServiceApi.person("hi")
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Person>() {
                                @Override
                                public void accept(Person person) throws Exception {
                                    Snackbar.make(view,
                                            "response: Person[" + person.name + " " + person.surname + "]", Snackbar.LENGTH_LONG)
                                            .setAction("Action", null).show();
                                }
                            }, eatException)
                    );
                } else if(ret % 4 == 1) {

                    cd.add(TestServiceApi.book("logic")
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Book>() {
                                @Override
                                public void accept(Book book){
                                    Snackbar.make(view,
                                            "response: Book[" + book.getTitle() + " " + book.getAuthor() + "]", Snackbar.LENGTH_LONG)
                                            .setAction("Action", null).show();
                                }
                            }, eatException)
                    );
                } else if(ret % 4 == 2) {
                    cd.add(TestServiceApi.book1(new Person("Maogan", "Tao"))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Book>() {
                                @Override
                                public void accept(Book book) throws Exception {
                                    Snackbar.make(view,
                                            "response: Person -> Book[" + book.getTitle() + " " + book.getAuthor() + "]", Snackbar.LENGTH_LONG)
                                            .setAction("Action", null).show();
                                }
                            }, eatException)
                    );
                } else {
                    cd.add(TestServiceApi.noParameter()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<String>() {
                                @Override
                                public void accept(String s) throws Exception {
                                    Snackbar.make(view,
                                            "response: noParameters -> " + s, Snackbar.LENGTH_LONG)
                                            .setAction("Action", null).show();
                                }
                            }, eatException)
                    );
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            cd.add(TestServiceApi.testException()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) throws Exception {
                            Snackbar.make(fab,
                                    "response: testException -> " + s, Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    }, eatException)
            );
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TestServiceApi.detach();
        cd.clear();
    }
}
