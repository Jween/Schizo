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

package io.jween.schizo.processor.util;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;


public class ElementUtil {

    /**
     *  get all methods annotated with given annotation from the specific class.
     *
     * @param elements the given processing environment element utils
     * @param type the class type element
     * @param annotation the given annotation
     * @return
     */
    public static Set<Element> getAnnotatedElements(
            Elements elements,
            TypeElement type,
            Class<? extends Annotation> annotation) {
        Set<Element> found = new HashSet<>();
        for (Element e : elements.getAllMembers(type)) {
            if (e.getAnnotation(annotation) != null)
                found.add(e);
        }
        return found;
    }
}
