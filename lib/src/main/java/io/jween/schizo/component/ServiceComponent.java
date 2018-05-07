package io.jween.schizo.component;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import io.jween.schizo.ISchizoBridgeInterface;
import io.jween.schizo.SchizoException;
import io.jween.schizo.SchizoRequest;
import io.jween.schizo.SchizoResponse;
import io.jween.schizo.converter.StringConverter;
import io.jween.schizo.converter.gson.GsonConverterFactory;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
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
        this.context = context;
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


    public Single<ISchizoBridgeInterface> getInterface() {
        if (isBound()) {

            return Single.just(aidl);
        } else {
            final String packageName = context.getPackageName();
            return Single.create(new SingleOnSubscribe<ISchizoBridgeInterface>() {
                @Override
                public void subscribe(@NonNull final SingleEmitter<ISchizoBridgeInterface> singleEmitter) throws Exception {
                    synchronized (boundLock) {
                        Log.i(TAG, "binding service ...");
                        Intent intent = new Intent(getAction());
                        intent.setPackage(packageName);
                        serviceConnection = new ServiceConnection() {
                            @Override
                            public void onServiceConnected(ComponentName componentName, IBinder service) {
                                Log.i(TAG, "onServiceConnected");
                                aidl = bindAidlInterfaceOnServiceConnected(componentName, service);
                                Log.i(TAG, "onServiceConnected end");
                                singleEmitter.onSuccess(aidl);
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
                            singleEmitter.onError(new SchizoException("Schizo cannot bind service with action " + getAction()));
                        }
                    }
                }
            }).subscribeOn(Schedulers.io());
        }
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
                        return iSchizoBridgeInterface.single(schizoRequest);
                    }
                })
                .map(new Function<SchizoResponse, RES>() {
                    @Override
                    public RES apply(SchizoResponse schizoResponse) throws Exception {
                        StringConverter responseConverter = factory.stringConverter(responseType);
                        int responseCode = schizoResponse.getCode();
                        RES result = null;

                        if (responseCode == SchizoResponse.CODE.SUCCESS) {
                            result = (RES)responseConverter.fromString(schizoResponse.getBody());
                        } else {
                            throw new SchizoException("Schizo Exception Code " + schizoResponse.getCode());
                        }
                        return result;

                    }
                });
    }
}
