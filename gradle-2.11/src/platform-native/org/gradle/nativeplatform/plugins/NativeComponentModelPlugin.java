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
package org.gradle.nativeplatform.plugins;

import org.apache.commons.lang.StringUtils;
import org.gradle.api.*;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.DefaultPolymorphicDomainObjectContainer;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.language.base.internal.LanguageSourceSetInternal;
import org.gradle.language.base.internal.SourceTransformTaskConfig;
import org.gradle.language.base.internal.registry.LanguageTransformContainer;
import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.language.nativeplatform.DependentSourceSet;
import org.gradle.language.nativeplatform.HeaderExportingSourceSet;
import org.gradle.language.nativeplatform.internal.DependentSourceSetInternal;
import org.gradle.model.*;
import org.gradle.model.internal.registry.ModelRegistry;
import org.gradle.nativeplatform.*;
import org.gradle.nativeplatform.internal.*;
import org.gradle.nativeplatform.internal.configure.NativeComponentRules;
import org.gradle.nativeplatform.internal.pch.PchEnabledLanguageTransform;
import org.gradle.nativeplatform.internal.prebuilt.DefaultPrebuiltLibraries;
import org.gradle.nativeplatform.internal.prebuilt.PrebuiltLibraryInitializer;
import org.gradle.nativeplatform.platform.NativePlatform;
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform;
import org.gradle.nativeplatform.platform.internal.NativePlatforms;
import org.gradle.nativeplatform.tasks.CreateStaticLibrary;
import org.gradle.nativeplatform.tasks.LinkSharedLibrary;
import org.gradle.nativeplatform.tasks.PrefixHeaderFileGenerateTask;
import org.gradle.nativeplatform.toolchain.internal.DefaultNativeToolChainRegistry;
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainRegistryInternal;
import org.gradle.nativeplatform.toolchain.internal.PreCompiledHeader;
import org.gradle.platform.base.*;
import org.gradle.platform.base.internal.PlatformResolvers;

import javax.inject.Inject;
import java.io.File;

import static org.gradle.model.internal.core.ModelNodes.withType;
import static org.gradle.model.internal.core.NodePredicate.allDescendants;

/**
 * A plugin that sets up the infrastructure for defining native binaries.
 */
@Incubating
public class NativeComponentModelPlugin implements Plugin<ProjectInternal> {
    private final Instantiator instantiator;
    private final ModelRegistry modelRegistry;

    @Inject
    public NativeComponentModelPlugin(ModelRegistry modelRegistry, Instantiator instantiator) {
        this.modelRegistry = modelRegistry;
        this.instantiator = instantiator;
    }

    public void apply(final ProjectInternal project) {
        project.getPluginManager().apply(ComponentModelBasePlugin.class);

        project.getExtensions().create("buildTypes", DefaultBuildTypeContainer.class, instantiator);
        project.getExtensions().create("flavors", DefaultFlavorContainer.class, instantiator);
        project.getExtensions().create("toolChains", DefaultNativeToolChainRegistry.class, instantiator);

        modelRegistry.getRoot().applyTo(allDescendants(withType(NativeComponentSpec.class)), NativeComponentRules.class);
    }

    static class Rules extends RuleSource {
        @ComponentType
        void nativeExecutable(ComponentTypeBuilder<NativeExecutableSpec> builder) {
            builder.defaultImplementation(DefaultNativeExecutableSpec.class);
        }

        @ComponentType
        void nativeLibrary(ComponentTypeBuilder<NativeLibrarySpec> builder) {
            builder.defaultImplementation(DefaultNativeLibrarySpec.class);
        }

        @ComponentType
        void registerTargetedNativeComponent(ComponentTypeBuilder<TargetedNativeComponent> builder) {
            builder.internalView(TargetedNativeComponentInternal.class);
        }

