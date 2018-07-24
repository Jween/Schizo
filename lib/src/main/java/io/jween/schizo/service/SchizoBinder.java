/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.jween.io/licenses/APACHE-LICENSE-2.0.md
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jween.schizo.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import io.jween.schizo.ISchizoBridgeInterface;
import io.jween.schizo.SchizoException;
import io.jween.schizo.SchizoResponse;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

/**
 * a reactive binder using rxjava.
 */
public class SchizoBinder {
    private static final String TAG = "SchizoBinder";
    private ISchizoBridgeInterface aidl;
    private ServiceConnection serviceConnection = null;
    private Context context;
    private String action;

    private final Object stateLock = new Object[0];

    public SchizoBinder(Context context, String action) {
        this.context = context;
        this.action = action;
    }

    private BehaviorSubject<BinderState> stateBehavior =
            BehaviorSubject.createDefault(BinderState.UNBOUND);

    public void changeStateToBinding() throws SchizoException {
        synchronized (stateLock) {
            if (BinderState.BINDING == stateBehavior.getValue() ) {
                return;
            }

            stateBehavior.onNext(BinderState.BINDING);
            this.serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder service) {
                    Log.i(TAG, "onServiceConnected");
                    changeStateToBound(ISchizoBridgeInterface.Stub.asInterface(service) );
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    Log.i(TAG, "onServiceDisconnected");
                    changeStateToUnbound();
                }
            };;
            final String packageName = context.getPackageName();
            Log.i(TAG, "binding service ...");
            Intent intent = new Intent(action);
            intent.setPackage(packageName);
            boolean bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

            if (!bound) {
                Log.e(TAG, "Error cant bind to service !");
                throw new SchizoException(SchizoResponse.CODE.IO_EXCEPTION, "Schizo cannot bind service with action " + action);
            }
        }
    }

    public void changeStateToBound(ISchizoBridgeInterface aidl) {
        synchronized (stateLock) {
            if (BinderState.BOUND == stateBehavior.getValue() ) {
                return;
            }
            this.aidl = aidl;
            stateBehavior.onNext(BinderState.BOUND);
        }
    }

    public void changeStateToUnbound() {
        synchronized (stateLock) {
            if (BinderState.UNBOUND == stateBehavior.getValue() ) {
                return;
            }

            stateBehavior.onNext(BinderState.UNBOUND);
            Log.i(TAG, "unbinding service ...");
            this.context.unbindService(serviceConnection);
            this.aidl = null;
            serviceConnection = null;
        }
    }

    public Observable<BinderState> observeState() {
        synchronized (stateLock) {
            return stateBehavior.subscribeOn(Schedulers.io()).observeOn(Schedulers.io());
        }
    }

    enum BinderState {
        UNBOUND,
        BINDING,
        BOUND
    }


    public Single<ISchizoBridgeInterface> getInterface() {
        return observeState()
                .doOnNext(new Consumer<BinderState>() {
                    @Override
                    public void accept(BinderState binderState) throws Exception {
                        if (binderState == BinderState.UNBOUND) {
                            changeStateToBinding();
                        }
                    }
                })
                .filter(new Predicate<BinderState>() {
                    @Override
                    public boolean test(BinderState binderState) throws Exception {
                        return binderState == BinderState.BOUND;
                    }
                })
                .firstOrError()
                .flatMap(new Function<BinderState, SingleSource<ISchizoBridgeInterface>>() {
                    @Override
                    public SingleSource<ISchizoBridgeInterface> apply(BinderState binderState) throws Exception {
                        return Single.just(aidl);
                    }
                }).observeOn(Schedulers.io());
    }
}
