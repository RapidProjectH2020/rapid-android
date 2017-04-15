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

package org.gradle.tooling.internal.consumer.connection;

import org.gradle.api.Action;
import org.gradle.tooling.internal.adapter.ProtocolToModelAdapter;
import org.gradle.tooling.internal.adapter.SourceObjectMapping;
import org.gradle.tooling.internal.consumer.parameters.ConsumerOperationParameters;
import org.gradle.tooling.internal.consumer.versioning.ModelMapping;
import org.gradle.tooling.internal.consumer.versioning.VersionDetails;
import org.gradle.tooling.internal.protocol.BuildActionRunner;
import org.gradle.tooling.internal.protocol.ConnectionVersion4;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
import org.gradle.tooling.model.idea.BasicIdeaProject;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.internal.Exceptions;
import org.gradle.tooling.model.internal.outcomes.ProjectOutcomes;

/**
 * An adapter for a {@link BuildActionRunner} based provider.
 *
 * <p>Used for providers >= 1.2 and <= 1.6.</p>
 */
public class BuildActionRunnerBackedConsumerConnection extends AbstractPost12ConsumerConnection {
    private final ModelProducer modelProducer;
    private final UnsupportedActionRunner actionRunner;

    public BuildActionRunnerBackedConsumerConnection(ConnectionVersion4 delegate, ModelMapping modelMapping, ProtocolToModelAdapter adapter) {
        super(delegate, new R12VersionDetails(delegate.getMetaData().getVersion()));
        ModelProducer consumerConnectionBackedModelProducer = new BuildActionRunnerBackedModelProducer(adapter, getVersionDetails(), modelMapping,  (BuildActionRunner) delegate, getCompatibilityMapperAction());
        ModelProducer producerWithGradleBuild = new GradleBuildAdapterProducer(adapter, consumerConnectionBackedModelProducer);
        modelProducer = new BuildInvocationsAdapterProducer(adapter, getVersionDetails(), producerWithGradleBuild);
        actionRunner = new UnsupportedActionRunner(getVersionDetails().getVersion());
    }

    @Override
    protected ActionRunner getActionRunner() {
        return actionRunner;
    }

    @Override
    protected ModelProducer getModelProducer() {
        return modelProducer;
    }

    private static class R12VersionDetails extends VersionDetails {
        public R12VersionDetails(String version) {
            super(version);
        }

        @Override
        public boolean maySupportModel(Class<?> modelType) {
            return modelType.equals(ProjectOutcomes.class)
                    || modelType.equals(HierarchicalEclipseProject.class)
                    || modelType.equals(EclipseProject.class)
                    || modelType.equals(IdeaProject.class)
                    || modelType.equals(BasicIdeaProject.class)
                    || modelType.equals(BuildEnvironment.class)
                    || modelType.equals(GradleProject.class)
                    || modelType.equals(Void.class);
        }
    }

    private static class BuildActionRunnerBackedModelProducer implements ModelProducer {
        private final ProtocolToModelAdapter adapter;
        private final VersionDetails versionDetails;
        private final ModelMapping modelMapping;
        private final BuildActionRunner buildActionRunner;
        private final Action<? super SourceObjectMapping> mapper;

        public BuildActionRunnerBackedModelProducer(ProtocolToModelAdapter adapter, VersionDetails versionDetails, ModelMapping modelMapping, BuildActionRunner buildActionRunner, Action<? super SourceObjectMapping> mapper) {
            this.adapter = adapter;
            this.versionDetails = versionDetails;
            this.modelMapping = modelMapping;
            this.buildActionRunner = buildActionRunner;
            this.mapper = mapper;
        }

        public <T> T produceModel(Class<T> type, ConsumerOperationParameters operationParameters) {
            if (!versionDetails.maySupportModel(type)) {
                //don't bother asking the provider for this model
                throw Exceptions.unsupportedModel(type, versionDetails.getVersion());

            }
            Class<?> protocolType = modelMapping.getProtocolType(type);
            Object model = buildActionRunner.run(protocolType, operationParameters).getModel();
            return adapter.adapt(type, model, mapper);
        }
    }
}
