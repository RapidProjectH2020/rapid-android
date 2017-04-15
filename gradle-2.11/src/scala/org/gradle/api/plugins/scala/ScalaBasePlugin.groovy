/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.api.plugins.scala
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.tasks.DefaultScalaSourceSet
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.ScalaRuntime
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.scala.ScalaDoc
import org.gradle.language.scala.internal.toolchain.DefaultScalaToolProvider

import javax.inject.Inject

class ScalaBasePlugin implements Plugin<Project> {
    static final String ZINC_CONFIGURATION_NAME = "zinc"

    static final String SCALA_RUNTIME_EXTENSION_NAME = "scalaRuntime"

    private final FileResolver fileResolver

    private Project project
    private ScalaRuntime scalaRuntime

    @Inject
    ScalaBasePlugin(FileResolver fileResolver) {
        this.fileResolver = fileResolver
    }

    void apply(Project project) {
        this.project = project
        project.pluginManager.apply(JavaBasePlugin)
        def javaPlugin = project.plugins.getPlugin(JavaBasePlugin.class)

        configureConfigurations(project)
        configureScalaRuntimeExtension()
        configureCompileDefaults()
        configureSourceSetDefaults(javaPlugin)
        configureScaladoc()
    }

    private void configureConfigurations(Project project) {
        project.configurations.create(ZINC_CONFIGURATION_NAME)
                .setVisible(false)
                .setDescription("The Zinc incremental compiler to be used for this Scala project.")
    }

    private void configureScalaRuntimeExtension() {
        scalaRuntime = project.extensions.create(SCALA_RUNTIME_EXTENSION_NAME, ScalaRuntime, project)
    }

    private void configureSourceSetDefaults(JavaBasePlugin javaPlugin) {
        project.convention.getPlugin(JavaPluginConvention.class).sourceSets.all { SourceSet sourceSet ->
            sourceSet.convention.plugins.scala = new DefaultScalaSourceSet(sourceSet.displayName, fileResolver)
            sourceSet.scala.srcDir { project.file("src/$sourceSet.name/scala") }
            sourceSet.allJava.source(sourceSet.scala)
            sourceSet.allSource.source(sourceSet.scala)
            sourceSet.resources.filter.exclude { FileTreeElement element -> sourceSet.scala.contains(element.file) }

            configureScalaCompile(javaPlugin, sourceSet)
        }
    }

    private void configureScalaCompile(JavaBasePlugin javaPlugin, SourceSet sourceSet) {
        def taskName = sourceSet.getCompileTaskName('scala')
        def scalaCompile = project.tasks.create(taskName, ScalaCompile)
        scalaCompile.dependsOn sourceSet.compileJavaTaskName
        javaPlugin.configureForSourceSet(sourceSet, scalaCompile)
        scalaCompile.description = "Compiles the $sourceSet.scala."
        scalaCompile.source = sourceSet.scala
        project.tasks[sourceSet.classesTaskName].dependsOn(taskName)

        // cannot use convention mapping because the resulting object won't be serializable
        // cannot compute at task execution time because we need association with source set
        project.gradle.projectsEvaluated {
            scalaCompile.scalaCompileOptions.incrementalOptions.with {
                if (!analysisFile) {
                    analysisFile = new File("$project.buildDir/tmp/scala/compilerAnalysis/${scalaCompile.name}.analysis")
                }
                if (!publishedCode) {
                    def jarTask = project.tasks.findByName(sourceSet.getJarTaskName())
                    publishedCode = jarTask?.archivePath
                }
            }
        }
    }

    private void configureCompileDefaults() {
        project.tasks.withType(ScalaCompile.class) { ScalaCompile compile ->
            compile.conventionMapping.scalaClasspath = { scalaRuntime.inferScalaClasspath(compile.classpath) }
            compile.conventionMapping.zincClasspath = {
                def config = project.configurations[ZINC_CONFIGURATION_NAME]
                if (!compile.scalaCompileOptions.useAnt && config.dependencies.empty) {
                    project.dependencies {
                        zinc("com.typesafe.zinc:zinc:$DefaultScalaToolProvider.DEFAULT_ZINC_VERSION")
                    }
                }
                config
            }
        }
    }

    private void configureScaladoc() {
        project.tasks.withType(ScalaDoc) { ScalaDoc scalaDoc ->
            scalaDoc.conventionMapping.destinationDir = { project.file("$project.docsDir/scaladoc") }
            scalaDoc.conventionMapping.title = { project.extensions.getByType(ReportingExtension).apiDocTitle }
            scalaDoc.conventionMapping.scalaClasspath = { scalaRuntime.inferScalaClasspath(scalaDoc.classpath) }
        }
    }
}