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

package org.gradle.nativeplatform.toolchain.internal.msvcpp;

import org.gradle.api.Named;
import org.gradle.nativeplatform.platform.Architecture;
import org.gradle.nativeplatform.platform.internal.NativePlatformInternal;
import org.gradle.util.VersionNumber;

import java.io.File;
import java.util.List;
import java.util.Map;

public class VisualCppInstall implements Named {
    private static final String COMPILER_FILENAME = "cl.exe";
    private static final String LINKER_FILENAME = "link.exe";
    private static final String ARCHIVER_FILENAME = "lib.exe";

    private final Map<Architecture, List<File>> paths;
    private final Map<Architecture, File> binaryPaths;
    private final Map<Architecture, File> libraryPaths;
    private final Map<Architecture, File> includePaths;
    private final Map<Architecture, String> assemblerFilenames;
    private final Map<Architecture, Map<String, String>> definitions;
    private final String name;
    private final VersionNumber version;

    public VisualCppInstall(String name, VersionNumber version,
            Map<Architecture, List<File>> paths, Map<Architecture, File> binaryPaths, Map<Architecture, File> libraryPaths,
            Map<Architecture, File> includePaths, Map<Architecture, String> assemblerFilenames,
            Map<Architecture, Map<String, String>> definitions) {
        this.paths = paths;
        this.name = name;
        this.version = version;
        this.binaryPaths = binaryPaths;
        this.libraryPaths = libraryPaths;
        this.includePaths = includePaths;
        this.assemblerFilenames = assemblerFilenames;
        this.definitions = definitions;
    }

    public String getName() {
        return name;
    }

    public VersionNumber getVersion() {
        return version;
    }

    public boolean isSupportedPlatform(NativePlatformInternal targetPlatform) {
        // TODO:ADAM - ARM only if the target OS is Windows 8 or later
        // TODO:MPUT - ARM also if the target OS is Windows RT or Windows Phone/Mobile/CE
        // TODO:ADAM - IA64 only if the target OS is Windows 2008 or earlier
        return targetPlatform.getOperatingSystem().isWindows()
                && (binaryPaths.containsKey(getPlatformArchitecture(targetPlatform)));
    }

    public List<File> getPath(NativePlatformInternal targetPlatform) {
        return paths.get(getPlatformArchitecture(targetPlatform));
    }

    public File getCompiler(NativePlatformInternal targetPlatform) {
        return new File(binaryPaths.get(getPlatformArchitecture(targetPlatform)), COMPILER_FILENAME);
    }

    public File getLinker(NativePlatformInternal targetPlatform) {
        return new File(binaryPaths.get(getPlatformArchitecture(targetPlatform)), LINKER_FILENAME);
    }

    public File getArchiver(NativePlatformInternal targetPlatform) {
        return new File(binaryPaths.get(getPlatformArchitecture(targetPlatform)), ARCHIVER_FILENAME);
    }

    public File getAssembler(NativePlatformInternal targetPlatform) {
        Architecture architecture = getPlatformArchitecture(targetPlatform);
        return new File(binaryPaths.get(architecture), assemblerFilenames.get(architecture));
    }

    public File getBinaryPath(NativePlatformInternal targetPlatform) {
        return binaryPaths.get(getPlatformArchitecture(targetPlatform));
    }

    public File getLibraryPath(NativePlatformInternal targetPlatform) {
        return libraryPaths.get(getPlatformArchitecture(targetPlatform));
    }

    public Map<String, String> getDefinitions(NativePlatformInternal targetPlatform) {
        return definitions.get(getPlatformArchitecture(targetPlatform));
    }

    public File getIncludePath(NativePlatformInternal targetPlatform) {
        return includePaths.get(getPlatformArchitecture(targetPlatform));
    }

    private Architecture getPlatformArchitecture(NativePlatformInternal targetPlatform) {
        return targetPlatform.getArchitecture();
    }
}
