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

package org.gradle.platform.base.binary;

import org.apache.commons.lang.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.Incubating;
import org.gradle.api.Nullable;
import org.gradle.api.artifacts.component.LibraryBinaryIdentifier;
import org.gradle.api.internal.AbstractBuildableModelElement;
import org.gradle.api.internal.DefaultDomainObjectSet;
import org.gradle.api.internal.project.taskfactory.ITaskFactory;
import org.gradle.internal.component.local.model.DefaultLibraryBinaryIdentifier;
import org.gradle.internal.reflect.DirectInstantiator;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.reflect.ObjectInstantiationException;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.model.ModelMap;
import org.gradle.model.internal.core.*;
import org.gradle.model.internal.type.ModelType;
import org.gradle.platform.base.BinarySpec;
import org.gradle.platform.base.BinaryTasksCollection;
import org.gradle.platform.base.ComponentSpec;
import org.gradle.platform.base.ModelInstantiationException;
import org.gradle.platform.base.internal.*;
import org.gradle.util.DeprecationLogger;

import java.io.File;
import java.util.Set;

/**
 * Base class for custom binary implementations.
 * A custom implementation of {@link org.gradle.platform.base.BinarySpec} must extend this type.
 *
 * TODO at the moment leaking BinarySpecInternal here to generate lifecycleTask in
 * LanguageBasePlugin$createLifecycleTaskForBinary#createLifecycleTaskForBinary rule
 *
 */
@Incubating
// Needs to be here instead of the specific methods, because Java 6 and 7 will throw warnings otherwise
@SuppressWarnings("deprecation")
public class BaseBinarySpec extends AbstractBuildableModelElement implements BinarySpecInternal {
    private static final ModelType<BinaryTasksCollection> BINARY_TASKS_COLLECTION = ModelType.of(BinaryTasksCollection.class);

    private static ThreadLocal<BinaryInfo> nextBinaryInfo = new ThreadLocal<BinaryInfo>();
    private final DomainObjectSet<LanguageSourceSet> inputSourceSets = new DefaultDomainObjectSet<LanguageSourceSet>(LanguageSourceSet.class);
    private final BinaryTasksCollection tasks;
    private final String name;
    private final MutableModelNode componentNode;
    private final MutableModelNode sources;
    private final Class<? extends BinarySpec> publicType;
    private BinaryNamingScheme namingScheme;
    private boolean disabled;

    public static <T extends BaseBinarySpec> T create(Class<? extends BinarySpec> publicType, Class<T> implementationType,
                                                      String name, MutableModelNode modelNode, @Nullable MutableModelNode componentNode,
                                                      Instantiator instantiator, ITaskFactory taskFactory) {
        nextBinaryInfo.set(new BinaryInfo(name, publicType, implementationType, modelNode, componentNode, taskFactory, instantiator));
        try {
            try {
                return DirectInstantiator.INSTANCE.newInstance(implementationType);
            } catch (ObjectInstantiationException e) {
                throw new ModelInstantiationException(String.format("Could not create binary of type %s", publicType.getSimpleName()), e.getCause());
            }
        } finally {
            nextBinaryInfo.set(null);
        }
    }

    public BaseBinarySpec() {
        this(nextBinaryInfo.get());
    }

    private BaseBinarySpec(BinaryInfo info) {
        if (info == null) {
            throw new ModelInstantiationException("Direct instantiation of a BaseBinarySpec is not permitted. Use a BinaryTypeBuilder instead.");
        }
        this.name = info.name;
        this.publicType = info.publicType;
        MutableModelNode modelNode = info.modelNode;
        this.componentNode = info.componentNode;
        this.tasks = info.instantiator.newInstance(DefaultBinaryTasksCollection.class, this, info.taskFactory);

        sources = ModelMaps.addModelMapNode(modelNode, LanguageSourceSet.class, "sources");
        ModelRegistration itemRegistration = ModelRegistrations.of(modelNode.getPath().child("tasks"))
            .action(ModelActionRole.Create, new Action<MutableModelNode>() {
                @Override
                public void execute(MutableModelNode modelNode) {
                    modelNode.setPrivateData(BINARY_TASKS_COLLECTION, tasks);
                }
            })
            .withProjection(new UnmanagedModelProjection<BinaryTasksCollection>(BINARY_TASKS_COLLECTION))
            .descriptor(modelNode.getDescriptor())
            .build();
        modelNode.addLink(itemRegistration);

        namingScheme = DefaultBinaryNamingScheme.component(componentName()).withBinaryName(name).withBinaryType(getTypeName());
    }

