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

package org.gradle.language.nativeplatform.internal;

import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.internal.operations.logging.BuildOperationLogger;
import org.gradle.nativeplatform.internal.AbstractBinaryToolSpec;
import org.gradle.nativeplatform.toolchain.internal.NativeCompileSpec;

import java.io.File;
import java.util.*;

public abstract class AbstractNativeCompileSpec extends AbstractBinaryToolSpec implements NativeCompileSpec {

    private List<File> includeRoots = new ArrayList<File>();
    private List<File> sourceFiles = new ArrayList<File>();
    private List<File> removedSourceFiles = new ArrayList<File>();
    private boolean incrementalCompile;
    private Map<String, String> macros = new LinkedHashMap<String, String>();
    private File objectFileDir;
    private boolean positionIndependentCode;
    private BuildOperationLogger oplogger;
    private File prefixHeaderFile;
    private File preCompiledHeaderObjectFile;
    private Map<File, SourceIncludes> sourceFileIncludes;
    private String preCompiledHeader;
    private IncrementalTaskInputs incrementalTaskInputs;

    public List<File> getIncludeRoots() {
        return includeRoots;
    }

    public void include(File... includeRoots) {
        Collections.addAll(this.includeRoots, includeRoots);
    }

    public void include(Iterable<File> includeRoots) {
        addAll(this.includeRoots, includeRoots);
    }

    public List<File> getSourceFiles() {
        return sourceFiles;
    }

    public void source(Iterable<File> sources) {
        addAll(sourceFiles, sources);
    }

    public void setSourceFiles(Collection<File> sources) {
        sourceFiles.clear();
        sourceFiles.addAll(sources);
    }

    public List<File> getRemovedSourceFiles() {
        return removedSourceFiles;
    }

    public void removedSource(Iterable<File> sources) {
        addAll(removedSourceFiles, sources);
    }

    public void setRemovedSourceFiles(Collection<File> sources) {
        removedSourceFiles.clear();
        removedSourceFiles.addAll(sources);
    }

    public boolean isIncrementalCompile() {
        return incrementalCompile;
    }

    public void setIncrementalCompile(boolean flag) {
        incrementalCompile = flag;
    }

    public File getObjectFileDir() {
        return objectFileDir;
    }

    public void setObjectFileDir(File objectFileDir) {
        this.objectFileDir = objectFileDir;
    }

    public Map<String, String> getMacros() {
        return macros;
    }

    public void setMacros(Map<String, String> macros) {
        this.macros = macros;
    }

    public void define(String name) {
        macros.put(name, null);
    }

    public void define(String name, String value) {
        macros.put(name, value);
    }

    public boolean isPositionIndependentCode() {
        return positionIndependentCode;
    }

    public void setPositionIndependentCode(boolean positionIndependentCode) {
        this.positionIndependentCode = positionIndependentCode;
    }

    @Override
    public File getPreCompiledHeaderObjectFile() {
        return preCompiledHeaderObjectFile;
    }

    @Override
    public void setPreCompiledHeaderObjectFile(File preCompiledHeaderObjectFile) {
        this.preCompiledHeaderObjectFile = preCompiledHeaderObjectFile;
    }

    @Override
    public File getPrefixHeaderFile() {
        return prefixHeaderFile;
    }

    @Override
    public void setPrefixHeaderFile(File pchFile) {
        this.prefixHeaderFile = pchFile;
    }

    @Override
    public String getPreCompiledHeader() {
        return preCompiledHeader;
    }

    @Override
    public void setPreCompiledHeader(String preCompiledHeader) {
        this.preCompiledHeader = preCompiledHeader;
    }

    private void addAll(List<File> list, Iterable<File> iterable) {
        for (File file : iterable) {
            list.add(file);
        }
    }

    public BuildOperationLogger getOperationLogger() {
        return oplogger;
    }

    public void setOperationLogger(BuildOperationLogger oplogger) {
        this.oplogger = oplogger;
    }

    @Override
    public Map<File, SourceIncludes> getSourceFileIncludes() {
        return sourceFileIncludes;
    }

    @Override
    public void setSourceFileIncludes(Map<File, SourceIncludes> map) {
        this.sourceFileIncludes = map;
    }

    public void setIncrementalInputs(IncrementalTaskInputs inputs) {
        this.incrementalTaskInputs = inputs;
    }

    public IncrementalTaskInputs getIncrementalInputs() {
        return incrementalTaskInputs;
    }
}