        @Model
        Repositories repositories(ServiceRegistry serviceRegistry, FlavorContainer flavors, PlatformContainer platforms, BuildTypeContainer buildTypes) {
            Instantiator instantiator = serviceRegistry.get(Instantiator.class);
            FileResolver fileResolver = serviceRegistry.get(FileResolver.class);
            NativePlatforms nativePlatforms = serviceRegistry.get(NativePlatforms.class);
            Action<PrebuiltLibrary> initializer = new PrebuiltLibraryInitializer(instantiator, nativePlatforms, platforms.withType(NativePlatform.class), buildTypes, flavors);
            return new DefaultRepositories(instantiator, fileResolver, initializer);
        }

        @Model
        NativeToolChainRegistryInternal toolChains(ExtensionContainer extensionContainer) {
            return extensionContainer.getByType(NativeToolChainRegistryInternal.class);
        }

        @Model
        BuildTypeContainer buildTypes(ExtensionContainer extensionContainer) {
            return extensionContainer.getByType(BuildTypeContainer.class);
        }

        @Model
        FlavorContainer flavors(ExtensionContainer extensionContainer) {
            return extensionContainer.getByType(FlavorContainer.class);
        }

        @Mutate
        public void registerNativePlatformResolver(PlatformResolvers resolvers, ServiceRegistry serviceRegistry) {
            resolvers.register(serviceRegistry.get(NativePlatformResolver.class));
        }

        @Defaults
        public void registerFactoryForCustomNativePlatforms(PlatformContainer platforms, ServiceRegistry serviceRegistry) {
            final Instantiator instantiator = serviceRegistry.get(Instantiator.class);
            NamedDomainObjectFactory<NativePlatform> nativePlatformFactory = new NamedDomainObjectFactory<NativePlatform>() {
                public NativePlatform create(String name) {
                    return instantiator.newInstance(DefaultNativePlatform.class, name);
                }
            };

            platforms.registerFactory(NativePlatform.class, nativePlatformFactory);

            // TODO:DAZ This is only here for backward compatibility: platforms should be typed on creation, I think.
            platforms.registerFactory(Platform.class, nativePlatformFactory);
        }

        @BinaryType
        void registerSharedLibraryBinaryType(BinaryTypeBuilder<SharedLibraryBinarySpec> builder) {
            builder.defaultImplementation(DefaultSharedLibraryBinarySpec.class);
            builder.internalView(SharedLibraryBinarySpecInternal.class);
        }

        @BinaryType
        void registerStaticLibraryBinaryType(BinaryTypeBuilder<StaticLibraryBinarySpec> builder) {
            builder.defaultImplementation(DefaultStaticLibraryBinarySpec.class);
            builder.internalView(StaticLibraryBinarySpecInternal.class);
        }

        @BinaryType
        void registerNativeExecutableBinaryType(BinaryTypeBuilder<NativeExecutableBinarySpec> builder) {
            builder.defaultImplementation(DefaultNativeExecutableBinarySpec.class);
            builder.internalView(NativeExecutableBinarySpecInternal.class);
        }

        @Finalize
        public void createDefaultToolChain(NativeToolChainRegistryInternal toolChains) {
            if (toolChains.isEmpty()) {
                toolChains.addDefaultToolChains();
            }
        }

        @Finalize
        public void createDefaultBuildTypes(BuildTypeContainer buildTypes) {
            if (buildTypes.isEmpty()) {
                buildTypes.create("debug");
            }
        }

        @Finalize
        public void createDefaultFlavor(FlavorContainer flavors) {
            if (flavors.isEmpty()) {
                flavors.create(DefaultFlavor.DEFAULT);
            }
        }

        @Mutate
        void configureGeneratedSourceSets(ModelMap<NativeComponentSpec> componentSpecs) {
            componentSpecs.afterEach(new Action<NativeComponentSpec>() {
                @Override
                public void execute(NativeComponentSpec componentSpec) {
                    componentSpec.getSources().withType(LanguageSourceSetInternal.class).afterEach(new Action<LanguageSourceSetInternal>() {
                        @Override
                        public void execute(LanguageSourceSetInternal languageSourceSet) {
                            Task generatorTask = languageSourceSet.getGeneratorTask();
                            if (generatorTask != null) {
                                languageSourceSet.builtBy(generatorTask);
                                maybeSetSourceDir(languageSourceSet.getSource(), generatorTask, "sourceDir");
                                if (languageSourceSet instanceof HeaderExportingSourceSet) {
                                    maybeSetSourceDir(((HeaderExportingSourceSet) languageSourceSet).getExportedHeaders(), generatorTask, "headerDir");
                                }
                            }
                        }
                    });
                }
            });
        }

