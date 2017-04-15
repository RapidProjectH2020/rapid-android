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

package org.gradle.launcher.cli;

import org.gradle.StartParameter;
import org.gradle.api.internal.DocumentationRegistry;
import org.gradle.cli.CommandLineConverter;
import org.gradle.cli.CommandLineParser;
import org.gradle.cli.ParsedCommandLine;
import org.gradle.configuration.GradleLauncherMetaData;
import org.gradle.internal.SystemProperties;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.nativeintegration.services.NativeServices;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.internal.service.ServiceRegistryBuilder;
import org.gradle.internal.service.scopes.GlobalScopeServices;
import org.gradle.launcher.daemon.bootstrap.ForegroundDaemonAction;
import org.gradle.launcher.daemon.client.*;
import org.gradle.launcher.daemon.configuration.CurrentProcess;
import org.gradle.launcher.daemon.configuration.DaemonParameters;
import org.gradle.launcher.daemon.configuration.ForegroundDaemonConfiguration;
import org.gradle.launcher.exec.*;
import org.gradle.logging.StyledTextOutputFactory;
import org.gradle.logging.internal.OutputEventListener;

import java.lang.management.ManagementFactory;

class BuildActionsFactory implements CommandLineAction {
    private final CommandLineConverter<Parameters> parametersConverter;
    private final ServiceRegistry loggingServices;

    BuildActionsFactory(ServiceRegistry loggingServices, CommandLineConverter<Parameters> parametersConverter) {
        this.loggingServices = loggingServices;
        this.parametersConverter = parametersConverter;
    }

    public void configureCommandLineParser(CommandLineParser parser) {
        parametersConverter.configure(parser);
    }

    public Runnable createAction(CommandLineParser parser, ParsedCommandLine commandLine) {
        Parameters parameters = parametersConverter.convert(commandLine, new Parameters());
        parameters.getDaemonParameters().applyDefaultsFor(new JvmVersionDetector().getJavaVersion(parameters.getDaemonParameters().getEffectiveJvm()));

        if (parameters.getDaemonParameters().isStop()) {
            return stopAllDaemons(parameters.getDaemonParameters(), loggingServices);
        }
        if (parameters.getDaemonParameters().isForeground()) {
            DaemonParameters daemonParameters = parameters.getDaemonParameters();
            ForegroundDaemonConfiguration conf = new ForegroundDaemonConfiguration(
                    daemonParameters.getUid(), daemonParameters.getBaseDir(), daemonParameters.getIdleTimeout());
            return new ForegroundDaemonAction(loggingServices, conf);
        }
        if (parameters.getDaemonParameters().getDaemonUsage().isEnabled()) {
            return runBuildWithDaemon(parameters.getStartParameter(), parameters.getDaemonParameters(), loggingServices);
        }
        if (canUseCurrentProcess(parameters.getDaemonParameters())) {
            return runBuildInProcess(parameters.getStartParameter(), parameters.getDaemonParameters(), loggingServices);
        }

        return runBuildInSingleUseDaemon(parameters.getStartParameter(), parameters.getDaemonParameters(), loggingServices);
    }

    private Runnable stopAllDaemons(DaemonParameters daemonParameters, ServiceRegistry loggingServices) {
        ServiceRegistry clientSharedServices = createGlobalClientServices();
        ServiceRegistry clientServices = clientSharedServices.get(DaemonClientFactory.class).createStopDaemonServices(loggingServices.get(OutputEventListener.class), daemonParameters);
        DaemonStopClient stopClient = clientServices.get(DaemonStopClient.class);
        return new StopDaemonAction(stopClient);
    }

