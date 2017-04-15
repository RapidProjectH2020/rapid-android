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

package org.gradle.nativeplatform.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Incubating
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.internal.operations.logging.BuildOperationLoggerFactory
import org.gradle.nativeplatform.internal.BuildOperationLoggingCompilerDecorator
import org.gradle.nativeplatform.internal.DefaultStaticLibraryArchiverSpec
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.nativeplatform.platform.internal.NativePlatformInternal
import org.gradle.nativeplatform.toolchain.NativeToolChain
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainInternal

import javax.inject.Inject

/**
 * Assembles a static library from object files.
 */
@Incubating
@ParallelizableTask
class CreateStaticLibrary extends DefaultTask implements ObjectFilesToBinary {
    private FileCollection source

    @Inject
    CreateStaticLibrary() {
        source = project.files()
        inputs.property("outputType") {
            NativeToolChainInternal.Identifier.identify((NativeToolChainInternal) toolChain, (NativePlatformInternal) targetPlatform)
        }
    }

    /**
     * The tool chain used for creating the static library.
     */
    NativeToolChain toolChain

    /**
     * The platform being targeted.
     */
    NativePlatform targetPlatform

    /**
     * The file where the output binary will be located.
     */
    @OutputFile
    File outputFile

    /**
     * The source object files to be passed to the archiver.
     */
    @InputFiles
    @SkipWhenEmpty
    // Can't use field due to GRADLE-2026
    FileCollection getSource() {
        source
    }

    /**
     * Adds a set of object files to be linked.
     * <p>
     * The provided source object is evaluated as per {@link org.gradle.api.Project#files(Object ...)}.
     */
    void source(Object source) {
        this.source.from source
    }

    /**
     * Additional arguments passed to the archiver.
     */
    @Input
    List<String> staticLibArgs

    @Inject
    public BuildOperationLoggerFactory getOperationLoggerFactory() {
        throw new UnsupportedOperationException();
    }

    @TaskAction
    void link() {

        def spec = new DefaultStaticLibraryArchiverSpec()
        spec.tempDir = getTemporaryDir()
        spec.outputFile = getOutputFile()
        spec.objectFiles getSource()
        spec.args getStaticLibArgs()

        def operationLogger = getOperationLoggerFactory().newOperationLogger(getName(), getTemporaryDir())
        spec.operationLogger = operationLogger

        def result = BuildOperationLoggingCompilerDecorator.wrap(toolChain.select(targetPlatform).newCompiler(spec.getClass())).execute(spec)
        didWork = result.didWork
    }

}
