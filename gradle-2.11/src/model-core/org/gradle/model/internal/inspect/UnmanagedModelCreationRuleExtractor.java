/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.model.internal.inspect;

import org.gradle.internal.Cast;
import org.gradle.model.internal.core.*;
import org.gradle.model.internal.core.rule.describe.ModelRuleDescriptor;
import org.gradle.model.internal.type.ModelType;

import java.util.List;

public class UnmanagedModelCreationRuleExtractor extends AbstractModelCreationRuleExtractor {
    private static final ModelType<Void> VOID = ModelType.of(Void.TYPE);

    @Override
    public boolean isSatisfiedBy(MethodRuleDefinition<?, ?> element) {
        return super.isSatisfiedBy(element) && !element.getReturnType().equals(VOID);
    }

    @Override
    protected <R, S> ExtractedModelRule buildRule(ModelPath modelPath, MethodRuleDefinition<R, S> ruleDefinition) {
        return new ExtractedUnmanagedCreationRule<R, S>(modelPath, ruleDefinition);
    }

    public String getDescription() {
        return String.format("%s and returning a model element", super.getDescription());
    }

    private static class UnmanagedElementCreationAction<T> implements MethodRuleAction {
        private final ModelRuleDescriptor descriptor;
        private final ModelReference<?> subject;
        private final ModelType<T> type;
        private final List<ModelReference<?>> inputs;

        private UnmanagedElementCreationAction(ModelRuleDescriptor descriptor, ModelReference<?> subject, List<ModelReference<?>> inputs, ModelType<T> type) {
            this.subject = subject;
            this.inputs = inputs;
            this.descriptor = descriptor;
            this.type = type;
        }

        @Override
        public ModelReference<?> getSubject() {
            return subject;
        }

        @Override
        public List<? extends ModelReference<?>> getInputs() {
            return inputs;
        }

        @Override
        public void execute(ModelRuleInvoker<?> ruleInvoker, MutableModelNode modelNode, List<ModelView<?>> inputs) {
            Object instance;
            if (inputs.size() == 0) {
                instance = ruleInvoker.invoke();
            } else {
                Object[] args = new Object[inputs.size()];
                for (int i = 0; i < inputs.size(); i++) {
                    args[i] = inputs.get(i).getInstance();
                }

                instance = ruleInvoker.invoke(args);
            }
            if (instance == null) {
                throw new ModelRuleExecutionException(descriptor, "rule returned null");
            }
            T value = Cast.uncheckedCast(instance);
            modelNode.setPrivateData(type, value);
        }
    }

    private static class ExtractedUnmanagedCreationRule<R, S> extends ExtractedCreationRule<R, S> {
        public ExtractedUnmanagedCreationRule(ModelPath modelPath, MethodRuleDefinition<R, S> ruleDefinition) {
            super(modelPath, ruleDefinition);
        }

        @Override
        protected void buildRegistration(MethodModelRuleApplicationContext context, ModelRegistrations.Builder registration) {
            ModelType<R> modelType = ruleDefinition.getReturnType();
            List<ModelReference<?>> inputs = ruleDefinition.getReferences();
            ModelRuleDescriptor descriptor = ruleDefinition.getDescriptor();
            ModelReference<Object> subjectReference = ModelReference.of(modelPath);
            registration.action(ModelActionRole.Create,
                    context.contextualize(ruleDefinition, new UnmanagedElementCreationAction<R>(descriptor, subjectReference, inputs, modelType)));
            registration.withProjection(new UnmanagedModelProjection<R>(modelType));
        }
    }
}
