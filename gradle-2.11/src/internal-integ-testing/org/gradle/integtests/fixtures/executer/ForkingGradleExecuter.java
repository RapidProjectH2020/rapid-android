/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.integtests.fixtures.executer;

import org.gradle.api.Action;
import org.gradle.internal.Factory;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.process.internal.ExecHandleBuilder;
import org.gradle.process.internal.JvmOptions;
import org.gradle.test.fixtures.file.TestDirectoryProvider;
import org.gradle.test.fixtures.file.TestFile;
import org.gradle.testfixtures.internal.NativeServicesTestFixture;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

public class ForkingGradleExecuter extends AbstractGradleExecuter {

    public ForkingGradleExecuter(GradleDistribution distribution, TestDirectoryProvider testDirectoryProvider) {
        super(distribution, testDirectoryProvider);
    }

    public void assertCanExecute() throws AssertionError {
        if (!getDistribution().isSupportsSpacesInGradleAndJavaOpts()) {
            Map<String, String> environmentVars = buildInvocation().environmentVars;
            for (String envVarName : Arrays.asList("JAVA_OPTS", "GRADLE_OPTS")) {
                String envVarValue = environmentVars.get(envVarName);
                if (envVarValue == null) {
                    continue;
                }
                for (String arg : JvmOptions.fromString(envVarValue)) {
                    if (arg.contains(" ")) {
                        throw new AssertionError(String.format("Env var %s contains arg with space (%s) which is not supported by Gradle %s", envVarName, arg, getDistribution().getVersion().getVersion()));
                    }
                }
            }
        }
    }

    @Override
    protected void transformInvocation(GradleInvocation invocation) {
        if (getDistribution().isSupportsSpacesInGradleAndJavaOpts()) {
            // Mix the implicit launcher JVM args in with the requested JVM args
            invocation.launcherJvmArgs.addAll(invocation.implicitLauncherJvmArgs);
        } else {
            // Need to move those implicit JVM args that contain a space to the Gradle command-line (if possible)
            // Note that this isn't strictly correct as some system properties can only be set on JVM start up.
            // Should change the implementation to deal with these properly
            for (String jvmArg : invocation.implicitLauncherJvmArgs) {
                if (!jvmArg.contains(" ")) {
                    invocation.launcherJvmArgs.add(jvmArg);
                } else if (jvmArg.startsWith("-D")) {
                    invocation.args.add(jvmArg);
                } else {
                    throw new UnsupportedOperationException(String.format("Cannot handle launcher JVM arg '%s' as it contains whitespace. This is not supported by Gradle %s.",
                            jvmArg, getDistribution().getVersion().getVersion()));
                }
            }
        }
        invocation.implicitLauncherJvmArgs.clear();

        // Inject the launcher JVM args via one of the environment variables
        Map<String, String> environmentVars = invocation.environmentVars;
        String jvmOptsEnvVar;
        if (!environmentVars.containsKey("GRADLE_OPTS")) {
            jvmOptsEnvVar = "GRADLE_OPTS";
        } else if (!environmentVars.containsKey("JAVA_OPTS")) {
            jvmOptsEnvVar = "JAVA_OPTS";
        } else {
            // This could be handled, just not implemented yet
            throw new UnsupportedOperationException(String.format("Both GRADLE_OPTS and JAVA_OPTS environment variables are being used. Cannot provide JVM args %s to Gradle command.", invocation.launcherJvmArgs));
        }
        environmentVars.put(jvmOptsEnvVar, toJvmArgsString(invocation.launcherJvmArgs));

        // Add a JAVA_HOME if none provided
        if (!environmentVars.containsKey("JAVA_HOME")) {
            environmentVars.put("JAVA_HOME", getJavaHome().getAbsolutePath());
        }
    }

    @Override
    protected List<String> getAllArgs() {
        List<String> args = new ArrayList<String>();
        args.addAll(super.getAllArgs());
        args.add("--stacktrace");
        addPropagatedSystemProperties(args);
        return args;
    }

    private void addPropagatedSystemProperties(List<String> args) {
        for (String propName : propagatedSystemProperties) {
            String propValue = System.getProperty(propName);
            if (propValue != null) {
                args.add("-D" + propName + "=" + propValue);
            }
        }
    }