        @Mutate
        void configurePrefixHeaderFiles(ModelMap<NativeComponentSpec> componentSpecs, final @Path("buildDir") File buildDir) {
            componentSpecs.afterEach(new Action<NativeComponentSpec>() {
                @Override
                public void execute(final NativeComponentSpec componentSpec) {
                    componentSpec.getSources().withType(DependentSourceSetInternal.class).afterEach(new Action<DependentSourceSetInternal>() {
                        @Override
                        public void execute(DependentSourceSetInternal dependentSourceSet) {
                            if (dependentSourceSet.getPreCompiledHeader() != null) {
                                String prefixHeaderDirName = String.format("tmp/%s/%s/prefixHeaders", componentSpec.getName(), dependentSourceSet.getName());
                                File prefixHeaderDir = new File(buildDir, prefixHeaderDirName);
                                File prefixHeaderFile = new File(prefixHeaderDir, "prefix-headers.h");
                                dependentSourceSet.setPrefixHeaderFile(prefixHeaderFile);
                            }
                        }
                    });
                }
            });
        }

        @Mutate
        void configurePrefixHeaderGenerationTasks(final TaskContainer tasks, ModelMap<NativeComponentSpec> nativeComponents) {
            for (final NativeComponentSpec nativeComponentSpec : nativeComponents.values()) {
                for (final DependentSourceSetInternal dependentSourceSet : nativeComponentSpec.getSources().withType(DependentSourceSetInternal.class).values()) {
                    if (dependentSourceSet.getPrefixHeaderFile() != null) {
                        String taskName = String.format("generate%s%sPrefixHeaderFile", StringUtils.capitalize(nativeComponentSpec.getName()), StringUtils.capitalize(dependentSourceSet.getName()));
                        tasks.create(taskName, PrefixHeaderFileGenerateTask.class, new Action<PrefixHeaderFileGenerateTask>() {
                            @Override
                            public void execute(PrefixHeaderFileGenerateTask prefixHeaderFileGenerateTask) {
                                prefixHeaderFileGenerateTask.setPrefixHeaderFile(dependentSourceSet.getPrefixHeaderFile());
                                prefixHeaderFileGenerateTask.setHeader(dependentSourceSet.getPreCompiledHeader());
                            }
                        });
                    }
                }
            }
        }

        @Mutate
        void configurePreCompiledHeaderCompileTasks(final TaskContainer tasks, ModelMap<NativeBinarySpecInternal> binaries, final LanguageTransformContainer languageTransforms, final ServiceRegistry serviceRegistry) {
            for (final NativeBinarySpecInternal nativeBinarySpec : binaries.values()) {
                for (final PchEnabledLanguageTransform<?> transform : languageTransforms.withType(PchEnabledLanguageTransform.class)) {
                    nativeBinarySpec.getInputs().withType(transform.getSourceSetType(), new Action<LanguageSourceSet>() {
                        @Override
                        public void execute(final LanguageSourceSet languageSourceSet) {
                            final DependentSourceSet dependentSourceSet = (DependentSourceSet) languageSourceSet;
                            if (dependentSourceSet.getPreCompiledHeader() != null) {
                                nativeBinarySpec.getPrefixFileToPCH().put(((DependentSourceSetInternal) dependentSourceSet).getPrefixHeaderFile(), new PreCompiledHeader());
                                final SourceTransformTaskConfig pchTransformTaskConfig = transform.getPchTransformTask();
                                String pchTaskName = String.format("%s%s%sPreCompiledHeader", pchTransformTaskConfig.getTaskPrefix(), StringUtils.capitalize(nativeBinarySpec.getProjectScopedName()), StringUtils.capitalize(dependentSourceSet.getName()));
                                Task pchTask = tasks.create(pchTaskName, pchTransformTaskConfig.getTaskType(), new Action<DefaultTask>() {
                                    @Override
                                    public void execute(DefaultTask task) {
                                        pchTransformTaskConfig.configureTask(task, nativeBinarySpec, dependentSourceSet, serviceRegistry);
                                    }
                                });
                                nativeBinarySpec.getTasks().add(pchTask);
                            }
                        }
                    });
                }
            }
        }

