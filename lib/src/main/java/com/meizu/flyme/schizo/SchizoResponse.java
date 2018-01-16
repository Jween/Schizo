package com.meizu.flyme.schizo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Jwn on 2017/12/29.
 */

public class SchizoResponse implements Parcelable {

    public interface CODE {
        int ERROR = 404;
        int SUCCESS = 200;
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
        this.code = in.readInt();
        this.body = in.readString();
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
}
