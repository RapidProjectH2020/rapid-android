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

package org.gradle.model.internal.registry;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.gradle.api.Nullable;
import org.gradle.internal.Cast;
import org.gradle.model.RuleSource;
import org.gradle.model.internal.core.*;
import org.gradle.model.internal.core.rule.describe.ModelRuleDescriptor;
import org.gradle.model.internal.type.ModelType;

import java.util.Map;
import java.util.Set;

import static org.gradle.model.internal.core.ModelNode.State.Created;
import static org.gradle.model.internal.core.ModelNode.State.Initialized;

class ModelElementNode extends ModelNodeInternal {
    private final Map<String, ModelNodeInternal> links = Maps.newTreeMap();
    private final MutableModelNode parent;
    private Object privateData;
    private ModelType<?> privateDataType;

    public ModelElementNode(ModelRegistryInternal modelRegistry, ModelRegistration registration, MutableModelNode parent) {
        super(modelRegistry, registration);
        this.parent = parent;
    }

    @Override
    public MutableModelNode getParent() {
        return parent;
    }

    @Override
    public <T> ModelView<? extends T> asImmutable(ModelType<T> type, @Nullable ModelRuleDescriptor ruleDescriptor) {
        ModelView<? extends T> modelView = getAdapter().asImmutable(type, this, ruleDescriptor);
        if (modelView == null) {
            throw new IllegalStateException("Model element " + getPath() + " cannot be expressed as a read-only view of type " + type);
        }
        return modelView;
    }

    @Override
    public <T> ModelView<? extends T> asMutable(ModelType<T> type, ModelRuleDescriptor ruleDescriptor) {
        ModelView<? extends T> modelView = getAdapter().asMutable(type, this, ruleDescriptor);
        if (modelView == null) {
            throw new IllegalStateException("Model element " + getPath() + " cannot be expressed as a mutable view of type " + type);
        }
        return modelView;
    }

    @Override
    public <T> T getPrivateData(Class<T> type) {
        return getPrivateData(ModelType.of(type));
    }

    public <T> T getPrivateData(ModelType<T> type) {
        if (privateData == null) {
            return null;
        }

        if (!type.isAssignableFrom(privateDataType)) {
            throw new ClassCastException("Cannot get private data '" + privateData + "' of type '" + privateDataType + "' as type '" + type);
        }
        return Cast.uncheckedCast(privateData);
    }

    @Override
    public Object getPrivateData() {
        return privateData;
    }

    @Override
    public <T> void setPrivateData(Class<? super T> type, T object) {
        setPrivateData(ModelType.of(type), object);
    }

    public <T> void setPrivateData(ModelType<? super T> type, T object) {
        if (!isMutable()) {
            throw new IllegalStateException(String.format("Cannot set value for model element '%s' as this element is not mutable.", getPath()));
        }
        this.privateDataType = type;
        this.privateData = object;
    }

    public boolean hasLink(String name) {
        return links.containsKey(name);
    }

    @Nullable
    public ModelNodeInternal getLink(String name) {
        return links.get(name);
    }

    public Iterable<? extends ModelNodeInternal> getLinks() {
        return links.values();
    }

    @Override
    public int getLinkCount(Predicate<? super MutableModelNode> predicate) {
        return Iterables.size(Iterables.filter(links.values(), predicate));
    }

    @Override
    public Set<String> getLinkNames(Predicate<? super MutableModelNode> predicate) {
        Set<String> names = Sets.newLinkedHashSet();
        for (Map.Entry<String, ModelNodeInternal> entry : links.entrySet()) {
            ModelNodeInternal link = entry.getValue();
            if (predicate.apply(link)) {
                names.add(entry.getKey());
            }
        }
        return names;
    }

    @Override
    public Iterable<? extends MutableModelNode> getLinks(Predicate<? super MutableModelNode> predicate) {
        return Iterables.filter(links.values(), predicate);
    }

