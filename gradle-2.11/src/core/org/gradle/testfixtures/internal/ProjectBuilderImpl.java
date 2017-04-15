/*
 * Copyright 2011 the original author or authors.
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

package org.gradle.testfixtures.internal;

import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.gradle.api.internal.AsmBackedClassGenerator;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.TemporaryFileProvider;
import org.gradle.api.internal.file.TmpDirTemporaryFileProvider;
import org.gradle.api.internal.initialization.ClassLoaderScope;
import org.gradle.api.internal.project.DefaultProject;
import org.gradle.api.internal.project.IProjectFactory;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.groovy.scripts.StringScriptSource;
import org.gradle.initialization.DefaultProjectDescriptor;
import org.gradle.initialization.DefaultProjectDescriptorRegistry;
import org.gradle.internal.nativeintegration.services.NativeServices;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.internal.service.ServiceRegistryBuilder;
import org.gradle.internal.service.scopes.ServiceRegistryFactory;
import org.gradle.invocation.DefaultGradle;
import org.gradle.logging.LoggingServiceRegistry;
import org.gradle.util.GFileUtils;

import java.io.File;

public class ProjectBuilderImpl {
    private static ServiceRegistry globalServices;
    private static final AsmBackedClassGenerator CLASS_GENERATOR = new AsmBackedClassGenerator();

    public Project createChildProject(String name, Project parent, File projectDir) {
        ProjectInternal parentProject = (ProjectInternal) parent;
        DefaultProject project = CLASS_GENERATOR.newInstance(
                DefaultProject.class,
                name,
                parentProject,
                (projectDir != null) ? projectDir.getAbsoluteFile() : new File(parentProject.getProjectDir(), name),
                new StringScriptSource("test build file", null),
                parentProject.getGradle(),
                parentProject.getGradle().getServiceRegistryFactory(),
                parentProject.getClassLoaderScope().createChild("project-" + name),
                parentProject.getBaseClassLoaderScope()
        );
        parentProject.addChildProject(project);
        parentProject.getProjectRegistry().addProject(project);
        return project;
    }

    public Project createProject(String name, File inputProjectDir) {
        File projectDir = prepareProjectDir(inputProjectDir);

        final File homeDir = new File(projectDir, "gradleHome");

        StartParameter startParameter = new StartParameter();
        File userHomeDir = new File(projectDir, "userHome");
        startParameter.setGradleUserHomeDir(userHomeDir);

        NativeServices.initialize(userHomeDir);

        ServiceRegistry topLevelRegistry = new TestBuildScopeServices(getGlobalServices(), startParameter, homeDir);
        GradleInternal gradle = CLASS_GENERATOR.newInstance(DefaultGradle.class, null, startParameter, topLevelRegistry.get(ServiceRegistryFactory.class));

        DefaultProjectDescriptor projectDescriptor = new DefaultProjectDescriptor(null, name, projectDir, new DefaultProjectDescriptorRegistry(),
                topLevelRegistry.get(FileResolver.class));
        ClassLoaderScope baseScope = gradle.getClassLoaderScope();
        ClassLoaderScope rootProjectScope = baseScope.createChild("root-project");
        ProjectInternal project = topLevelRegistry.get(IProjectFactory.class).createProject(projectDescriptor, null, gradle, rootProjectScope, baseScope);

        gradle.setRootProject(project);
        gradle.setDefaultProject(project);

        return project;
    }

    private ServiceRegistry getGlobalServices() {
        if (globalServices == null) {
            globalServices = ServiceRegistryBuilder
                    .builder()
                    .displayName("global services")
                    .parent(LoggingServiceRegistry.newNestedLogging())
                    .parent(NativeServices.getInstance())
                    .provider(new TestGlobalScopeServices())
                    .build();
        }
        return globalServices;
    }

    public File prepareProjectDir(File projectDir) {
        if (projectDir == null) {
            TemporaryFileProvider temporaryFileProvider = new TmpDirTemporaryFileProvider();
            projectDir = temporaryFileProvider.createTemporaryDirectory("gradle", "projectDir");
            // TODO deleteOnExit won't clean up non-empty directories (and it leaks memory for long-running processes).
            projectDir.deleteOnExit();
        } else {
            projectDir = GFileUtils.canonicalise(projectDir);
        }
        return projectDir;
    }
}
