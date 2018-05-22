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
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
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
                            SchizoException exception = SchizoException.fromSchizoErrorBody(schizoResponse.getBody());
                            throw exception;
                        }
                        return result;

                    }
                });
    }
}
