/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.test.fixtures.archive

import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipFile

class ZipTestFixture extends ArchiveTestFixture {
    ZipTestFixture(File file, String encoding=null) {
        def zipFile = new ZipFile(file, encoding)
        try {
            def entries = zipFile.getEntries()
            while (entries.hasMoreElements()) {
                def entry = entries.nextElement()
                String content = getContentForEntry(entry, zipFile)
                if (!entry.directory) {
                    add(entry.name, content)
                }
            }
        } finally {
            zipFile.close();
        }
    }

    private String getContentForEntry(ZipEntry entry, ZipFile zipFile) {
        def extension = entry.name.tokenize(".").last()
        if (!(extension in ["jar", "zip"])) {
            return zipFile.getInputStream(entry).text
        }
        return ""
    }
}
