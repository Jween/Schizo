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

import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.ArrayMap;


public class RemoteCallbackMap<E extends IInterface> {
    private static final String TAG = "SchizoCallbackList";

    private final ArrayMap<IBinder, DeathCallback> callbacks = new ArrayMap<>();
    private final ArrayMap<Object, IBinder> cookies = new ArrayMap<>();

    private boolean mKilled = false;

    private final class DeathCallback implements IBinder.DeathRecipient {
        final E callback;
        final Object cookie;

        DeathCallback(E callback, Object cookie) {
            this.callback = callback;
            this.cookie = cookie;
        }

        public void binderDied() {
            synchronized (callbacks) {
                callbacks.remove(callback.asBinder());
                cookies.remove(cookie);
            }
            onCallbackDied(callback, cookie);
        }
    }

    public boolean register(E callback, Object cookie) {
        synchronized (callbacks) {
            if (mKilled) {
                return false;
            }

            IBinder binder = callback.asBinder();
            try {
                DeathCallback cb = new DeathCallback(callback, cookie);
                binder.linkToDeath(cb, 0);
                callbacks.put(binder, cb);
                cookies.put(cookie, binder);
                return true;
            } catch (RemoteException e) {
                return false;
            }
        }
    }

    public E getCallback(Object cookie) {
        synchronized (callbacks) {
            if (mKilled) {
                return null;
            }


            IBinder binder =cookies.get(cookie);
            if (binder != null) {
                DeathCallback cb = callbacks.get(binder);
                if (cb != null) {
                    return cb.callback;
                }
            }

            return null;
        }
    }

    public boolean isRegistered(Object cookie) {
        return getCallback(cookie) != null;
    }

    /**
     * Remove from the list a callback that was previously added with
     * {@link #register}.  This uses the
     * {@link IInterface#asBinder callback.asBinder()} object to correctly
     * find the previous registration.
     * Registrations are not counted; a single unregister call will remove
     * a callback after any number calls to {@link #register} for it.
     *
     * @param callback The callback to be removed from the list.  Passing
     * null here will cause a NullPointerException, so you will generally want
     * to check for null before calling.
     *
     * @return Returns true if the callback was found and unregistered.  Returns
     * false if the given callback was not found on the list.
     *
     * @see #register
     */
    public boolean unregister(E callback) {
        synchronized (callbacks) {
            DeathCallback cb = callbacks.remove(callback.asBinder());
            if (cb != null) {
                cookies.remove(cb.cookie);
                cb.callback.asBinder().unlinkToDeath(cb, 0);
                return true;
            }
            return false;
        }
    }

    /**
     * Disable this callback list.  All registered callbacks are unregistered,
     * and the list is disabled so that future calls to {@link #register} will
     * fail.  This should be used when a Service is stopping, to prevent clients
     * from registering callbacks after it is stopped.
     *
     * @see #register
     */
    public void kill() {
        synchronized (callbacks) {
            for (int cbi = callbacks.size() - 1; cbi >= 0; cbi--) {
                DeathCallback cb = callbacks.valueAt(cbi);
                cb.callback.asBinder().unlinkToDeath(cb, 0);
            }
            callbacks.clear();
            cookies.clear();
            mKilled = true;
        }
    }


    public void onCallbackDied(E callback, Object cookie) {

    }

    public int getRegisteredCallbackCount() {
        synchronized (callbacks) {
            if (mKilled) {
                return 0;
            }
            return callbacks.size();
        }
    }
}

