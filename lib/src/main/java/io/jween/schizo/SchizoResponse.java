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

/**
 * Created by Jwn on 2017/12/29.
 */

public class SchizoResponse implements Parcelable {

    public interface CODE {
        int ERROR = 404;
        int SUCCESS = 200;
        int ON_NEXT = 100;
        int COMPLETE = 200;

        int IO_EXCEPTION = 401;
        int ILLEGAL_ACCESS = 402;

    }

    private int code = CODE.ERROR;
    private String body;

    public SchizoResponse() {

    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.code);
        dest.writeString(this.body);
    }

    protected SchizoResponse(Parcel in) {
        readFromParcel(in);
    }

    public static final Parcelable.Creator<SchizoResponse> CREATOR = new Parcelable.Creator<SchizoResponse>() {
        @Override
        public SchizoResponse createFromParcel(Parcel source) {
            return new SchizoResponse(source);
        }

        @Override
        public SchizoResponse[] newArray(int size) {
            return new SchizoResponse[size];
        }
    };

    public void readFromParcel(Parcel in) {
        this.code = in.readInt();
        this.body = in.readString();
    }
}
