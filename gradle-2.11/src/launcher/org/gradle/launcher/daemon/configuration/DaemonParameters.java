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
package org.gradle.launcher.daemon.configuration;

import com.google.common.collect.ImmutableList;
import org.gradle.api.JavaVersion;
import org.gradle.api.Nullable;
import org.gradle.api.internal.file.IdentityFileResolver;
import org.gradle.initialization.BuildLayoutParameters;
import org.gradle.internal.jvm.JavaInfo;
import org.gradle.internal.jvm.Jvm;
import org.gradle.process.internal.JvmOptions;
import org.gradle.util.GUtil;

import java.io.File;
import java.util.*;

public class DaemonParameters {
    static final int DEFAULT_IDLE_TIMEOUT = 3 * 60 * 60 * 1000;

    public static final List<String> DEFAULT_JVM_ARGS = ImmutableList.of("-Xmx1024m", "-XX:MaxPermSize=256m", "-XX:+HeapDumpOnOutOfMemoryError");
    public static final List<String> DEFAULT_JVM_9_ARGS = ImmutableList.of("-Xmx1024m", "-XX:+HeapDumpOnOutOfMemoryError");
    public static final String INTERACTIVE_TOGGLE = "org.gradle.interactive";

    private final String uid;
    private final File gradleUserHomeDir;

    private File baseDir;
    private int idleTimeout = DEFAULT_IDLE_TIMEOUT;
    private final JvmOptions jvmOptions = new JvmOptions(new IdentityFileResolver());
    private DaemonUsage daemonUsage = DaemonUsage.IMPLICITLY_DISABLED;
    private boolean hasJvmArgs;
    private boolean foreground;
    private boolean stop;
    private boolean interactive = System.console() != null || Boolean.getBoolean(INTERACTIVE_TOGGLE);
    private JavaInfo jvm = Jvm.current();

    public DaemonParameters(BuildLayoutParameters layout) {
        this(layout, Collections.<String, String>emptyMap());
    }

    public DaemonParameters(BuildLayoutParameters layout, Map<String, String> extraSystemProperties) {
        this.uid = UUID.randomUUID().toString();
        jvmOptions.systemProperties(extraSystemProperties);
        baseDir = new File(layout.getGradleUserHomeDir(), "daemon");
        gradleUserHomeDir = layout.getGradleUserHomeDir();
    }

    public boolean isInteractive() {
        return interactive;
    }

    public DaemonParameters setEnabled(boolean enabled) {
        daemonUsage = enabled ? DaemonUsage.EXPLICITLY_ENABLED : DaemonUsage.EXPLICITLY_DISABLED;
        return this;
    }

    public String getUid() {
        return uid;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public File getGradleUserHomeDir() {
        return gradleUserHomeDir;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public List<String> getEffectiveJvmArgs() {
        return jvmOptions.getAllImmutableJvmArgs();
    }

    public JavaInfo getEffectiveJvm() {
        return jvm;
    }

    @Nullable
    public DaemonParameters setJvm(JavaInfo jvm) {
        this.jvm = jvm == null ? Jvm.current() : jvm;
        return this;
    }

    public void applyDefaultsFor(JavaVersion javaVersion) {
        if (hasJvmArgs) {
            return;
        }
        if (javaVersion.compareTo(JavaVersion.VERSION_1_9) >= 0) {
            jvmOptions.jvmArgs(DEFAULT_JVM_9_ARGS);
        } else {
            jvmOptions.jvmArgs(DEFAULT_JVM_ARGS);
        }
    }

    public Map<String, String> getSystemProperties() {
        Map<String, String> systemProperties = new HashMap<String, String>();
        GUtil.addToMap(systemProperties, jvmOptions.getSystemProperties());
        return systemProperties;
    }

    public Map<String, String> getEffectiveSystemProperties() {
        Map<String, String> systemProperties = new HashMap<String, String>();
        GUtil.addToMap(systemProperties, jvmOptions.getSystemProperties());
        GUtil.addToMap(systemProperties, System.getProperties());
        return systemProperties;
    }

    public void setJvmArgs(Iterable<String> jvmArgs) {
        hasJvmArgs = true;
        jvmOptions.setAllJvmArgs(jvmArgs);
    }

    public void setDebug(boolean debug) {
        jvmOptions.setDebug(debug);
    }

    public DaemonParameters setBaseDir(File baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    public boolean getDebug() {
        return jvmOptions.getDebug();
    }

    public DaemonUsage getDaemonUsage() {
        return daemonUsage;
    }

    public boolean isForeground() {
        return foreground;
    }

    public void setForeground(boolean foreground) {
        this.foreground = foreground;
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }
}
