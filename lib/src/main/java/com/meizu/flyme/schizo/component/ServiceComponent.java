package com.meizu.flyme.schizo.component;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.meizu.flyme.schizo.ISchizoBridgeInterface;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Jwn on 2017/9/15.
 */

public class ServiceComponent implements Component{
    private static final String TAG = "ServiceComponent";

    private ISchizoBridgeInterface aidl;
    private ServiceConnection serviceConnection = null;
    private Context context = null;
    private String action;
    private boolean bound = false;
    private final Object boundLock = new Object[0];


    public ServiceComponent(Context context, String action) {
        this.context = context.getApplicationContext();
        this.action = action;
    }

//    private Intent getServiceIntent() {
//        return new Intent(action);
//    }

    private String getAction() {
        return action;
    }

    private boolean isBound() {
        synchronized (boundLock) {
            return bound;
        }
    }

    /**
     * unbind from service
     */
    public void unbindService() {
        synchronized (boundLock) {
            if (this.context != null && serviceConnection != null) {
                Log.i(TAG, "unbinding service ...");
                this.context.unbindService(serviceConnection);
                serviceConnection = null;
                bound = false;
            }
        }
    }


    public Observable<ISchizoBridgeInterface> getInterface() {
        if (isBound()) {
            return Observable.just(aidl);
        } else {
            return Observable.create(new ObservableOnSubscribe<ISchizoBridgeInterface>() {
                @Override
                public void subscribe(@NonNull final ObservableEmitter<ISchizoBridgeInterface> observableEmitter) throws Exception {
                    synchronized (boundLock) {
                        Log.i(TAG, "binding service ...");
                        Intent intent = new Intent(action);
                        intent.setPackage("com.meizu.flyme.schizo.sample");
                        serviceConnection = new ServiceConnection() {
                            @Override
                            public void onServiceConnected(ComponentName componentName, IBinder service) {
                                Log.i(TAG, "onServiceConnected");
                                aidl = bindAidlInterfaceOnServiceConnected(componentName, service);
                                Log.i(TAG, "onServiceConnected end");
                                observableEmitter.onNext(aidl);
                                observableEmitter.onComplete();
                            }

                            @Override
                            public void onServiceDisconnected(ComponentName componentName) {
                                Log.i(TAG, "onServiceDisconnected");
                                aidl = null;
                                synchronized (boundLock) {
                                    bound = false;
                                }
                            }
                        };

                        bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

                        if (!bound) {
                            Log.e(TAG, "Error cant bind to service !");
                            observableEmitter.onComplete();
                        }
                    }
                }
            }).subscribeOn(Schedulers.io());
        }
    }

    private ISchizoBridgeInterface bindAidlInterfaceOnServiceConnected(ComponentName componentName, IBinder service) {
        return ISchizoBridgeInterface.Stub.asInterface(service);
    }
}
