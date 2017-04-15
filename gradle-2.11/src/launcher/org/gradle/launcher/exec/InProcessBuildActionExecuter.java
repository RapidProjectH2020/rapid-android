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

package org.gradle.launcher.exec;

import org.gradle.api.internal.GradleInternal;
import org.gradle.initialization.BuildRequestContext;
import org.gradle.initialization.DefaultGradleLauncher;
import org.gradle.initialization.GradleLauncherFactory;
import org.gradle.internal.invocation.BuildAction;
import org.gradle.internal.invocation.BuildActionRunner;
import org.gradle.internal.invocation.BuildController;
import org.gradle.internal.service.ServiceRegistry;

public class InProcessBuildActionExecuter implements BuildActionExecuter<BuildActionParameters> {
    private final GradleLauncherFactory gradleLauncherFactory;
    private final BuildActionRunner buildActionRunner;

    public InProcessBuildActionExecuter(GradleLauncherFactory gradleLauncherFactory, BuildActionRunner buildActionRunner) {
        this.gradleLauncherFactory = gradleLauncherFactory;
        this.buildActionRunner = buildActionRunner;
    }

    public Object execute(BuildAction action, BuildRequestContext buildRequestContext, BuildActionParameters actionParameters, ServiceRegistry contextServices) {
        DefaultGradleLauncher gradleLauncher = (DefaultGradleLauncher) gradleLauncherFactory.newInstance(action.getStartParameter(), buildRequestContext, contextServices);
        try {
            gradleLauncher.addStandardOutputListener(buildRequestContext.getOutputListener());
            gradleLauncher.addStandardErrorListener(buildRequestContext.getErrorListener());
            DefaultBuildController buildController = new DefaultBuildController(gradleLauncher);
            buildActionRunner.run(action, buildController);
            return buildController.result;
        } finally {
            gradleLauncher.stop();
        }
    }

    private static class DefaultBuildController implements BuildController {
        private enum State {Created, Completed}

        private State state = State.Created;
        private boolean hasResult;
        private Object result;
        private final DefaultGradleLauncher gradleLauncher;

        public DefaultBuildController(DefaultGradleLauncher gradleLauncher) {
            this.gradleLauncher = gradleLauncher;
        }

        public DefaultGradleLauncher getLauncher() {
            if (state == State.Completed) {
                throw new IllegalStateException("Cannot use launcher after build has completed.");
            }
            return gradleLauncher;
        }

        @Override
        public boolean hasResult() {
            return hasResult;
        }

        @Override
        public Object getResult() {
            if (!hasResult) {
                throw new IllegalStateException("No result has been provided for this build action.");
            }
            return result;
        }

        @Override
        public void setResult(Object result) {
            this.hasResult = true;
            this.result = result;
        }

        public GradleInternal getGradle() {
            return getLauncher().getGradle();
        }

        public GradleInternal run() {
            try {
                return (GradleInternal) getLauncher().run().getGradle();
            } finally {
                state = State.Completed;
            }
        }

        public GradleInternal configure() {
            try {
                return (GradleInternal) getLauncher().getBuildAnalysis().getGradle();
            } finally {
                state = State.Completed;
            }
        }
    }
}
