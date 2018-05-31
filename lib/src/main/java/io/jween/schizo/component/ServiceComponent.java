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

package io.jween.schizo.component;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.jween.schizo.ISchizoBridgeInterface;
import io.jween.schizo.SchizoCallback;
import io.jween.schizo.SchizoException;
import io.jween.schizo.SchizoRequest;
import io.jween.schizo.SchizoResponse;
import io.jween.schizo.converter.StringConverter;
import io.jween.schizo.converter.gson.GsonConverterFactory;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

/**
 * Created by Jwn on 2017/9/15.
 */

public class ServiceComponent implements Component{
    private static final String TAG = "ServiceComponent";

    private ISchizoBridgeInterface aidl;
    private ServiceConnection serviceConnection = null;
    private Context context;
    private String action;

    private final Object stateLock = new Object[0];

    enum BinderState {
        UNBOUND,
        BINDING,
        BOUND
    }

    private BehaviorSubject<BinderState> stateBehavior =
            BehaviorSubject.createDefault(BinderState.UNBOUND);

    private void changeState(BinderState state) {
        synchronized (stateLock) {
            stateBehavior.onNext(state);
        }
    }


    private Observable<BinderState> observeState() {
        synchronized (stateLock) {
            return stateBehavior.subscribeOn(Schedulers.io()).observeOn(Schedulers.io());
        }
    }


    public ServiceComponent(Context context, String action) {
        this.context = context;
        this.action = action;
    }

//    private Intent getServiceIntent() {
//        return new Intent(action);
//    }

    private String getAction() {
        return action;
    }


    /**
     * unbind from service
     */
    public void unbindService() {
        synchronized (stateLock) {
            if (this.context != null && serviceConnection != null) {
                Log.i(TAG, "unbinding service ...");
                this.context.unbindService(serviceConnection);
                serviceConnection = null;
                changeState(BinderState.UNBOUND);
            }
        }
    }

