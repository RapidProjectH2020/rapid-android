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
package org.gradle.language.nativeplatform.internal.incremental;

import org.gradle.api.Transformer;
import org.gradle.api.file.EmptyFileVisitor;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.changedetection.changes.IncrementalTaskInputsInternal;
import org.gradle.api.internal.changedetection.state.FileSnapshotter;
import org.gradle.api.internal.changedetection.state.TaskArtifactStateCacheAccess;
import org.gradle.api.internal.file.collections.DirectoryFileTree;
import org.gradle.api.internal.tasks.SimpleWorkResult;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.WorkResult;
import org.gradle.cache.PersistentStateCache;
import org.gradle.internal.Factory;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.language.base.internal.tasks.SimpleStaleClassCleaner;
import org.gradle.language.nativeplatform.internal.SourceIncludes;
import org.gradle.language.nativeplatform.internal.incremental.sourceparser.CSourceParser;
import org.gradle.language.nativeplatform.internal.incremental.sourceparser.RegexBackedCSourceParser;
import org.gradle.nativeplatform.toolchain.Clang;
import org.gradle.nativeplatform.toolchain.Gcc;
import org.gradle.nativeplatform.toolchain.NativeToolChain;
import org.gradle.nativeplatform.toolchain.internal.NativeCompileSpec;
import org.gradle.util.CollectionUtils;

import java.io.File;
import java.util.Collection;
import java.util.Map;

public class IncrementalNativeCompiler<T extends NativeCompileSpec> implements Compiler<T> {
    private final Compiler<T> delegateCompiler;
    private final boolean importsAreIncludes;
    private final TaskInternal task;
    private final TaskArtifactStateCacheAccess cacheAccess;
    private final FileSnapshotter fileSnapshotter;
    private final CompilationStateCacheFactory compilationStateCacheFactory;
    private final Logger logger = Logging.getLogger(IncrementalNativeCompiler.class);

    private final CSourceParser sourceParser = new RegexBackedCSourceParser();

    public IncrementalNativeCompiler(TaskInternal task, TaskArtifactStateCacheAccess cacheAccess, FileSnapshotter fileSnapshotter, CompilationStateCacheFactory compilationStateCacheFactory,
                                     Compiler<T> delegateCompiler, NativeToolChain toolChain) {
        this.task = task;
        this.cacheAccess = cacheAccess;
        this.fileSnapshotter = fileSnapshotter;
        this.compilationStateCacheFactory = compilationStateCacheFactory;
        this.delegateCompiler = delegateCompiler;
        this.importsAreIncludes = Clang.class.isAssignableFrom(toolChain.getClass()) || Gcc.class.isAssignableFrom(toolChain.getClass());
    }

    public WorkResult execute(final T spec) {
        final PersistentStateCache<CompilationState> compileStateCache = compilationStateCacheFactory.create(task.getPath());
        final IncrementalCompilation compilation = cacheAccess.useCache("process source files", new Factory<IncrementalCompilation>() {
            public IncrementalCompilation create() {
                DefaultSourceIncludesParser sourceIncludesParser = new DefaultSourceIncludesParser(sourceParser, importsAreIncludes);
                IncrementalCompileProcessor processor = createProcessor(compileStateCache, sourceIncludesParser, spec.getIncludeRoots());
                // TODO - do not hold the lock while processing the source files - this prevents other tasks from executing concurrently
                return processor.processSourceFiles(spec.getSourceFiles());
            }
        });

        spec.setSourceFileIncludes(mapIncludes(spec.getSourceFiles(), compilation.getFinalState()));

        final IncrementalTaskInputsInternal taskInputs = (IncrementalTaskInputsInternal) spec.getIncrementalInputs();

        handleDiscoveredInputs(spec, compilation, taskInputs);

        WorkResult workResult;
        if (spec.isIncrementalCompile()) {
            workResult = doIncrementalCompile(compilation, spec);
        } else {
            workResult = doCleanIncrementalCompile(spec);
        }

        cacheAccess.useCache("update compilation state", new Factory<Void>() {
            public Void create() {
                compileStateCache.set(compilation.getFinalState());
                return null;
            }
        });

        return workResult;
    }

    protected void handleDiscoveredInputs(T spec, IncrementalCompilation compilation, final IncrementalTaskInputsInternal taskInputs) {
        for (File includeFile : compilation.getIncludeCandidates()) {
            taskInputs.newInput(includeFile);
        }

        if (sourceFilesUseMacroIncludes(spec.getSourceFiles(), compilation.getFinalState())) {
            logger.info("The path to some #include files could not be determined.  Falling back to slow path which includes all files in the include search path as inputs for {}.", task.getName());
            for (final File includeRoot : spec.getIncludeRoots()) {
                logger.info("adding files in {} to discovered inputs for {}", includeRoot, task.getName());
                new DirectoryFileTree(includeRoot).visit(new EmptyFileVisitor() {
                    @Override
                    public void visitFile(FileVisitDetails fileDetails) {
                        taskInputs.newInput(fileDetails.getFile());
                    }
                });
            }
        }
    }

    private Map<File, SourceIncludes> mapIncludes(Collection<File> files, final CompilationState compilationState) {
        return CollectionUtils.collectMapValues(files, new Transformer<SourceIncludes, File>() {
            @Override
            public SourceIncludes transform(File file) {
                return compilationState.getState(file).getSourceIncludes();
            }
        });
    }

    private boolean sourceFilesUseMacroIncludes(Collection<File> files, final CompilationState compilationState) {
        // If we couldn't determine all dependencies of some files due to macros, we have to scan all include directories.
        return CollectionUtils.any(files, new Spec<File>() {
            @Override
            public boolean isSatisfiedBy(File file) {
                // If a macro was used for any #includes
                return CollectionUtils.any(compilationState.getState(file).getResolvedIncludes(), new Spec<ResolvedInclude>() {
                    @Override
                    public boolean isSatisfiedBy(ResolvedInclude element) {
                        // Using macro
                        return element.isMaybeMacro();
                    }
                });
            }
        });
    }

    protected WorkResult doIncrementalCompile(IncrementalCompilation compilation, T spec) {
        // Determine the actual sources to clean/compile
        spec.setSourceFiles(compilation.getRecompile());
        spec.setRemovedSourceFiles(compilation.getRemoved());
        return delegateCompiler.execute(spec);
    }

    protected WorkResult doCleanIncrementalCompile(T spec) {
        boolean deleted = cleanPreviousOutputs(spec);
        WorkResult compileResult = delegateCompiler.execute(spec);
        if (deleted && !compileResult.getDidWork()) {
            return new SimpleWorkResult(deleted);
        }
        return compileResult;
    }

    private boolean cleanPreviousOutputs(NativeCompileSpec spec) {
        SimpleStaleClassCleaner cleaner = new SimpleStaleClassCleaner(getTask().getOutputs());
        cleaner.setDestinationDir(spec.getObjectFileDir());
        cleaner.execute();
        return cleaner.getDidWork();
    }

    protected TaskInternal getTask() {
        return task;
    }

    private IncrementalCompileProcessor createProcessor(PersistentStateCache<CompilationState> compileStateCache, SourceIncludesParser sourceIncludesParser, Iterable<File> includes) {
        DefaultSourceIncludesResolver dependencyParser = new DefaultSourceIncludesResolver(CollectionUtils.toList(includes));

        return new IncrementalCompileProcessor(compileStateCache, dependencyParser, sourceIncludesParser, fileSnapshotter);
    }
}