    private Runnable runBuildWithDaemon(StartParameter startParameter, DaemonParameters daemonParameters, ServiceRegistry loggingServices) {
        // Create a client that will match based on the daemon startup parameters.
        ServiceRegistry clientSharedServices = createGlobalClientServices();
        ServiceRegistry clientServices = clientSharedServices.get(DaemonClientFactory.class).createBuildClientServices(loggingServices.get(OutputEventListener.class), daemonParameters, System.in);
        DaemonClient client = clientServices.get(DaemonClient.class);
        return runBuild(startParameter, daemonParameters, client, clientSharedServices);
    }

    private boolean canUseCurrentProcess(DaemonParameters requiredBuildParameters) {
        CurrentProcess currentProcess = new CurrentProcess();
        return currentProcess.configureForBuild(requiredBuildParameters);
    }

    private Runnable runBuildInProcess(StartParameter startParameter, DaemonParameters daemonParameters, ServiceRegistry loggingServices) {
        ServiceRegistry globalServices = ServiceRegistryBuilder.builder()
                .displayName("Global services")
                .parent(loggingServices)
                .parent(NativeServices.getInstance())
                .provider(new GlobalScopeServices(startParameter.isContinuous()))
                .build();

        BuildActionExecuter<BuildActionParameters> executer = globalServices.get(BuildExecuter.class);
        StyledTextOutputFactory textOutputFactory = globalServices.get(StyledTextOutputFactory.class);
        DocumentationRegistry documentationRegistry = globalServices.get(DocumentationRegistry.class);
        DaemonUsageSuggestingBuildActionExecuter daemonUsageSuggestingExecuter = new DaemonUsageSuggestingBuildActionExecuter(executer, textOutputFactory, documentationRegistry);

        return runBuild(startParameter, daemonParameters, daemonUsageSuggestingExecuter, globalServices);
    }

    private Runnable runBuildInSingleUseDaemon(StartParameter startParameter, DaemonParameters daemonParameters, ServiceRegistry loggingServices) {
        //(SF) this is a workaround until this story is completed. I'm hardcoding setting the idle timeout to be max X mins.
        //this way we avoid potential runaway daemons that steal resources on linux and break builds on windows.
        //We might leave that in if we decide it's a good idea for an extra safety net.
        int maxTimeout = 2 * 60 * 1000;
        if (daemonParameters.getIdleTimeout() > maxTimeout) {
            daemonParameters.setIdleTimeout(maxTimeout);
        }
        //end of workaround.

        // Create a client that will not match any existing daemons, so it will always startup a new one
        ServiceRegistry clientSharedServices = createGlobalClientServices();
        ServiceRegistry clientServices = clientSharedServices.get(DaemonClientFactory.class).createSingleUseDaemonClientServices(loggingServices.get(OutputEventListener.class), daemonParameters, System.in);
        DaemonClient client = clientServices.get(DaemonClient.class);
        return runBuild(startParameter, daemonParameters, client, clientSharedServices);
    }

    private ServiceRegistry createGlobalClientServices() {
        return ServiceRegistryBuilder.builder()
                .displayName("Daemon client global services")
                .parent(NativeServices.getInstance())
                .provider(new GlobalScopeServices(false))
                .provider(new DaemonClientGlobalServices())
                .build();
    }

    private Runnable runBuild(StartParameter startParameter, DaemonParameters daemonParameters, BuildActionExecuter<BuildActionParameters> executer, ServiceRegistry sharedServices) {
        BuildActionParameters parameters = new DefaultBuildActionParameters(
                daemonParameters.getEffectiveSystemProperties(),
                System.getenv(),
                SystemProperties.getInstance().getCurrentDir(),
                startParameter.getLogLevel(),
                daemonParameters.getDaemonUsage(), startParameter.isContinuous(), daemonParameters.isInteractive(), ClassPath.EMPTY);
        return new RunBuildAction(executer, startParameter, clientMetaData(), getBuildStartTime(), parameters, sharedServices);
    }

    private long getBuildStartTime() {
        return ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    private GradleLauncherMetaData clientMetaData() {
        return new GradleLauncherMetaData();
    }
}
