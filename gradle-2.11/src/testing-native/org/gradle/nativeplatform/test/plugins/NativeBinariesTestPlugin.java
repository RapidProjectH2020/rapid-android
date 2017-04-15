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

package org.gradle.nativeplatform.test.plugins;

import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.language.nativeplatform.DependentSourceSet;
import org.gradle.model.Defaults;
import org.gradle.model.ModelMap;
import org.gradle.model.RuleSource;
import org.gradle.nativeplatform.plugins.NativeComponentPlugin;
import org.gradle.nativeplatform.test.NativeTestSuiteBinarySpec;
import org.gradle.nativeplatform.test.internal.DefaultNativeTestSuiteBinarySpec;
import org.gradle.nativeplatform.test.internal.NativeTestSuiteBinarySpecInternal;
import org.gradle.platform.base.BinarySpec;
import org.gradle.platform.base.BinaryType;
import org.gradle.platform.base.BinaryTypeBuilder;
import org.gradle.testing.base.plugins.TestingModelBasePlugin;

/**
 * A plugin that sets up the infrastructure for testing native binaries.
 */
@Incubating
public class NativeBinariesTestPlugin implements Plugin<Project> {

    public void apply(final Project project) {
        project.getPluginManager().apply(NativeComponentPlugin.class);
        project.getPluginManager().apply(TestingModelBasePlugin.class);
    }

    @SuppressWarnings("UnusedDeclaration")
    static class Rules extends RuleSource {
        @BinaryType
        void nativeTestSuiteBinary(BinaryTypeBuilder<NativeTestSuiteBinarySpec> builder) {
            builder.defaultImplementation(DefaultNativeTestSuiteBinarySpec.class);
            builder.internalView(NativeTestSuiteBinarySpecInternal.class);
        }

        @Defaults
        void attachTestedBinarySourcesToTestBinaries(ModelMap<NativeTestSuiteBinarySpecInternal> binaries) {
            binaries.afterEach(new Action<NativeTestSuiteBinarySpecInternal>() {
                @Override
                public void execute(NativeTestSuiteBinarySpecInternal testSuiteBinary) {
                    BinarySpec testedBinary = testSuiteBinary.getTestedBinary();
                    for (DependentSourceSet testSource : testSuiteBinary.getInputs().withType(DependentSourceSet.class)) {
                        testSource.lib(testedBinary.getInputs());
                    }
                    testSuiteBinary.getInputs().addAll(testedBinary.getInputs());
                }
            });
        }
    }
}
