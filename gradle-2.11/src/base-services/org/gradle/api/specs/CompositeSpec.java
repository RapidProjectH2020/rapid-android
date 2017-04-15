/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.specs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A {@link org.gradle.api.specs.Spec} which aggregates a sequence of other {@code Spec} instances.
 *
 * @param <T> The target type for this Spec
 */
abstract public class CompositeSpec<T> implements Spec<T> {
    private final Spec<? super T>[] specs;

    protected CompositeSpec(Spec<? super T>... specs) {
        this(Arrays.asList(specs));
    }

    protected CompositeSpec(Iterable<? extends Spec<? super T>> specs) {
        List<Spec<? super T>> copy = new ArrayList<Spec<? super T>>();
        for (Spec<? super T> spec : specs) {
            copy.add(spec);
        }
        this.specs = copy.toArray(new Spec[copy.size()]);
    }

    // Not public. Evaluation of these specs is a major hot spot for large builds, so use an array for iteration
    Spec<? super T>[] getSpecsArray() {
        return specs;
    }

    public List<Spec<? super T>> getSpecs() {
        return Collections.unmodifiableList(Arrays.asList(specs));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompositeSpec)) {
            return false;
        }

        CompositeSpec that = (CompositeSpec) o;
        return Arrays.equals(specs, that.specs);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(specs);
    }
}