        private void maybeSetSourceDir(SourceDirectorySet sourceSet, Task task, String propertyName) {
            Object value = task.property(propertyName);
            if (value != null) {
                sourceSet.srcDir(value);
            }
        }

        @BinaryTasks
        public void sharedLibraryTasks(ModelMap<Task> tasks, final SharedLibraryBinarySpecInternal binary) {
            String taskName = binary.getNamingScheme().getTaskName("link");
            tasks.create(taskName, LinkSharedLibrary.class, new Action<LinkSharedLibrary>() {
                @Override
                public void execute(LinkSharedLibrary linkTask) {
                    linkTask.setDescription("Links " + binary.getDisplayName());
                    linkTask.setToolChain(binary.getToolChain());
                    linkTask.setTargetPlatform(binary.getTargetPlatform());
                    linkTask.setOutputFile(binary.getSharedLibraryFile());
                    linkTask.setInstallName(binary.getSharedLibraryFile().getName());
                    linkTask.setLinkerArgs(binary.getLinker().getArgs());

                    linkTask.lib(new NativeComponents.BinaryLibs(binary) {
                        @Override
                        protected FileCollection getFiles(NativeDependencySet nativeDependencySet) {
                            return nativeDependencySet.getLinkFiles();
                        }
                    });
                }
            });
        }

        @BinaryTasks
        public void staticLibraryTasks(ModelMap<Task> tasks, final StaticLibraryBinarySpecInternal binary) {
            String taskName = binary.getNamingScheme().getTaskName("create");
            tasks.create(taskName, CreateStaticLibrary.class, new Action<CreateStaticLibrary>() {
                @Override
                public void execute(CreateStaticLibrary task) {
                    task.setDescription("Creates " + binary.getDisplayName());
                    task.setToolChain(binary.getToolChain());
                    task.setTargetPlatform(binary.getTargetPlatform());
                    task.setOutputFile(binary.getStaticLibraryFile());
                    task.setStaticLibArgs(binary.getStaticLibArchiver().getArgs());
                }
            });
        }

        @BinaryTasks
        public void executableTasks(ModelMap<Task> tasks, final NativeExecutableBinarySpecInternal executableBinary) {
            NativeComponents.createExecutableTask(executableBinary, executableBinary.getExecutable().getFile());
        }

        /**
         * Can't use @BinaryTasks because the binary is not _built-by_ the install task, but it is associated with it. Rule is called multiple times, so need to check for task existence before
         * creating.
         */
        @Defaults
        void createInstallTasks(ModelMap<Task> tasks, ModelMap<NativeBinarySpecInternal> binaries) {
            for (NativeExecutableBinarySpecInternal binary : binaries.withType(NativeExecutableBinarySpecInternal.class).values()) {
                NativeComponents.createInstallTask(binary, binary.getInstallation(), binary.getExecutable(), binary.getNamingScheme());
            }
        }
    }

    private static class DefaultRepositories extends DefaultPolymorphicDomainObjectContainer<ArtifactRepository> implements Repositories {
        private DefaultRepositories(final Instantiator instantiator, final FileResolver fileResolver, final Action<PrebuiltLibrary> binaryFactory) {
            super(ArtifactRepository.class, instantiator, new ArtifactRepositoryNamer());
            registerFactory(PrebuiltLibraries.class, new NamedDomainObjectFactory<PrebuiltLibraries>() {
                public PrebuiltLibraries create(String name) {
                    return instantiator.newInstance(DefaultPrebuiltLibraries.class, name, instantiator, fileResolver, binaryFactory);
                }
            });
        }
    }

    private static class ArtifactRepositoryNamer implements Namer<ArtifactRepository> {
        public String determineName(ArtifactRepository object) {
            return object.getName();
        }
    }

}
