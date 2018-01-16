package com.meizu.flyme.schizo.sample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.meizu.flyme.schizo.ISchizoBridgeInterface;
import com.meizu.flyme.schizo.SchizoRequest;
import com.meizu.flyme.schizo.SchizoResponse;
import com.meizu.flyme.schizo.component.ServiceComponent;
import com.meizu.flyme.schizo.sample.constant.Actions;

import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ServiceComponent sc = new ServiceComponent(this, Actions.TEST);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();

                sc.getInterface()
                        .map(new Function<ISchizoBridgeInterface, SchizoResponse>() {
                            @Override
                            public SchizoResponse apply(ISchizoBridgeInterface iSchizoBridgeInterface) throws Exception {
                                SchizoRequest request = new SchizoRequest("person");
                                request.setBody("hi");
                                return iSchizoBridgeInterface.single(request);
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<SchizoResponse>() {
                            @Override
                            public void accept(SchizoResponse schizoResponse) throws Exception {
                                Snackbar.make(view, "response: " + schizoResponse.getBody(), Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        });
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