    private void bindService() throws SchizoException {
        changeState(BinderState.BINDING);

        final String packageName = context.getPackageName();
        Log.i(TAG, "binding service ...");
        Intent intent = new Intent(getAction());
        intent.setPackage(packageName);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                Log.i(TAG, "onServiceConnected");
                aidl = bindAidlInterfaceOnServiceConnected(componentName, service);
                Log.i(TAG, "onServiceConnected end");
                changeState(BinderState.BOUND);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.i(TAG, "onServiceDisconnected");
                aidl = null;
                changeState(BinderState.UNBOUND);
            }
        };

        boolean bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        if (!bound) {
            Log.e(TAG, "Error cant bind to service !");
            throw new SchizoException(SchizoResponse.CODE.IO_EXCEPTION, "Schizo cannot bind service with action " + getAction());
        }
    }

    public Single<ISchizoBridgeInterface> getInterface() {
        return observeState()
                .doOnNext(new Consumer<BinderState>() {
                    @Override
                    public void accept(BinderState binderState) throws Exception {
                        if (binderState == BinderState.UNBOUND) {
                            bindService();
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

    private ISchizoBridgeInterface bindAidlInterfaceOnServiceConnected(ComponentName componentName, IBinder service) {
        return ISchizoBridgeInterface.Stub.asInterface(service);
    }

    private StringConverter.Factory converterFactory;

    /* public */ void setConverterFactory(StringConverter.Factory factory) {
        this.converterFactory = factory;
    }

    public StringConverter.Factory getConverterFactory() {
        if (converterFactory == null) {
            converterFactory = defaultConverterFactory();
        }
        return converterFactory;
    }

    private static StringConverter.Factory defaultConverterFactory() {
        return GsonConverterFactory.create();
    }

    public final <REQ, RES> Single<RES> process(final String api, final REQ request, final Class<RES> responseType) {
        final StringConverter.Factory factory = getConverterFactory();
        return getInterface()
                .map(new Function<ISchizoBridgeInterface, SchizoResponse>() {
                    @Override
                    public SchizoResponse apply(ISchizoBridgeInterface iSchizoBridgeInterface) throws Exception {
                        SchizoRequest schizoRequest = new SchizoRequest(api);
                        if (request != null) {
                            StringConverter requestConverter = factory.stringConverter(request.getClass());
                            schizoRequest.setBody(requestConverter.toString(request));
                        }
                        Log.e(TAG, "single schizo request in component is " + schizoRequest.getApi() + "/" + schizoRequest.getBody());
                        SchizoResponse response = iSchizoBridgeInterface.single(schizoRequest);
                        Log.e(TAG, "single schizo response in component call is " + response.getCode() + "/" + response.getBody());
                        return response;
                    }
                })
                .map(new Function<SchizoResponse, RES>() {
                    @Override
                    public RES apply(SchizoResponse schizoResponse) throws Exception {
                        Log.e(TAG, "single schizo response in component map is " + schizoResponse.getCode() + "/" + schizoResponse.getBody());
                        StringConverter responseConverter = factory.stringConverter(responseType);
                        int responseCode = schizoResponse.getCode();
                        RES result = null;

                        if (responseCode == SchizoResponse.CODE.SUCCESS) {
                            result = (RES)responseConverter.fromString(schizoResponse.getBody());
                            return result;
                        } else {
                            SchizoException exception = SchizoException.fromSchizoErrorBody(schizoResponse.getBody());
                            throw exception;
                        }
                    }
                });
    }

    private class AidlObservableSource<T> implements ObservableSource<SchizoResponse> {
        String api;
        T request;
        ISchizoBridgeInterface aidl;
        StringConverter.Factory factory = getConverterFactory();
        AtomicBoolean freeResource = new AtomicBoolean(true);
        public AidlObservableSource(String api, T request ) {
            this.api = api;
            this.request = request;
        }

        public void setAidlInterface(ISchizoBridgeInterface aidl) {
            this.aidl = aidl;
            freeResource.set(false);
        }

        @Override
        public void subscribe(final Observer<? super SchizoResponse> observer) {
            SchizoRequest schizoRequest = new SchizoRequest(api);
            if (request != null) {
                StringConverter requestConverter = factory.stringConverter(request.getClass());
                try {
                    schizoRequest.setBody(requestConverter.toString(request));
                } catch (IOException e) {
                    e.printStackTrace();
                    observer.onError(e);
                }
            }
            try {
                SchizoCallback schizoCallback = new SchizoCallback.Stub() {

                    @Override
                    public void onNext(SchizoResponse cb) throws RemoteException {
                        Log.d(TAG, "get onNext " + cb.getCode() + "/" + cb.getBody());
                        if (freeResource.get()) {
                            aidl.dispose(this);
                            Log.d(TAG, "Stub free resource onNext.");
                        }
                        observer.onNext(cb);
                    }

                    @Override
                    public void onComplete() throws RemoteException {
                        if (freeResource.get()) {
                            aidl.dispose(this);
                            Log.d(TAG, "Stub free resource onComplete.");
                        }
                        observer.onComplete();
                    }

                    @Override
                    public void onError(SchizoException e) throws RemoteException {
                        Log.d(TAG, "Stub get onError ");
                        e.printStackTrace();
                        if (freeResource.get()) {
                            aidl.dispose(this);
                            Log.d(TAG, "Stub free resource onError ");
                        }
                        observer.onError(e);
                    }
                };
                aidl.observe(schizoRequest, schizoCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "observing error ");
                e.printStackTrace();
                observer.onError(e);
            }
        }

        public void freeResource() {
            freeResource.set(true);
        }
    }

    public final <REQ, NEXT> Observable<NEXT> processObserver(final String api, final REQ request, final Class<NEXT> responseType) {
        final StringConverter.Factory factory = getConverterFactory();
        final AidlObservableSource<REQ> source = new AidlObservableSource<>(api, request);
        return getInterface()
                .flatMapObservable(new Function<ISchizoBridgeInterface, ObservableSource<? extends SchizoResponse>>() {
                    @Override
                    public ObservableSource<? extends SchizoResponse> apply(
                            final ISchizoBridgeInterface iSchizoBridgeInterface) throws Exception {
                        source.setAidlInterface(iSchizoBridgeInterface);
                        return source;
                    }
                }).doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        source.freeResource();
                    }
                })
                .share()
                .map(new Function<SchizoResponse, NEXT>() {
                    @Override
                    public NEXT apply(SchizoResponse schizoResponse) throws Exception {
                        StringConverter responseConverter = factory.stringConverter(responseType);
                        int responseCode = schizoResponse.getCode();
                        NEXT result = null;

                        if (responseCode == SchizoResponse.CODE.ON_NEXT) {
                            result = (NEXT)responseConverter.fromString(schizoResponse.getBody());
                        } else {
                            SchizoException exception = SchizoException.fromSchizoErrorBody(schizoResponse.getBody());
                            throw exception;
                        }
                        return result;

                    }
                });
    }
}
