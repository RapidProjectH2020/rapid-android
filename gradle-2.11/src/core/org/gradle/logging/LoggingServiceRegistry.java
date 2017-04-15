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

package org.gradle.logging;

import org.gradle.cli.CommandLineConverter;
import org.gradle.internal.Actions;
import org.gradle.internal.TimeProvider;
import org.gradle.internal.TrueTimeProvider;
import org.gradle.internal.service.DefaultServiceRegistry;
import org.gradle.logging.internal.*;
import org.gradle.logging.internal.slf4j.Slf4jLoggingConfigurer;

/**
 * A {@link org.gradle.internal.service.ServiceRegistry} implementation that provides the logging services. To use this:
 *
 * <ol>
 *     <li>Create an instance using one of the static factory methods below.</li>
 *     <li>Create an instance of {@link LoggingManagerInternal}.</li>
 *     <li>Configure the logging manager as appropriate.</li>
 *     <li>Start the logging manager using {@link org.gradle.logging.LoggingManagerInternal#start()}.</li>
 *     <li>When finished, stop the logging manager using {@link LoggingManagerInternal#stop()}.</li>
 * </ol>
 */
public abstract class LoggingServiceRegistry extends DefaultServiceRegistry {
    private TextStreamOutputEventListener stdoutListener;

    /**
     * Creates a set of logging services which are suitable to use globally in a process. In particular:
     *
     * <ul>
     *     <li>Replaces System.out and System.err with implementations that route output through the logging system as per {@link LoggingManagerInternal#captureSystemSources()}.</li>
     *     <li>Configures slf4j, log4j and java util logging to route log messages through the logging system.</li>
     *     <li>Routes logging output to the original System.out and System.err as per {@link LoggingManagerInternal#attachSystemOutAndErr()}.</li>
     *     <li>Sets log level to {@link org.gradle.api.logging.LogLevel#LIFECYCLE}.</li>
     * </ul>
     *
     * <p>Does nothing until started.</p>
     *
     * <p>Allows dynamic and colored output to be written to the console. Use {@link LoggingManagerInternal#attachProcessConsole(ConsoleOutput)} to enable this.</p>
     */
    public static LoggingServiceRegistry newCommandLineProcessLogging() {
        CommandLineLogging loggingServices = new CommandLineLogging();
        LoggingManagerInternal rootLoggingManager = loggingServices.get(DefaultLoggingManagerFactory.class).getRoot();
        rootLoggingManager.captureSystemSources();
        rootLoggingManager.attachSystemOutAndErr();
        return loggingServices;
    }

    /**
     * Creates a set of logging services which are suitable to use embedded in another application. In particular:
     *
     * <ul>
     *     <li>Configures slf4j and log4j to route log messages through the logging system.</li>
     *     <li>Sets log level to {@link org.gradle.api.logging.LogLevel#LIFECYCLE}.</li>
     * </ul>
     *
     * <p>Does not:</p>
     *
     * <ul>
     *     <li>Replace System.out and System.err to capture output written to these destinations. Use {@link LoggingManagerInternal#captureSystemSources()} to enable this.</li>
     *     <li>Configure java util logging. Use {@link LoggingManagerInternal#captureSystemSources()} to enable this.</li>
     *     <li>Route logging output to the original System.out and System.err. Use {@link LoggingManagerInternal#attachSystemOutAndErr()} to enable this.</li>
     * </ul>
     *
     * <p>Does nothing until started.</p>
     */
    public static LoggingServiceRegistry newEmbeddableLogging() {
        return new CommandLineLogging();
    }

    /**
     * Creates a set of logging services to set up a new logging scope that does nothing by default. The methods on {@link LoggingManagerInternal} can be used to configure the
     * logging services do useful things.
     *
     * <p>Sets log level to {@link org.gradle.api.logging.LogLevel#LIFECYCLE}.</p>
     */
    public static LoggingServiceRegistry newNestedLogging() {
        return new NestedLogging();
    }

    protected CommandLineConverter<LoggingConfiguration> createCommandLineConverter() {
        return new LoggingCommandLineConverter();
    }

    protected TimeProvider createTimeProvider() {
        return new TrueTimeProvider();
    }

    protected StyledTextOutputFactory createStyledTextOutputFactory() {
        return new DefaultStyledTextOutputFactory(getStdoutListener(), get(TimeProvider.class));
    }

    protected TextStreamOutputEventListener getStdoutListener() {
        if (stdoutListener == null) {
            stdoutListener = new TextStreamOutputEventListener(get(OutputEventListener.class));
        }
        return stdoutListener;
    }

    protected ProgressLoggerFactory createProgressLoggerFactory() {
        return new DefaultProgressLoggerFactory(new ProgressLoggingBridge(get(OutputEventListener.class)), get(TimeProvider.class));
    }

    protected abstract DefaultLoggingManagerFactory createLoggingManagerFactory();

    protected OutputEventRenderer createOutputEventRenderer() {
        return new OutputEventRenderer(Actions.doNothing());
    }

    private static class CommandLineLogging extends LoggingServiceRegistry {
        protected DefaultLoggingManagerFactory createLoggingManagerFactory() {
            OutputEventRenderer renderer = get(OutputEventRenderer.class);
            LoggingSystem stdout = new DefaultStdOutLoggingSystem(getStdoutListener(), get(TimeProvider.class));
            LoggingSystem stderr = new DefaultStdErrLoggingSystem(new TextStreamOutputEventListener(get(OutputEventListener.class)), get(TimeProvider.class));
            return new DefaultLoggingManagerFactory(
                    new DefaultLoggingConfigurer(renderer,
                            new Slf4jLoggingConfigurer(renderer)),
                    renderer,
                    new JavaUtilLoggingSystem(),
                    stdout,
                    stderr);
        }

        protected OutputEventRenderer createOutputEventRenderer() {
            return new OutputEventRenderer(new ConsoleConfigureAction());
        }
    }

    private static class NestedLogging extends LoggingServiceRegistry {
        protected DefaultLoggingManagerFactory createLoggingManagerFactory() {
            OutputEventRenderer renderer = get(OutputEventRenderer.class);
            // Don't configure anything
            return new DefaultLoggingManagerFactory(renderer,
                    renderer,
                    new NoOpLoggingSystem(),
                    new NoOpLoggingSystem(),
                    new NoOpLoggingSystem());
        }
    }
}
