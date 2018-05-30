// SchizoCallback.aidl
package io.jween.schizo;

// Declare any non-default types here with import statements
import io.jween.schizo.SchizoResponse;
import io.jween.schizo.SchizoException;

interface SchizoCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void onNext(inout SchizoResponse cb);
    void onComplete();
    void onError(inout SchizoException e);
}