    @Nullable
    private String componentName() {
        ComponentSpec component = getComponent();
        return component != null ? component.getName() : null;
    }

    @Override
    public LibraryBinaryIdentifier getId() {
        // TODO:DAZ This can throw a NPE: will need an identifier for a variant without an owning component
        ComponentSpec component = getComponent();
        return new DefaultLibraryBinaryIdentifier(component.getProjectPath(), component.getName(), getName());
    }

    @Override
    public Class<? extends BinarySpec> getPublicType() {
        return publicType;
    }

    @Nullable
    public ComponentSpec getComponent() {
        return getComponentAs(ComponentSpec.class);
    }

    @Nullable
    protected <T extends ComponentSpec> T getComponentAs(Class<T> componentType) {
        if (componentNode == null) {
            return null;
        }
        ModelType<T> modelType = ModelType.of(componentType);
        return componentNode.canBeViewedAs(modelType)
            ? componentNode.asImmutable(modelType, componentNode.getDescriptor()).getInstance()
            : null;
    }

    protected String getTypeName() {
        return publicType.getSimpleName();
    }

    @Override
    public String getProjectScopedName() {
        ComponentSpec owner = getComponent();
        return owner == null ? name : owner.getName() + StringUtils.capitalize(name);
    }

    public String getDisplayName() {
        ComponentSpec owner = getComponent();
        if (owner == null) {
            return String.format("%s '%s'", getTypeName(), name);
        } else {
            return String.format("%s '%s:%s'", getTypeName(), owner.getName(), name);
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public void setBuildable(boolean buildable) {
        this.disabled = !buildable;
    }

    public final boolean isBuildable() {
        return getBuildAbility().isBuildable();
    }

    @Override
    public DomainObjectSet<LanguageSourceSet> getSource() {
        DeprecationLogger.nagUserOfReplacedProperty("source", "inputs");
        return getInputs();
    }

    @Override
    public DomainObjectSet<LanguageSourceSet> getInputs() {
        return inputSourceSets;
    }

    @Override
    public ModelMap<LanguageSourceSet> getSources() {
        return ModelMaps.toView(sources, LanguageSourceSet.class);
    }

    public BinaryTasksCollection getTasks() {
        return tasks;
    }

    public boolean isLegacyBinary() {
        return false;
    }

    public BinaryNamingScheme getNamingScheme() {
        return namingScheme;
    }

    public void setNamingScheme(BinaryNamingScheme namingScheme) {
        this.namingScheme = namingScheme;
    }

    @Override
    public boolean hasCodependentSources() {
        return false;
    }

    private static class BinaryInfo {
        private final String name;
        private final Class<? extends BinarySpec> publicType;
        private final MutableModelNode modelNode;
        private final MutableModelNode componentNode;
        private final ITaskFactory taskFactory;
        private final Instantiator instantiator;

        private BinaryInfo(String name, Class<? extends BinarySpec> publicType, Class<? extends BaseBinarySpec> implementationType, MutableModelNode modelNode, MutableModelNode componentNode, ITaskFactory taskFactory, Instantiator instantiator) {
            this.name = name;
            this.publicType = publicType;
            this.modelNode = modelNode;
            this.componentNode = componentNode;
            this.taskFactory = taskFactory;
            this.instantiator = instantiator;
        }
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public final BinaryBuildAbility getBuildAbility() {
        if (disabled) {
            return new FixedBuildAbility(false);
        }
        return getBinaryBuildAbility();
    }

    protected BinaryBuildAbility getBinaryBuildAbility() {
        // Default behavior is to always be buildable.  Binary implementations should define what
        // criteria make them buildable or not.
        return new FixedBuildAbility(true);
    }

    public static void replaceSingleDirectory(Set<File> dirs, File dir) {
        switch (dirs.size()) {
            case 0:
                dirs.add(dir);
                break;
            case 1:
                dirs.clear();
                dirs.add(dir);
                break;
            default:
                throw new IllegalStateException("Can't replace multiple directories.");
        }
    }

}
