package io.jween.schizo;

/**
 * Created by Jwn on 2018/1/18.
 */

public class SchizoException extends Exception{
    private int code;

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
        String[] postSplit = errorBody.split(":", 2);
        return new SchizoException(Integer.valueOf(postSplit[0]), postSplit[1]);
    }
}