    private ExecHandleBuilder createExecHandleBuilder() {
        TestFile gradleHomeDir = getDistribution().getGradleHomeDir();
        if (!gradleHomeDir.isDirectory()) {
            fail(gradleHomeDir + " is not a directory.\n"
                + "If you are running tests from IDE make sure that gradle tasks that prepare the test image were executed. Last time it was 'intTestImage' task.");
        }

        NativeServicesTestFixture.initialize();
        ExecHandleBuilder builder = new ExecHandleBuilder() {
            @Override
            public File getWorkingDir() {
                // Override this, so that the working directory is not canonicalised. Some int tests require that
                // the working directory is not canonicalised
                return ForkingGradleExecuter.this.getWorkingDir();
            }
        };

        // Clear the user's environment
        builder.environment("GRADLE_HOME", "");
        builder.environment("JAVA_HOME", "");
        builder.environment("GRADLE_OPTS", "");
        builder.environment("JAVA_OPTS", "");

        GradleInvocation invocation = buildInvocation();

        builder.environment(invocation.environmentVars);
        builder.workingDir(getWorkingDir());
        builder.setStandardInput(connectStdIn());

        builder.args(invocation.args);

        ExecHandlerConfigurer configurer = OperatingSystem.current().isWindows() ? new WindowsConfigurer() : new UnixConfigurer();
        configurer.configure(builder);

        getLogger().info(String.format("Execute in %s with: %s %s", builder.getWorkingDir(), builder.getExecutable(), builder.getArgs()));

        return builder;
    }

    @Override
    public GradleHandle doStart() {
        return createGradleHandle(getResultAssertion(), getDefaultCharacterEncoding(), new Factory<ExecHandleBuilder>() {
            public ExecHandleBuilder create() {
                return createExecHandleBuilder();
            }
        }).start();
    }

    protected ForkingGradleHandle createGradleHandle(Action<ExecutionResult> resultAssertion, String encoding, Factory<ExecHandleBuilder> execHandleFactory) {
        return new ForkingGradleHandle(getStdinPipe(), isUseDaemon(), resultAssertion, encoding, execHandleFactory);
    }

    protected ExecutionResult doRun() {
        return start().waitForFinish();
    }

    protected ExecutionFailure doRunWithFailure() {
        return start().waitForFailure();
    }

    private interface ExecHandlerConfigurer {
        void configure(ExecHandleBuilder builder);
    }

    private class WindowsConfigurer implements ExecHandlerConfigurer {
        public void configure(ExecHandleBuilder builder) {
            String cmd;
            if (getExecutable() != null) {
                cmd = getExecutable().replace('/', File.separatorChar);
            } else {
                cmd = "gradle";
            }
            builder.executable("cmd");

            List<String> allArgs = builder.getArgs();
            builder.setArgs(Arrays.asList("/c", cmd));
            builder.args(allArgs);

            String gradleHome = getDistribution().getGradleHomeDir().getAbsolutePath();

            // NOTE: Windows uses Path, but allows asking for PATH, and PATH
            //       is set within builder object for some things such
            //       as CommandLineIntegrationTest, try PATH first, and
            //       then revert to default of Path if null
            Object path = builder.getEnvironment().get("PATH");
            if (path == null) {
                path = builder.getEnvironment().get("Path");
            }
            path = String.format("%s\\bin;%s", gradleHome, path);
            builder.environment("PATH", path);
            builder.environment("Path", path);
            builder.environment("GRADLE_EXIT_CONSOLE", "true");
        }
    }

    private class UnixConfigurer implements ExecHandlerConfigurer {
        public void configure(ExecHandleBuilder builder) {
            if (getExecutable() != null) {
                File exe = new File(getExecutable());
                if (exe.isAbsolute()) {
                    builder.executable(exe.getAbsolutePath());
                } else {
                    builder.executable(String.format("%s/%s", getWorkingDir().getAbsolutePath(), getExecutable()));
                }
            } else {
                builder.executable(String.format("%s/bin/gradle", getDistribution().getGradleHomeDir().getAbsolutePath()));
            }
        }
    }

}
