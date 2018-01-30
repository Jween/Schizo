// ISchizoBridgeInterface.aidl
package io.jween.schizo;

import io.jween.schizo.SchizoRequest;
import io.jween.schizo.SchizoResponse;

interface ISchizoBridgeInterface {

    SchizoResponse single(in SchizoRequest request);

//    void observe(in SchizoRequest request, SchizoCallback callback);
}