    @Override
    public int getLinkCount() {
        return links.size();
    }

    @Override
    public boolean hasLink(String name, Predicate<? super MutableModelNode> predicate) {
        ModelNodeInternal linked = getLink(name);
        return linked != null && predicate.apply(linked);
    }

    @Override
    public void applyToLink(ModelActionRole type, ModelAction action) {
        if (!getPath().isDirectChild(action.getSubject().getPath())) {
            throw new IllegalArgumentException(String.format("Linked element action reference has a path (%s) which is not a child of this node (%s).", action.getSubject().getPath(), getPath()));
        }
        modelRegistry.bind(action.getSubject(), type, action);
    }

    @Override
    public void applyTo(NodePredicate predicate, ModelActionRole role, ModelAction action) {
        modelRegistry.configureMatching(predicate.scope(getPath()), role, action);
    }

    @Override
    public void applyTo(NodePredicate predicate, Class<? extends RuleSource> rules) {
        modelRegistry.configureMatching(predicate.scope(getPath()), rules);
    }

    @Override
    public <T> void addReference(String name, ModelType<T> type, ModelRuleDescriptor descriptor) {
        // TODO:LPTR Remove projection for reference node
        // This shouldn't be needed, but if there's no actual value referenced, model report can only
        // show the type of the node if we do this for now. It should use the schema instead to find
        // the type of the property node instead.
        ModelProjection projection = new EmptyReferenceProjection<T>(type);
        ModelRegistration registration = ModelRegistrations.of(getPath().child(name))
            .withProjection(projection)
            .descriptor(descriptor).build();
        addNode(new ModelReferenceNode(modelRegistry, registration, this), registration);
    }

    @Override
    public void addLink(ModelRegistration registration) {
        addNode(new ModelElementNode(modelRegistry, registration, this), registration);
    }

    private void addNode(ModelNodeInternal child, ModelRegistration registration) {
        ModelPath childPath = child.getPath();
        if (!getPath().isDirectChild(childPath)) {
            throw new IllegalArgumentException(String.format("Element registration has a path (%s) which is not a child of this node (%s).", childPath, getPath()));
        }

        ModelNodeInternal currentChild = links.get(childPath.getName());
        if (currentChild != null) {
            if (!currentChild.isAtLeast(Created)) {
                throw new DuplicateModelException(
                    String.format(
                        "Cannot create '%s' using creation rule '%s' as the rule '%s' is already registered to create this model element.",
                        childPath,
                        describe(registration.getDescriptor()),
                        describe(currentChild.getDescriptor())
                    )
                );
            }
            throw new DuplicateModelException(
                String.format(
                    "Cannot create '%s' using creation rule '%s' as the rule '%s' has already been used to create this model element.",
                    childPath,
                    describe(registration.getDescriptor()),
                    describe(currentChild.getDescriptor())
                )
            );
        }
        if (!isMutable()) {
            throw new IllegalStateException(
                String.format(
                    "Cannot create '%s' using creation rule '%s' as model element '%s' is no longer mutable.",
                    childPath,
                    describe(registration.getDescriptor()),
                    getPath()
                )
            );
        }
        links.put(child.getPath().getName(), child);
        modelRegistry.registerNode(child, registration.getActions());
    }

    @Override
    public void removeLink(String name) {
        if (links.remove(name) != null) {
            modelRegistry.remove(getPath().child(name));
        }
    }

    @Override
    public void setTarget(ModelNode target) {
        throw new UnsupportedOperationException(String.format("This node (%s) is not a reference to another node.", getPath()));
    }

    @Override
    public void ensureUsable() {
        ensureAtLeast(Initialized);
    }

    @Override
    public void ensureAtLeast(State state) {
        modelRegistry.transition(this, state, true);
    }

    private static String describe(ModelRuleDescriptor descriptor) {
        StringBuilder stringBuilder = new StringBuilder();
        descriptor.describeTo(stringBuilder);
        return stringBuilder.toString();
    }
}
