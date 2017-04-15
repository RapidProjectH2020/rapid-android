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

package org.gradle.initialization.buildsrc;

import org.gradle.StartParameter;
import org.gradle.api.internal.initialization.ClassLoaderScope;
import org.gradle.cache.CacheRepository;
import org.gradle.cache.PersistentCache;
import org.gradle.cache.internal.FileLockManager;
import org.gradle.initialization.GradleLauncher;
import org.gradle.initialization.GradleLauncherFactory;
import org.gradle.internal.Factory;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.classpath.DefaultClassPath;
import org.gradle.internal.progress.BuildOperationDetails;
import org.gradle.internal.progress.BuildOperationExecutor;
import org.gradle.util.GradleVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;

import static org.gradle.cache.internal.filelock.LockOptionsBuilder.mode;

public class BuildSourceBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildSourceBuilder.class);

    private final GradleLauncherFactory gradleLauncherFactory;
    private final ClassLoaderScope classLoaderScope;
    private final CacheRepository cacheRepository;
    private final BuildOperationExecutor buildOperationExecutor;

    public BuildSourceBuilder(GradleLauncherFactory gradleLauncherFactory, ClassLoaderScope classLoaderScope, CacheRepository cacheRepository, BuildOperationExecutor buildOperationExecutor) {
        this.gradleLauncherFactory = gradleLauncherFactory;
        this.classLoaderScope = classLoaderScope;
        this.cacheRepository = cacheRepository;
        this.buildOperationExecutor = buildOperationExecutor;
    }

    public ClassLoaderScope buildAndCreateClassLoader(StartParameter startParameter) {
        ClassPath classpath = createBuildSourceClasspath(startParameter);
        ClassLoaderScope childScope = classLoaderScope.createChild(startParameter.getCurrentDir().getAbsolutePath());
        childScope.export(classpath);
        childScope.lock();
        return childScope;
    }

    ClassPath createBuildSourceClasspath(final StartParameter startParameter) {
        assert startParameter.getCurrentDir() != null && startParameter.getBuildFile() == null;

        LOGGER.debug("Starting to build the build sources.");
        if (!startParameter.getCurrentDir().isDirectory()) {
            LOGGER.debug("Gradle source dir does not exist. We leave.");
            return new DefaultClassPath();
        }
        return buildOperationExecutor.run(BuildOperationDetails.displayName("Build buildSrc").progressDisplayName("buildSrc").build(), new Factory<ClassPath>() {
            @Override
            public ClassPath create() {
                return buildBuildSrc(startParameter);
            }
        });
    }

    private ClassPath buildBuildSrc(StartParameter startParameter) {
        // If we were not the most recent version of Gradle to build the buildSrc dir, then do a clean build
        // Otherwise, just to a regular build
        final PersistentCache buildSrcCache = createCache(startParameter);
        try {
            GradleLauncher gradleLauncher = buildGradleLauncher(startParameter);
            try {
                return buildSrcCache.useCache("rebuild buildSrc", new BuildSrcUpdateFactory(buildSrcCache, gradleLauncher, new BuildSrcBuildListenerFactory()));
            } finally {
                gradleLauncher.stop();
            }
        } finally {
            // This isn't quite right. We should not unlock the classes until we're finished with them, and the classes may be used across multiple builds
            buildSrcCache.close();
        }
    }

    PersistentCache createCache(StartParameter startParameter) {
        return cacheRepository
                .cache(new File(startParameter.getCurrentDir(), ".gradle/noVersion/buildSrc"))
                .withCrossVersionCache()
                .withDisplayName("buildSrc state cache")
                .withLockOptions(mode(FileLockManager.LockMode.None).useCrossVersionImplementation())
                .withProperties(Collections.singletonMap("gradle.version", GradleVersion.current().getVersion()))
                .open();
    }

    private GradleLauncher buildGradleLauncher(StartParameter startParameter) {
        final StartParameter startParameterArg = startParameter.newInstance();
        startParameterArg.setProjectProperties(startParameter.getProjectProperties());
        startParameterArg.setSearchUpwards(false);
        startParameterArg.setProfile(startParameter.isProfile());
        return gradleLauncherFactory.newInstance(startParameterArg);
    }
}
