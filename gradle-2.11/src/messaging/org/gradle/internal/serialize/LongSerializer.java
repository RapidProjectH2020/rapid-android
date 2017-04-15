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
package org.gradle.internal.serialize;

public class LongSerializer implements Serializer<Long> {
    public Long read(Decoder decoder) throws Exception {
        return decoder.readLong();
    }

    public void write(Encoder encoder, Long value) throws Exception {
        if (value == null) {
            throw new IllegalArgumentException("This serializer does not serialize null values.");
        }
        encoder.writeLong(value);
    }
}
