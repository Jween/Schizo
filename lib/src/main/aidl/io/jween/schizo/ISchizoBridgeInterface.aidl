// ISchizoBridgeInterface.aidl
package io.jween.schizo;

import io.jween.schizo.SchizoRequest;
import io.jween.schizo.SchizoResponse;
import io.jween.schizo.SchizoCallback;

interface ISchizoBridgeInterface {

    SchizoResponse single(in SchizoRequest request);

    void observe(in SchizoRequest request, in SchizoCallback callback);
    void dispose(in SchizoCallback callback);
}
