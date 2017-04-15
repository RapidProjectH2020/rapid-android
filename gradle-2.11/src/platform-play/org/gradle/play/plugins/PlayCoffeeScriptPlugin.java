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

package org.gradle.play.plugins;

import org.gradle.api.*;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.language.base.internal.SourceTransformTaskConfig;
import org.gradle.language.base.internal.registry.LanguageTransform;
import org.gradle.language.base.internal.registry.LanguageTransformContainer;
import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.language.coffeescript.CoffeeScriptSourceSet;
import org.gradle.language.coffeescript.internal.DefaultCoffeeScriptSourceSet;
import org.gradle.language.javascript.JavaScriptSourceSet;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.platform.base.BinarySpec;
import org.gradle.platform.base.BinaryTasks;
import org.gradle.platform.base.LanguageType;
import org.gradle.platform.base.LanguageTypeBuilder;
import org.gradle.play.PlayApplicationSpec;
import org.gradle.play.internal.JavaScriptSourceCode;
import org.gradle.play.internal.PlayApplicationBinarySpecInternal;
import org.gradle.play.tasks.PlayCoffeeScriptCompile;

import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * Plugin for adding coffeescript compilation to a Play application.  Adds support for
 * defining {@link org.gradle.language.coffeescript.CoffeeScriptSourceSet} source sets.  A
 * "coffeeScript" source set is created by default.
 */
@SuppressWarnings("UnusedDeclaration")
@Incubating
public class PlayCoffeeScriptPlugin implements Plugin<Project> {
    private static final String DEFAULT_COFFEESCRIPT_VERSION = "1.8.0";
    private static final String DEFAULT_RHINO_VERSION = "1.7R4";

    static String getDefaultCoffeeScriptDependencyNotation() {
        return String.format("org.coffeescript:coffee-script-js:%s@js", DEFAULT_COFFEESCRIPT_VERSION);
    }

    static String getDefaultRhinoDependencyNotation() {
        return String.format("org.mozilla:rhino:%s", DEFAULT_RHINO_VERSION);
    }

    @Override
    public void apply(Project target) {
        target.getPluginManager().apply(ComponentModelBasePlugin.class);
    }

    static class Rules extends RuleSource {
        @LanguageType
        void registerCoffeeScript(LanguageTypeBuilder<CoffeeScriptSourceSet> builder) {
            builder.setLanguageName("coffeeScript");
            builder.defaultImplementation(DefaultCoffeeScriptSourceSet.class);
        }

        @Mutate
        void createCoffeeScriptSourceSets(ModelMap<PlayApplicationSpec> components) {
            components.afterEach(new Action<PlayApplicationSpec>() {
                @Override
                public void execute(PlayApplicationSpec playComponent) {
                    playComponent.getSources().create("coffeeScript", CoffeeScriptSourceSet.class, new Action<CoffeeScriptSourceSet>() {
                        @Override
                        public void execute(CoffeeScriptSourceSet coffeeScriptSourceSet) {
                            coffeeScriptSourceSet.getSource().srcDir("app/assets");
                            coffeeScriptSourceSet.getSource().include("**/*.coffee");
                        }
                    });
                }
            });
        }

        @Mutate
        void createGeneratedJavaScriptSourceSets(ModelMap<PlayApplicationBinarySpecInternal> binaries, final ServiceRegistry serviceRegistry) {
            final FileResolver fileResolver = serviceRegistry.get(FileResolver.class);
            binaries.all(new Action<PlayApplicationBinarySpecInternal>() {
                @Override
                public void execute(PlayApplicationBinarySpecInternal playApplicationBinarySpec) {
                    for (CoffeeScriptSourceSet coffeeScriptSourceSet : playApplicationBinarySpec.getInputs().withType(CoffeeScriptSourceSet.class)) {
                        playApplicationBinarySpec.addGeneratedJavaScript(coffeeScriptSourceSet, fileResolver);
                    }
                }
            });
        }

        // TODO:DAZ This should not need to be `@BinaryTasks`
        @BinaryTasks
        void configureCoffeeScriptCompileDefaults(ModelMap<Task> tasks, final PlayApplicationBinarySpecInternal binary) {
            tasks.beforeEach(PlayCoffeeScriptCompile.class, new Action<PlayCoffeeScriptCompile>() {
                @Override
                public void execute(PlayCoffeeScriptCompile coffeeScriptCompile) {
                    coffeeScriptCompile.setRhinoClasspathNotation(getDefaultRhinoDependencyNotation());
                    coffeeScriptCompile.setCoffeeScriptJsNotation(getDefaultCoffeeScriptDependencyNotation());
                }
            });
        }

        @Mutate
        void registerLanguageTransform(LanguageTransformContainer languages) {
            languages.add(new CoffeeScript());
        }
    }

    private static class CoffeeScript implements LanguageTransform<CoffeeScriptSourceSet, JavaScriptSourceCode> {
        public Class<CoffeeScriptSourceSet> getSourceSetType() {
            return CoffeeScriptSourceSet.class;
        }

        public Class<JavaScriptSourceCode> getOutputType() {
            return JavaScriptSourceCode.class;
        }

        public Map<String, Class<?>> getBinaryTools() {
            return Collections.emptyMap();
        }

        public SourceTransformTaskConfig getTransformTask() {
            return new SourceTransformTaskConfig() {
                public String getTaskPrefix() {
                    return "compile";
                }

                public Class<? extends DefaultTask> getTaskType() {
                    return PlayCoffeeScriptCompile.class;
                }

                public void configureTask(Task task, BinarySpec binarySpec, LanguageSourceSet sourceSet, ServiceRegistry serviceRegistry) {
                    PlayApplicationBinarySpecInternal binary = (PlayApplicationBinarySpecInternal) binarySpec;
                    CoffeeScriptSourceSet coffeeScriptSourceSet = (CoffeeScriptSourceSet) sourceSet;
                    PlayCoffeeScriptCompile coffeeScriptCompile = (PlayCoffeeScriptCompile) task;
                    JavaScriptSourceSet javaScriptSourceSet = binary.getGeneratedJavaScript().get(coffeeScriptSourceSet);

                    coffeeScriptCompile.setDescription("Compiles coffeescript for the " + coffeeScriptSourceSet.getDisplayName() + ".");

                    File generatedSourceDir = binary.getNamingScheme().getOutputDirectory(task.getProject().getBuildDir(), "src");
                    File outputDirectory = new File(generatedSourceDir, javaScriptSourceSet.getName());
                    coffeeScriptCompile.setDestinationDir(outputDirectory);
                    coffeeScriptCompile.setSource(coffeeScriptSourceSet.getSource());

                    javaScriptSourceSet.getSource().srcDir(outputDirectory);
                    javaScriptSourceSet.builtBy(coffeeScriptCompile);
                }
            };
        }

        public boolean applyToBinary(BinarySpec binary) {
            return binary instanceof PlayApplicationBinarySpecInternal;
        }
    }
}
