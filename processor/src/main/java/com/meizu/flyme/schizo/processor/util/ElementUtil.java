package com.meizu.flyme.schizo.processor.util;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Created by Jwn on 2018/1/23.
 */

public class ElementUtil {

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
