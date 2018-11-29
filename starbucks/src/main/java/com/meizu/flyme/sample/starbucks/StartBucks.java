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

package com.meizu.flyme.sample.starbucks;

import android.text.TextUtils;

import com.meizu.flyme.sample.starbucks.bean.Milk;

import java.util.concurrent.TimeUnit;

import io.jween.schizo.SchizoException;
import io.jween.schizo.SchizoResponse;
import io.jween.schizo.annotation.Action;
import io.jween.schizo.annotation.Api;
import io.jween.schizo.service.SchizoService;
import io.reactivex.Observable;

@Action("io.jween.service.starbucks")
public class StartBucks extends SchizoService {

    @Api("getCoffee")
    Coffee makeCoffee(String name)
            throws Exception{
        if (TextUtils.isEmpty(name)) {
            throw new SchizoException(SchizoResponse.CODE.ERROR,
                    "Unknown coffee name");
        }
        return new Coffee(name, "StarBucks");
    }

    @Api("buyMilk")
    Milk makeMilk(String name) {
        return new Milk(2, "StarBucks", name);
    }

    @Api("clock")
    Observable<Long> clock() {
        return Observable.interval(1, TimeUnit.SECONDS);
    }
}
