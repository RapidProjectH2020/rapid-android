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

import groovy.transform.PackageScope
import org.gradle.api.GradleException
import org.gradle.api.Incubating
import org.gradle.api.JavaVersion
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.quality.internal.FindBugsReportsImpl
import org.gradle.api.plugins.quality.internal.findbugs.*
import org.gradle.api.reporting.Reporting
import org.gradle.api.resources.TextResource
import org.gradle.api.tasks.*
import org.gradle.internal.Factory
import org.gradle.internal.reflect.Instantiator
import org.gradle.logging.ConsoleRenderer
import org.gradle.process.internal.WorkerProcessBuilder

import javax.inject.Inject

/**
 * Analyzes code with <a href="http://findbugs.sourceforge.net">FindBugs</a>. See the
 * <a href="http://findbugs.sourceforge.net/manual/">FindBugs Manual</a> for additional information
 * on configuration options.
 */
class FindBugs extends SourceTask implements VerificationTask, Reporting<FindBugsReports> {
    /**
     * The classes to be analyzed.
     */
    @SkipWhenEmpty
    @InputFiles
    FileCollection classes

    /**
     * Compile class path for the classes to be analyzed.
     * The classes on this class path are used during analysis
     * but aren't analyzed themselves.
     */
    @InputFiles
    FileCollection classpath

    /**
     * Class path holding the FindBugs library.
     */
    @InputFiles
    FileCollection findbugsClasspath

    /**
     * Class path holding any additional FindBugs plugins.
     */
    @InputFiles
    FileCollection pluginClasspath

    /**
     * Whether or not to allow the build to continue if there are warnings.
     */
    boolean ignoreFailures

    /**
     * The analysis effort level. The value specified should be one of {@code min}, {@code default}, or {@code max}.
     * Higher levels increase precision and find more bugs at the expense of running time and memory consumption.
     */
    @Input
    @Optional
    String effort

    /**
     * The priority threshold for reporting bugs. If set to {@code low}, all bugs are reported. If set to
     * {@code medium} (the default), medium and high priority bugs are reported. If set to {@code high},
     * only high priority bugs are reported.
     */
    @Input
    @Optional
    String reportLevel

    /**
     * The maximum heap size for the forked findbugs process (ex: '1g').
     */
    @Input
    @Optional
    String maxHeapSize

    /**
     * The bug detectors which should be run. The bug detectors are specified by their class names,
     * without any package qualification. By default, all detectors which are not disabled by default are run.
     */
    @Input
    @Optional
    Collection<String> visitors = []

    /**
     * Similar to {@code visitors} except that it specifies bug detectors which should not be run.
     * By default, no visitors are omitted.
     */
    @Input
    @Optional
    Collection<String> omitVisitors = []

    /**
     * A filter specifying which bugs are reported. Replaces the {@code includeFilter} property.
     *
     * @since 2.2
     */
    @Incubating
    @Nested
    @Optional
    TextResource includeFilterConfig

    /**
     * A filter specifying bugs to exclude from being reported. Replaces the {@code excludeFilter} property.
     *
     * @since 2.2
     */
    @Incubating
    @Nested
    @Optional
    TextResource excludeFilterConfig

    /**
     * A filter specifying baseline bugs to exclude from being reported.
     */
    @Incubating
    @Nested
    @Optional
    TextResource excludeBugsFilterConfig

    /**
     * Any additional arguments (not covered here more explicitly like {@code effort}) to be passed along to FindBugs.
     * <p>
     * Extra arguments are passed to FindBugs after the arguments Gradle understands (like {@code effort} but before the list of classes to analyze.
     * This should only be used for arguments that cannot be provided by Gradle directly. Gradle does not try to interpret or validate the arguments
     * before passing them to FindBugs.
     * <p>
     * See the <a href="https://code.google.com/p/findbugs/source/browse/findbugs/src/java/edu/umd/cs/findbugs/TextUICommandLine.java">FindBugs TextUICommandLine source</a> for available options.
     *
     * @since 2.6
     */
    @Input
    @Optional
    Collection<String> extraArgs = []

    @Nested
    private final FindBugsReportsImpl reports

    FindBugs() {
        reports = instantiator.newInstance(FindBugsReportsImpl, this)
    }

    @Inject
    Instantiator getInstantiator() {
        throw new UnsupportedOperationException();
    }

