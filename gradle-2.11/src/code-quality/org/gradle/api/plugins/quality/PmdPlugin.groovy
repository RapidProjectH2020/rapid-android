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
package org.gradle.api.plugins.quality

import org.gradle.api.JavaVersion
import org.gradle.api.plugins.quality.internal.AbstractCodeQualityPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.util.VersionNumber

/**
 *  A plugin for the <a href="http://pmd.sourceforge.net/">PMD</a> source code analyzer.
 * <p>
 * Declares a <tt>pmd</tt> configuration which needs to be configured with the PMD library to be used.
 * <p>
 * For each source set that is to be analyzed, a {@link Pmd} task is created and configured to analyze all Java code.
 * <p
 * All PMD tasks (including user-defined ones) are added to the <tt>check</tt> lifecycle task.
 *
 * @see PmdExtension
 * @see Pmd
 */
class PmdPlugin extends AbstractCodeQualityPlugin<Pmd> {
    public static final String DEFAULT_PMD_VERSION = "5.2.3"
    private PmdExtension extension

    @Override
    protected String getToolName() {
        return "PMD"
    }

    @Override
    protected Class<Pmd> getTaskType() {
        return Pmd
    }

    @Override
    protected CodeQualityExtension createExtension() {
        extension = project.extensions.create("pmd", PmdExtension, project)
        extension.with {
            toolVersion = DEFAULT_PMD_VERSION
            ruleSets = ["java-basic"]
            ruleSetFiles = project.files()
        }
        extension.getConventionMapping().with{
            targetJdk = { getDefaultTargetJdk(project.sourceCompatibility) }
        }
        return extension
    }

    TargetJdk getDefaultTargetJdk(JavaVersion javaVersion) {
        try {
            return TargetJdk.toVersion(javaVersion.toString())
        } catch(IllegalArgumentException ignored) {
            // TargetJDK does not include 1.1, 1.2 and 1.8;
            // Use same fallback as PMD
            return TargetJdk.VERSION_1_4
        }
    }

    @Override
    protected void configureTaskDefaults(Pmd task, String baseName) {
        def config = project.configurations['pmd']
        config.defaultDependencies { dependencies ->
            VersionNumber version = VersionNumber.parse(this.extension.toolVersion)
            String dependency = calculateDefaultDependencyNotation(version)
            dependencies.add(this.project.dependencies.create(dependency))
        }
        task.conventionMapping.with {
            pmdClasspath = { config }
            ruleSets = { extension.ruleSets }
            ruleSetConfig = { extension.ruleSetConfig }
            ruleSetFiles = { extension.ruleSetFiles }
            ignoreFailures = { extension.ignoreFailures }
            rulePriority = { extension.rulePriority }
            consoleOutput = { extension.consoleOutput }
            targetJdk = { extension.targetJdk }
            task.reports.all { report ->
                report.conventionMapping.with {
                    enabled = { true }
                    destination = { new File(extension.reportsDir, "${baseName}.${report.name}") }
                }
            }
        }
    }

    private String calculateDefaultDependencyNotation(VersionNumber toolVersion) {
        if (toolVersion < VersionNumber.version(5)) {
            return "pmd:pmd:$extension.toolVersion"
        } else if (toolVersion < VersionNumber.parse("5.2.0")) {
            return "net.sourceforge.pmd:pmd:$extension.toolVersion"
        }
        return "net.sourceforge.pmd:pmd-java:$extension.toolVersion"
    }

    @Override
    protected void configureForSourceSet(SourceSet sourceSet, Pmd task) {
        task.with {
            description = "Run PMD analysis for ${sourceSet.name} classes"
        }
        task.setSource(sourceSet.allJava)
        task.conventionMapping.with {
            classpath = { sourceSet.compileClasspath }
        }
    }
}
