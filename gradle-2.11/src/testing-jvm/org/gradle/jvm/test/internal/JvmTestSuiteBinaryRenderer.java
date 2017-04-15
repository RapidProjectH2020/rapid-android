/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.jvm.test.internal;

import org.gradle.api.tasks.diagnostics.internal.text.TextReportBuilder;
import org.gradle.jvm.internal.AbstractJvmBinaryRenderer;
import org.gradle.jvm.test.JvmTestSuiteBinarySpec;
import org.gradle.model.internal.manage.schema.ModelSchemaStore;

import javax.inject.Inject;

public abstract class JvmTestSuiteBinaryRenderer<T extends JvmTestSuiteBinarySpec> extends AbstractJvmBinaryRenderer<T> {
    @Inject
    public JvmTestSuiteBinaryRenderer(ModelSchemaStore schemaStore) {
        super(schemaStore);
    }

    @Override
    protected void renderTasks(JvmTestSuiteBinarySpec binary, TextReportBuilder builder) {
        builder.item("run using task", binary.getTasks().getRun().getPath());
    }

    @Override
    protected void renderDetails(T binary, TextReportBuilder builder) {
        if (binary.getTestedBinary() != null) {
            builder.item("binary under test", binary.getTestedBinary().getDisplayName());
        }
        super.renderDetails(binary, builder);
    }

}
