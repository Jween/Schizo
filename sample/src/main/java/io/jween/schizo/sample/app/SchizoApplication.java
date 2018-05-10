package io.jween.schizo.sample.app;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import io.jween.schizo.sample.service.TestServiceApi;

/**
 * Created by Jwn on 2017/8/30.
 */

public final class SchizoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == pid) {
                String currentProcName = processInfo.processName;
                if (!TextUtils.isEmpty(currentProcName) && currentProcName.equals(":background")) {
                    //Rest of the initializations are not needed for the background
                    //process
                    return;
                }
            }
        }

        /* Initializations for the UI process */

        TestServiceApi.attach(this);
    }
}
