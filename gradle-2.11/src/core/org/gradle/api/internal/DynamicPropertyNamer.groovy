/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.api.internal

import org.gradle.api.Namer

class DynamicPropertyNamer implements Namer<Object> {

    final String propertyName

    DynamicPropertyNamer() {
        this("name")
    }

    DynamicPropertyNamer(String propertyName) {
        this.propertyName = propertyName
    }

    String determineName(thing) {
        def name

        try {
            name = thing."$propertyName"
        } catch (MissingPropertyException e) {
            throw new NoNamingPropertyException(thing, propertyName)
        }
        
        if (name == null) {
            throw new NullNamingPropertyException(thing, propertyName)
        }
        
        name.toString()
    }
}