/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.play.tasks;

import org.gradle.api.Incubating;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.BaseForkOptions;
import org.gradle.deployment.internal.DeploymentRegistry;
import org.gradle.logging.ProgressLogger;
import org.gradle.logging.ProgressLoggerFactory;
import org.gradle.play.internal.run.*;
import org.gradle.play.internal.toolchain.PlayToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Task to run a Play application.
 */
@Incubating
public class PlayRun extends ConventionTask {
    private static Logger logger = LoggerFactory.getLogger(PlayRun.class);

    private int httpPort;

    @InputFile
    private File applicationJar;

    @InputFile
    private File assetsJar;

    @InputFiles
    private Set<File> assetsDirs;

    @InputFiles
    private FileCollection runtimeClasspath;

    @InputFiles
    private FileCollection changingClasspath;

    private BaseForkOptions forkOptions;

    private PlayToolProvider playToolProvider;

    /**
     * fork options for the running a Play application.
     */
    public BaseForkOptions getForkOptions() {
        if (forkOptions == null) {
            forkOptions = new BaseForkOptions();
        }
        return forkOptions;
    }

    @TaskAction
    public void run() {
        ProgressLoggerFactory progressLoggerFactory = getServices().get(ProgressLoggerFactory.class);
        PlayApplicationDeploymentHandle deploymentHandle = registerOrFindDeploymentHandle(getPath());

        if (!deploymentHandle.isRunning()) {
            ProgressLogger progressLogger = progressLoggerFactory.newOperation(PlayRun.class)
                .start("Start Play server", "Starting Play");

            try {
                int httpPort = getHttpPort();
                PlayRunSpec spec = new DefaultPlayRunSpec(runtimeClasspath, changingClasspath, applicationJar, assetsJar, assetsDirs, getProject().getProjectDir(), getForkOptions(), httpPort);
                PlayApplicationRunnerToken runnerToken = playToolProvider.get(PlayApplicationRunner.class).start(spec);
                deploymentHandle.start(runnerToken);
            } finally {
                progressLogger.completed();
            }
        }

        if (!getProject().getGradle().getStartParameter().isContinuous()) {
            ProgressLogger progressLogger = progressLoggerFactory.newOperation(PlayRun.class)
                .start(String.format("Run Play App at http://localhost:%d/", httpPort),
                    String.format("Running at http://localhost:%d/", httpPort));
            try {
                waitForCtrlD();
            } finally {
                progressLogger.completed();
            }
        } else {
            logger.warn(String.format("Running Play App (%s) at http://localhost:%d/", getPath(), httpPort));
        }
    }

    private void waitForCtrlD() {
        while (true) {
            try {
                int c = System.in.read();
                if (c == -1 || c == 4) {
                    // STOP on Ctrl-D or EOF.
                    logger.info("received end of stream (ctrl+d)");
                    return;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * The HTTP port listened to by the Play application.
     *
     * This port should be available.  The Play application will fail to start if the port is already in use.
     *
     * @return HTTP port
     */
    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    /**
     * The Play application jar to run.
     */
    public File getApplicationJar() {
        return applicationJar;
    }

    public void setApplicationJar(File applicationJar) {
        this.applicationJar = applicationJar;
    }

    /**
     * The assets jar to run with the Play application.
     */
    public File getAssetsJar() {
        return assetsJar;
    }

    public void setAssetsJar(File assetsJar) {
        this.assetsJar = assetsJar;
    }

    /**
     * The directories of the assets for the Play application (for live reload functionality).
     */
    public Set<File> getAssetsDirs() {
        return assetsDirs;
    }

    public void setAssetsDirs(Set<File> assetsDirs) {
        this.assetsDirs = assetsDirs;
    }

    public void setRuntimeClasspath(FileCollection runtimeClasspath) {
        this.runtimeClasspath = runtimeClasspath;
    }

    public void setChangingClasspath(FileCollection changingClasspath) {
        this.changingClasspath = changingClasspath;
    }

    public void setPlayToolProvider(PlayToolProvider playToolProvider) {
        this.playToolProvider = playToolProvider;
    }

    @Inject
    public DeploymentRegistry getDeploymentRegistry() {
        throw new UnsupportedOperationException();
    }

    private PlayApplicationDeploymentHandle registerOrFindDeploymentHandle(String deploymentId) {
        DeploymentRegistry deploymentRegistry = getDeploymentRegistry();
        PlayApplicationDeploymentHandle deploymentHandle = deploymentRegistry.get(PlayApplicationDeploymentHandle.class, deploymentId);
        if (deploymentHandle == null) {
            deploymentHandle = new PlayApplicationDeploymentHandle(deploymentId);
            deploymentRegistry.register(deploymentId, deploymentHandle);
        }
        return deploymentHandle;
    }
}
