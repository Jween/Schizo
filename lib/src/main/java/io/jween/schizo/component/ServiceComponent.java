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

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;

import io.jween.schizo.ISchizoBridgeInterface;
import io.jween.schizo.SchizoCallback;
import io.jween.schizo.SchizoException;
import io.jween.schizo.SchizoRequest;
import io.jween.schizo.SchizoResponse;
import io.jween.schizo.converter.StringConverter;
import io.jween.schizo.converter.gson.GsonConverterFactory;
import io.jween.schizo.service.SchizoBinder;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;

/**
 * Created by Jwn on 2017/9/15.
 */

public class ServiceComponent implements Component{
    private static final String TAG = "ServiceComponent";

    private SchizoBinder schizoBinder;

    public ServiceComponent(Context context, String action) {
        schizoBinder = new SchizoBinder(context, action);
    }

    /**
     * unbind from service
     */
    public void unbindService() {
        schizoBinder.changeStateToUnbound();
    }

    private void bindService() throws SchizoException {
        schizoBinder.changeStateToBinding();
    }

    public Single<ISchizoBridgeInterface> getInterface() {
        return schizoBinder.getInterface();
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

    public final <REQ, RES> Single<RES> process(final String api, final REQ request, final Type responseType) {

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
                        if (freeResource.get()) {
                            aidl.dispose(this);
                        }
                        observer.onNext(cb);
                    }

                    @Override
                    public void onComplete() throws RemoteException {
                        if (freeResource.get()) {
                            aidl.dispose(this);
                        }
                        observer.onComplete();
                    }

                    @Override
                    public void onError(SchizoException e) throws RemoteException {
                        e.printStackTrace();
                        if (freeResource.get()) {
                            aidl.dispose(this);
                        }
                        observer.onError(e);
                    }
                };
                aidl.observe(schizoRequest, schizoCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
                observer.onError(e);
            }
        }

        public void freeResource() {
            freeResource.set(true);
        }
    }

    public final <REQ, NEXT> Observable<NEXT> processObserver(final String api, final REQ request, final Type responseType) {
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