    @Inject
    Factory<WorkerProcessBuilder> getWorkerProcessBuilderFactory() {
        throw new UnsupportedOperationException();
    }

    /**
     * The reports to be generated by this task.
     *
     * @return The reports container
     */
    FindBugsReports getReports() {
        reports
    }

    /**
     * Configures the reports to be generated by this task.
     *
     * The contained reports can be configured by name and closures. Example:
     *
     * <pre>
     * findbugsTask {
     *   reports {
     *     xml {
     *       destination "build/findbugs.xml"
     *     }
     *   }
     * }
     * </pre>
     *
     * @param closure The configuration
     * @return The reports container
     */
    FindBugsReports reports(Closure closure) {
        reports.configure(closure)
    }

    /**
     * The filename of a filter specifying which bugs are reported.
     */
    File getIncludeFilter() {
        getIncludeFilterConfig()?.asFile()
    }

    /**
     * The filename of a filter specifying which bugs are reported.
     */
    void setIncludeFilter(File filter) {
        setIncludeFilterConfig(project.resources.text.fromFile(filter))
    }

    /**
     * The filename of a filter specifying bugs to exclude from being reported.
     */
    File getExcludeFilter() {
        getExcludeFilterConfig()?.asFile()
    }

    /**
     * The filename of a filter specifying bugs to exclude from being reported.
     */
    void setExcludeFilter(File filter) {
        setExcludeFilterConfig(project.resources.text.fromFile(filter))
    }

    /**
     * The filename of a filter specifying baseline bugs to exclude from being reported.
     */
    File getExcludeBugsFilter() {
        getExcludeBugsFilterConfig()?.asFile()
    }

    /**
     * The filename of a filter specifying baseline bugs to exclude from being reported.
     */
    void setExcludeBugsFilter(File filter) {
        setExcludeBugsFilterConfig(project.resources.text.fromFile(filter))
    }

    @TaskAction
    void run() {
        new FindBugsClasspathValidator(JavaVersion.current()).validateClasspath(getFindbugsClasspath().files*.name)

        FindBugsSpec spec = generateSpec()
        FindBugsWorkerManager manager = new FindBugsWorkerManager()

        logging.captureStandardOutput(LogLevel.DEBUG)
        logging.captureStandardError(LogLevel.DEBUG)

        FindBugsResult result = manager.runWorker(getProject().getProjectDir(), workerProcessBuilderFactory, getFindbugsClasspath(), spec)
        evaluateResult(result);
    }

    /**
     * For testing only.
     */
    @PackageScope
    FindBugsSpec generateSpec() {
        FindBugsSpecBuilder specBuilder = new FindBugsSpecBuilder(getClasses())
            .withPluginsList(getPluginClasspath())
            .withSources(getSource())
            .withClasspath(getClasspath())
            .withDebugging(logger.isDebugEnabled())
            .withEffort(getEffort())
            .withReportLevel(getReportLevel())
            .withMaxHeapSize(getMaxHeapSize())
            .withVisitors(getVisitors())
            .withOmitVisitors(getOmitVisitors())
            .withExcludeFilter(getExcludeFilter())
            .withIncludeFilter(getIncludeFilter())
            .withExcludeBugsFilter(getExcludeBugsFilter())
            .withExtraArgs(getExtraArgs())
            .configureReports(getReports())

        return specBuilder.build()
    }

    /**
     * For testing only.
     */
    @PackageScope
    void evaluateResult(FindBugsResult result) {
        if (result.exception) {
            throw new GradleException("FindBugs encountered an error. Run with --debug to get more information.", result.exception)
        }
        if (result.errorCount) {
            throw new GradleException("FindBugs encountered an error. Run with --debug to get more information.")
        }
        if (result.bugCount) {
            def message = "FindBugs rule violations were found."
            def report = reports.firstEnabled
            if (report) {
                def reportUrl = new ConsoleRenderer().asClickableFileUrl(report.destination)
                message += " See the report at: $reportUrl"
            }
            if (getIgnoreFailures()) {
                logger.warn(message)
            } else {
                throw new GradleException(message)
            }
        }
    }

    public FindBugs extraArgs(Iterable<String> arguments) {
        for ( String argument : arguments ) {
            extraArgs.add(argument);
        }
        return this;
    }

    public FindBugs extraArgs(String... arguments) {
        extraArgs.addAll( Arrays.asList(arguments) );
        return this;
    }
}
