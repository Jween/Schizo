package io.jween.schizo.sample;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import java.util.List;
import java.util.Optional;

import io.jween.schizo.sample.service.TestServiceApi;
import io.jween.schizo.sample.service.bean.Book;
import io.jween.schizo.sample.service.bean.Person;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {
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
                append("\n" + throwable.toString());
                throwable.printStackTrace();
            }
        };
    }

    public void onApiPersonClicked(View v) {
        append("\nRequest: person/String(name=hi)");
        cd.add(TestServiceApi.person("hi")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Person>() {
                    @Override
                    public void accept(Person person) throws Exception {
                        append("\nResponse: Person[" + person.name + " " + person.surname + "]");
                    }
                }, eatException)
        );
    }

    public void onApiBookClicked(View v) {
        append("\nRequest: book/String(title=logic)");
        cd.add(TestServiceApi.book("logic")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Book>() {
                    @Override
                    public void accept(Book book){
                        append("\nResponse: Book[" + book.getTitle() + " " + book.getAuthor() + "]");
                    }
                }, eatException)
        );
    }

    public void onApiObserveNumberClicked(View v) {
        append("\nRequest: observeNumber/");
        Disposable d = TestServiceApi.observeNumber()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Optional<Long>>() {
                    @Override
                    public void accept(Optional<Long> aLong) throws Exception {
                        if (aLong.isPresent()) {
                            append("\nnumber changed to " + aLong.get());
                        } else {
                            append("\nnumber removed!");
                        }
                    }
                }, eatException);
        cd.add(d);
    }

    public void onApiBook1Clicked(View v) {
        append("\nRequest: book/Person(name:Maogan,surname:Tao)");
        cd.add(TestServiceApi.book1(new Person("Maogan", "Tao"))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Book>() {
                    @Override
                    public void accept(Book book) throws Exception {
                        append("\nResponse: Person -> Book[" + book.getTitle() + " " + book.getAuthor() + "]");
                    }
                }, eatException)
        );
    }

    public void onApiBookListClicked(View v) {
        append("\nRequest: bookList/author(Foo)  // get books written by Foo");
        Disposable d = TestServiceApi.bookList("Foo")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Book>>() {
                    @Override
                    public void accept(List<Book> books) throws Exception {
                        append("\nReponse: of bookList/author(Foo)");
                        for (Book book : books) {
                            append("\n    " + book.getTitle() + "/" + book.getAuthor());
                        }
                    }
                }, eatException);

        cd.add(d);
    }

    public void onApiNoParameterClicked(View v) {
        append("\nRequest: noParameter/");
        cd.add(TestServiceApi.noParameter()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        append("\nResponse: noParameter -> " + s);
                    }
                }, eatException)
        );
    }

    public void onApiTestExceptionClicked(View v) {
        append("\nRequest: testException/");
        cd.add(TestServiceApi.testException()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        append("\nResponse: testException -> " + s);
                    }
                }, eatException)
        );
    }

    Disposable disposable;
    public void onApiObserveCounterClicked(View v) {
        append("\nRequest: observeCounter/interval(3)");
        if(disposable != null && !disposable.isDisposed()) {
            append("\nRequest: dispose last request.");
            disposable.dispose();
        }
        disposable = TestServiceApi.observeCounter(3)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        append("\nOnNext(3):  -> " + s);
                    }
                }, eatException);
        cd.add(disposable);
    }

    public void onApiDisposeCounterClicked(View v) {
        append("\nDispose: observeCounter/interval(3)");
        if(disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }


    /**
     * append text to console and scroll to bottom.
     * @param text
     */
    private void append(String text) {
        final int maxTextLength = 5000;

        CharSequence currentText = console.getText();
        int currentTextLength = currentText.length();
        int appendixLength = text.length();
        if (currentTextLength > maxTextLength) {
            if (currentTextLength > appendixLength) {
                console.setText(currentText.subSequence(appendixLength - 1, currentTextLength) );
                console.append(text);
            } else {
                console.setText(text);
                console.append(""); // append makes TextView scroll to bottom
            }
        } else {
            console.append(text);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cd.clear();
    }
}
