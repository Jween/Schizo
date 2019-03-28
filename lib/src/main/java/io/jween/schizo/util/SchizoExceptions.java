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

package io.jween.schizo.util;

import io.jween.schizo.SchizoException;
import io.jween.schizo.SchizoResponse;

public class SchizoExceptions {
    public static SchizoException from(Throwable throwable) {
        SchizoException exception;
        Throwable cause = throwable.getCause();
        if (cause == null) {
            exception = new SchizoException(
                    SchizoResponse.CODE.ILLEGAL_ACCESS, "Unknown cause");
        } else if (cause instanceof SchizoException) {
            exception = (SchizoException) cause;
        } else {
            exception = new SchizoException(
                    SchizoResponse.CODE.ILLEGAL_ACCESS, cause.getMessage());
        }
        return exception;
    }
}
