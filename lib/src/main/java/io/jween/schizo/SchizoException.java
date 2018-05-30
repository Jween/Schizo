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

package io.jween.schizo;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by Jwn on 2018/1/18.
 */

public class SchizoException extends RemoteException implements Parcelable {
    private int code;
    private String customMessage;

    public SchizoException() {

    }

    public SchizoException(int code, String message) {
        super(message);
        setCode(code);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String toSchizoErrorBody() {
        return code + ":" + getMessage();
    }

    public static SchizoException fromSchizoErrorBody(String errorBody) {
        Log.d("TAG", "errorBody is " + errorBody);
        String[] postSplit = errorBody.split(":", 2);
        return new SchizoException(Integer.valueOf(postSplit[0]), postSplit[1]);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String getMessage() {
        return TextUtils.isEmpty(customMessage) ? super.getMessage() : customMessage;
    }

    public void setMessage(String customMessage) {
        this.customMessage = customMessage;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.code);
        dest.writeString(getMessage());
    }

    protected SchizoException(Parcel in) {
        this.code = in.readInt();
        setMessage(in.readString());
    }

    public static final Parcelable.Creator<SchizoException> CREATOR = new Parcelable.Creator<SchizoException>() {
        @Override
        public SchizoException createFromParcel(Parcel source) {
            return new SchizoException(source);
        }

        @Override
        public SchizoException[] newArray(int size) {
            return new SchizoException[size];
        }
    };

    public void readFromParcel(Parcel in) {
        this.code = in.readInt();
        setMessage(in.readString());
    }
}
