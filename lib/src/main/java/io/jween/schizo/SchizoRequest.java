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

public class SchizoRequest implements Parcelable {
    private String api;
    private String body;

    public SchizoRequest(String api) {
        this.api = api;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
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
        dest.writeString(this.api);
        dest.writeString(this.body);
    }

    protected SchizoRequest(Parcel in) {
        this.api = in.readString();
        this.body = in.readString();
    }

    public static final Parcelable.Creator<SchizoRequest> CREATOR = new Parcelable.Creator<SchizoRequest>() {
        @Override
        public SchizoRequest createFromParcel(Parcel source) {
            return new SchizoRequest(source);
        }

        @Override
        public SchizoRequest[] newArray(int size) {
            return new SchizoRequest[size];
        }
    };
}
