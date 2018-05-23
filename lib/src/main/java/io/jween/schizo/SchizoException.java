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
