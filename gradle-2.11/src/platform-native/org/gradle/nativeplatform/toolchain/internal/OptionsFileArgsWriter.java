/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.nativeplatform.toolchain.internal;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Action;
import org.gradle.api.Transformer;
import org.gradle.api.UncheckedIOException;
import org.gradle.platform.base.internal.toolchain.ArgWriter;
import org.gradle.util.GFileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class OptionsFileArgsWriter implements Action<List<String>> {
    private final Transformer<ArgWriter, PrintWriter> argWriterFactory;
    private final File tempDir;

    public OptionsFileArgsWriter(Transformer<ArgWriter, PrintWriter> argWriterFactory, File tempDir) {
        this.argWriterFactory = argWriterFactory;
        this.tempDir = tempDir;
    }

    @Override
    public void execute(List<String> args) {
        List<String> originalArgs = Lists.newArrayList(args);
        args.clear();
        args.addAll(transformArgs(originalArgs, tempDir));
    }

    protected List<String> transformArgs(List<String> originalArgs, File tempDir) {
        GFileUtils.mkdirs(tempDir);
        File optionsFile = new File(tempDir, "options.txt");
        try {
            PrintWriter writer = new PrintWriter(optionsFile);
            try {
                ArgWriter argWriter = argWriterFactory.transform(writer);
                argWriter.args(originalArgs);
            } finally {
                IOUtils.closeQuietly(writer);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Could not write compiler options file '%s'.", optionsFile.getAbsolutePath()), e);
        }

        return Arrays.asList(String.format("@%s", optionsFile.getAbsolutePath()));
    }
}
