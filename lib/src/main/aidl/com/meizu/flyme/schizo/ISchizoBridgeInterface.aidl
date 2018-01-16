// ISchizoBridgeInterface.aidl
package com.meizu.flyme.schizo;

import com.meizu.flyme.schizo.SchizoRequest;
import com.meizu.flyme.schizo.SchizoResponse;

interface ISchizoBridgeInterface {

    SchizoResponse single(in SchizoRequest request);

//    void observe(in SchizoRequest request, SchizoCallback callback);
}
