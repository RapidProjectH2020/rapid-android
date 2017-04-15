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
package org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph;

import com.google.common.base.Joiner;
import org.gradle.api.Action;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.result.ComponentSelectionReason;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.artifacts.ResolveContext;
import org.gradle.api.internal.artifacts.ResolvedConfigurationIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.*;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.conflicts.CandidateModule;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.conflicts.ConflictHandler;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.conflicts.ConflictResolutionResult;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.conflicts.PotentialConflict;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.ModuleVersionSelection;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.VersionSelectionReasons;
import org.gradle.internal.Cast;
import org.gradle.internal.component.local.model.DslOriginDependencyMetaData;
import org.gradle.internal.component.model.*;
import org.gradle.internal.resolve.ModuleVersionResolveException;
import org.gradle.internal.resolve.resolver.ComponentMetaDataResolver;
import org.gradle.internal.resolve.resolver.DependencyToComponentIdResolver;
import org.gradle.internal.resolve.resolver.ResolveContextToComponentResolver;
import org.gradle.internal.resolve.result.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DependencyGraphBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyGraphBuilder.class);
    private final DependencyToConfigurationResolver dependencyToConfigurationResolver;
    private final ConflictHandler conflictHandler;
    private final ResolveContextToComponentResolver moduleResolver;
    private final DependencyToComponentIdResolver idResolver;
    private final ComponentMetaDataResolver metaDataResolver;

    public DependencyGraphBuilder(DependencyToComponentIdResolver componentIdResolver, ComponentMetaDataResolver componentMetaDataResolver,
                                  ResolveContextToComponentResolver resolveContextToComponentResolver, DependencyToConfigurationResolver dependencyToConfigurationResolver,
                                  ConflictHandler conflictHandler) {
        this.idResolver = componentIdResolver;
        this.metaDataResolver = componentMetaDataResolver;
        this.moduleResolver = resolveContextToComponentResolver;
        this.conflictHandler = conflictHandler;
        this.dependencyToConfigurationResolver = dependencyToConfigurationResolver;
    }

    public void resolve(ResolveContext resolveContext, DependencyGraphVisitor modelVisitor) {
        DefaultBuildableComponentResolveResult rootModule = new DefaultBuildableComponentResolveResult();
        moduleResolver.resolve(resolveContext, rootModule);

        ResolveState resolveState = new ResolveState(rootModule, resolveContext.getName(), idResolver, metaDataResolver, dependencyToConfigurationResolver);
        conflictHandler.registerResolver(new DirectDependencyForcingResolver(resolveState.root.moduleRevision));

        traverseGraph(resolveState, conflictHandler);

        assembleResult(resolveState, modelVisitor);
    }

    /**
     * Traverses the dependency graph, resolving conflicts and building the paths from the root configuration.
     */
    private void traverseGraph(final ResolveState resolveState, final ConflictHandler conflictHandler) {
        resolveState.onMoreSelected(resolveState.root);

        List<DependencyEdge> dependencies = new ArrayList<DependencyEdge>();
        while (resolveState.peek() != null || conflictHandler.hasConflicts()) {
            if (resolveState.peek() != null) {
                ConfigurationNode node = resolveState.pop();
                LOGGER.debug("Visiting configuration {}.", node);

                // Calculate the outgoing edges of this configuration
                dependencies.clear();
                node.visitOutgoingDependencies(dependencies);

                for (DependencyEdge dependency : dependencies) {
                    LOGGER.debug("Visiting dependency {}", dependency);

                    // Resolve dependency to a particular revision
                    ModuleVersionResolveState moduleRevision = dependency.resolveModuleRevisionId();
                    if (moduleRevision == null) {
                        // Failed to resolve.
                        continue;
                    }
                    ModuleIdentifier moduleId = moduleRevision.id.getModule();

                    // Check for a new conflict
                    if (moduleRevision.state == ModuleState.New) {
                        ModuleResolveState module = resolveState.getModule(moduleId);

                        // A new module revision. Check for conflict
                        PotentialConflict c = conflictHandler.registerModule(module);
                        if (!c.conflictExists()) {
                            // No conflict. Select it for now
                            LOGGER.debug("Selecting new module version {}", moduleRevision);
                            module.select(moduleRevision);
                        } else {
                            // We have a conflict
                            LOGGER.debug("Found new conflicting module version {}", moduleRevision);

                            // Deselect the currently selected version, and remove all outgoing edges from the version
                            // This will propagate through the graph and prune configurations that are no longer required
                            // For each module participating in the conflict (many times there is only one participating module that has multiple versions)
                            c.withParticipatingModules(new Action<ModuleIdentifier>() {
                                public void execute(ModuleIdentifier module) {
                                    ModuleVersionResolveState previouslySelected = resolveState.getModule(module).clearSelection();
                                    if (previouslySelected != null) {
                                        for (ConfigurationNode configuration : previouslySelected.configurations) {
                                            configuration.deselect();
                                        }
                                    }
                                }
                            });
                        }
                    }

                    dependency.attachToTargetConfigurations();
                }
            } else {
                // We have some batched up conflicts. Resolve the first, and continue traversing the graph
                conflictHandler.resolveNextConflict(new Action<ConflictResolutionResult>() {
                    public void execute(final ConflictResolutionResult result) {
                        result.getConflict().withParticipatingModules(new Action<ModuleIdentifier>() {
                            public void execute(ModuleIdentifier moduleIdentifier) {
                                ModuleVersionResolveState selected = result.getSelected();
                                // Restart each configuration. For the evicted configuration, this means moving incoming dependencies across to the
                                // matching selected configuration. For the select configuration, this mean traversing its dependencies.
                                resolveState.getModule(moduleIdentifier).restart(selected);
                            }
                        });
                    }
                });
            }
        }
    }

    /**
     * Populates the result from the graph traversal state.
     */
    private void assembleResult(ResolveState resolveState, DependencyGraphVisitor listener) {
        listener.start(resolveState.root);

        // Visit the nodes
        for (ConfigurationNode resolvedConfiguration : resolveState.getConfigurationNodes()) {
            if (resolvedConfiguration.isSelected()) {
                resolvedConfiguration.validate();
                listener.visitNode(resolvedConfiguration);
            }
        }
        // Visit the edges
        for (ConfigurationNode resolvedConfiguration : resolveState.getConfigurationNodes()) {
            if (resolvedConfiguration.isSelected()) {
                listener.visitEdge(resolvedConfiguration);
            }
        }

        listener.finish(resolveState.root);
    }

    /**
     * Represents the edges in the dependency graph.
     */
    private static class DependencyEdge implements DependencyGraphEdge {
        public final ConfigurationNode from;
        public final ModuleVersionSelectorResolveState selector;

        private final DependencyMetaData dependencyMetaData;
        private final ResolveState resolveState;
        private final ModuleResolutionFilter resolutionFilter;
        private final Set<ConfigurationNode> targetConfigurations = new LinkedHashSet<ConfigurationNode>();
        private ModuleVersionResolveState targetModuleRevision;

        public DependencyEdge(ConfigurationNode from, DependencyMetaData dependencyMetaData, ModuleResolutionFilter resolutionFilter, ResolveState resolveState) {
            this.from = from;
            this.dependencyMetaData = dependencyMetaData;
            this.resolutionFilter = resolutionFilter;
            this.resolveState = resolveState;
            selector = resolveState.getSelector(dependencyMetaData);
        }

        @Override
        public String toString() {
            return String.format("%s -> %s(%s)", from.toString(), dependencyMetaData.getRequested(), Joiner.on(',').join(dependencyMetaData.getModuleConfigurations()));
        }

        @Override
        public DependencyGraphNode getFrom() {
            return from;
        }

        /**
         * @return The resolved module version
         */
        public ModuleVersionResolveState resolveModuleRevisionId() {
            if (targetModuleRevision == null) {
                targetModuleRevision = selector.resolveModuleRevisionId();
                selector.getSelectedModule().addUnattachedDependency(this);
            }
            return targetModuleRevision;
        }

        public boolean isTransitive() {
            return from.isTransitive() && dependencyMetaData.isTransitive();
        }

        public void attachToTargetConfigurations() {
            if (targetModuleRevision.state != ModuleState.Selected) {
                return;
            }
            calculateTargetConfigurations();
            for (ConfigurationNode targetConfiguration : targetConfigurations) {
                targetConfiguration.addIncomingEdge(this);
            }
            if (!targetConfigurations.isEmpty()) {
                selector.getSelectedModule().removeUnattachedDependency(this);
            }
        }

        public void removeFromTargetConfigurations() {
            for (ConfigurationNode targetConfiguration : targetConfigurations) {
                targetConfiguration.removeIncomingEdge(this);
            }
            targetConfigurations.clear();
            if (targetModuleRevision != null) {
                selector.getSelectedModule().removeUnattachedDependency(this);
            }
        }

        public void restart(ModuleVersionResolveState selected) {
            targetModuleRevision = selected;
            attachToTargetConfigurations();
        }

        private void calculateTargetConfigurations() {
            targetConfigurations.clear();
            ComponentResolveMetaData targetModuleVersion = targetModuleRevision.getMetaData();
            if (targetModuleVersion == null) {
                // Broken version
                return;
            }

            Set<ConfigurationMetaData> targetConfigurations = resolveState.dependencyToConfigurationResolver.resolveTargetConfigurations(dependencyMetaData, from.metaData, targetModuleVersion);
            for (ConfigurationMetaData targetConfiguration : targetConfigurations) {
                ConfigurationNode targetConfigurationNode = resolveState.getConfigurationNode(targetModuleRevision, targetConfiguration.getName());
                this.targetConfigurations.add(targetConfigurationNode);
            }
        }

        public ModuleResolutionFilter getSelector() {
            Set<String> hierarchy = from.metaData.getHierarchy();
            ModuleResolutionFilter selector = DefaultModuleResolutionFilter.excludeAny(dependencyMetaData.getExcludeRules(hierarchy));
            return selector.intersect(resolutionFilter);
        }

        public ComponentSelector getRequested() {
            return dependencyMetaData.getSelector();
        }

        public ModuleVersionSelector getRequestedModuleVersion() {
            return dependencyMetaData.getRequested();
        }

        public ModuleVersionResolveException getFailure() {
            return selector.getFailure();
        }

        public ModuleVersionIdentifier getSelected() {
            return selector.getSelected().getId();
        }

        public ComponentSelectionReason getReason() {
            return selector.getSelectionReason();
        }

        public ModuleDependency getModuleDependency() {
            if (dependencyMetaData instanceof DslOriginDependencyMetaData) {
                return ((DslOriginDependencyMetaData) dependencyMetaData).getSource();
            }
            return null;
        }

        public Set<ComponentArtifactMetaData> getArtifacts(ConfigurationMetaData metaData1) {
            return dependencyMetaData.getArtifacts(from.metaData, metaData1);
        }
    }

    /**
     * Global resolution state.
     */
    private static class ResolveState {
        private final Map<ModuleIdentifier, ModuleResolveState> modules = new LinkedHashMap<ModuleIdentifier, ModuleResolveState>();
        private final Map<ResolvedConfigurationIdentifier, ConfigurationNode> nodes = new LinkedHashMap<ResolvedConfigurationIdentifier, ConfigurationNode>();
        private final Map<ModuleVersionSelector, ModuleVersionSelectorResolveState> selectors = new LinkedHashMap<ModuleVersionSelector, ModuleVersionSelectorResolveState>();
        private final RootConfigurationNode root;
        private final DependencyToComponentIdResolver idResolver;
        private final ComponentMetaDataResolver metaDataResolver;
        private final DependencyToConfigurationResolver dependencyToConfigurationResolver;
        private final Set<ConfigurationNode> queued = new HashSet<ConfigurationNode>();
        private final LinkedList<ConfigurationNode> queue = new LinkedList<ConfigurationNode>();

        public ResolveState(ComponentResolveResult rootResult, String rootConfigurationName, DependencyToComponentIdResolver idResolver,
                            ComponentMetaDataResolver metaDataResolver, DependencyToConfigurationResolver dependencyToConfigurationResolver) {
            this.idResolver = idResolver;
            this.metaDataResolver = metaDataResolver;
            this.dependencyToConfigurationResolver = dependencyToConfigurationResolver;
            ModuleVersionResolveState rootVersion = getRevision(rootResult.getId());
            rootVersion.setMetaData(rootResult.getMetaData());
            root = new RootConfigurationNode(rootVersion, new ResolvedConfigurationIdentifier(rootVersion.id, rootConfigurationName), this);
            nodes.put(root.id, root);
            root.moduleRevision.module.select(root.moduleRevision);
        }

        public ModuleResolveState getModule(ModuleIdentifier id) {
            ModuleResolveState module = modules.get(id);
            if (module == null) {
                module = new ModuleResolveState(id, this, metaDataResolver);
                modules.put(id, module);
            }
            return module;
        }

        public ModuleVersionResolveState getRevision(ModuleVersionIdentifier id) {
            return getModule(id.getModule()).getVersion(id);
        }

        public Collection<ConfigurationNode> getConfigurationNodes() {
            return nodes.values();
        }

        public ConfigurationNode getConfigurationNode(ModuleVersionResolveState module, String configurationName) {
            ResolvedConfigurationIdentifier id = new ResolvedConfigurationIdentifier(module.id, configurationName);
            ConfigurationNode configuration = nodes.get(id);
            if (configuration == null) {
                configuration = new ConfigurationNode(id, module, this);
                nodes.put(id, configuration);
            }
            return configuration;
        }

        public ModuleVersionSelectorResolveState getSelector(DependencyMetaData dependencyMetaData) {
            ModuleVersionSelector requested = dependencyMetaData.getRequested();
            ModuleVersionSelectorResolveState resolveState = selectors.get(requested);
            if (resolveState == null) {
                resolveState = new ModuleVersionSelectorResolveState(dependencyMetaData, idResolver, this);
                selectors.put(requested, resolveState);
            }
            return resolveState;
        }

        public ConfigurationNode peek() {
            return queue.isEmpty() ? null : queue.getFirst();
        }

        public ConfigurationNode pop() {
            ConfigurationNode next = queue.removeFirst();
            queued.remove(next);
            return next;
        }

        /**
         * Called when a change is made to a configuration node, such that its dependency graph <em>may</em> now be larger than it previously was, and the node should be visited.
         */
        public void onMoreSelected(ConfigurationNode configuration) {
            // Add to the end of the queue, so that we traverse the graph in breadth-wise order to pick up as many conflicts as
            // possible before attempting to resolve them
            if (queued.add(configuration)) {
                queue.addLast(configuration);
            }
        }

        /**
         * Called when a change is made to a configuration node, such that its dependency graph <em>may</em> now be smaller than it previously was, and the node should be visited.
         */
        public void onFewerSelected(ConfigurationNode configuration) {
            // Add to the front of the queue, to flush out configurations that are no longer required.
            if (queued.add(configuration)) {
                queue.addFirst(configuration);
            }
        }
    }

    enum ModuleState {
        New,
        Selected,
        Conflict,
        Evicted
    }

    /**
     * Resolution state for a given module.
     */
    private static class ModuleResolveState implements CandidateModule {
        final ComponentMetaDataResolver metaDataResolver;
        final ModuleIdentifier id;
        final Set<DependencyEdge> unattachedDependencies = new LinkedHashSet<DependencyEdge>();
        final Map<ModuleVersionIdentifier, ModuleVersionResolveState> versions = new LinkedHashMap<ModuleVersionIdentifier, ModuleVersionResolveState>();
        final Set<ModuleVersionSelectorResolveState> selectors = new HashSet<ModuleVersionSelectorResolveState>();
        final ResolveState resolveState;
        ModuleVersionResolveState selected;

        private ModuleResolveState(ModuleIdentifier id, ResolveState resolveState, ComponentMetaDataResolver metaDataResolver) {
            this.id = id;
            this.resolveState = resolveState;
            this.metaDataResolver = metaDataResolver;
        }

        @Override
        public String toString() {
            return id.toString();
        }

        public ModuleIdentifier getId() {
            return id;
        }

        public Collection<ModuleVersionResolveState> getVersions() {
            return versions.values();
        }

        public void select(ModuleVersionResolveState selected) {
            assert this.selected == null;
            this.selected = selected;
            for (ModuleVersionResolveState version : versions.values()) {
                version.state = ModuleState.Evicted;
            }
            selected.state = ModuleState.Selected;
        }

        public ModuleVersionResolveState clearSelection() {
            ModuleVersionResolveState previousSelection = selected;
            selected = null;
            for (ModuleVersionResolveState version : versions.values()) {
                version.state = ModuleState.Conflict;
            }
            return previousSelection;
        }

        public void restart(ModuleVersionResolveState selected) {
            select(selected);
            for (ModuleVersionResolveState version : versions.values()) {
                version.restart(selected);
            }
            for (ModuleVersionSelectorResolveState selector : selectors) {
                selector.restart(selected);
            }
            for (DependencyEdge dependency : new ArrayList<DependencyEdge>(unattachedDependencies)) {
                dependency.restart(selected);
            }
            unattachedDependencies.clear();
        }

        public void addUnattachedDependency(DependencyEdge edge) {
            unattachedDependencies.add(edge);
        }

        public void removeUnattachedDependency(DependencyEdge edge) {
            unattachedDependencies.remove(edge);
        }

        public ModuleVersionResolveState getVersion(ModuleVersionIdentifier id) {
            ModuleVersionResolveState moduleRevision = versions.get(id);
            if (moduleRevision == null) {
                moduleRevision = new ModuleVersionResolveState(this, id, metaDataResolver);
                versions.put(id, moduleRevision);
            }

            return moduleRevision;
        }

        public void addSelector(ModuleVersionSelectorResolveState selector) {
            selectors.add(selector);
        }
    }

    /**
     * Resolution state for a given module version.
     */
    public static class ModuleVersionResolveState implements ComponentResolutionState, ModuleVersionSelection {
        public final ModuleVersionIdentifier id;
        private final ComponentMetaDataResolver resolver;
        private final Set<ConfigurationNode> configurations = new LinkedHashSet<ConfigurationNode>();
        private final ModuleResolveState module;
        private ComponentResolveMetaData metaData;
        private ModuleState state = ModuleState.New;
        private ComponentSelectionReason selectionReason = VersionSelectionReasons.REQUESTED;
        private ModuleVersionResolveException failure;
        private ModuleVersionSelectorResolveState firstReference;

        private ModuleVersionResolveState(ModuleResolveState module, ModuleVersionIdentifier id, ComponentMetaDataResolver resolver) {
            this.module = module;
            this.id = id;
            this.resolver = resolver;
        }

        @Override
        public String toString() {
            return id.toString();
        }

        public String getVersion() {
            return id.getVersion();
        }

        public ModuleVersionIdentifier getId() {
            return id;
        }

        public ModuleVersionResolveException getFailure() {
            return failure;
        }

        public void restart(ModuleVersionResolveState selected) {
            for (ConfigurationNode configuration : configurations) {
                configuration.restart(selected);
            }
        }

        public void addResolver(ModuleVersionSelectorResolveState resolver) {
            if (firstReference == null) {
                firstReference = resolver;
            }
        }

        public void resolve() {
            if (metaData != null || failure != null) {
                return;
            }

            ComponentIdResolveResult idResolveResult = firstReference.idResolveResult;
            if (idResolveResult.getFailure() != null) {
                failure = idResolveResult.getFailure();
                return;
            }
            if (idResolveResult.getMetaData() != null) {
                metaData = idResolveResult.getMetaData();
                return;
            }

            DefaultBuildableComponentResolveResult result = new DefaultBuildableComponentResolveResult();
            resolver.resolve(idResolveResult.getId(), DefaultComponentOverrideMetadata.forDependency(firstReference.dependencyMetaData), result);
            if (result.getFailure() != null) {
                failure = result.getFailure();
                return;
            }
            metaData = result.getMetaData();
        }

        public ComponentResolveMetaData getMetaData() {
            if (metaData == null) {
                resolve();
            }
            return metaData;
        }

        public void setMetaData(ComponentResolveMetaData metaData) {
            this.metaData = metaData;
            this.failure = null;
        }

        public void addConfiguration(ConfigurationNode configurationNode) {
            configurations.add(configurationNode);
        }

        public ComponentSelectionReason getSelectionReason() {
            return selectionReason;
        }

        public void setSelectionReason(ComponentSelectionReason reason) {
            this.selectionReason = reason;
        }

        public ComponentIdentifier getComponentId() {
            return getMetaData().getComponentId();
        }

        public List<ModuleVersionResolveState> getIncoming() {
            List<ModuleVersionResolveState> incoming = new ArrayList<ModuleVersionResolveState>();
            for (DependencyGraphBuilder.ConfigurationNode configuration : configurations) {
                for (DependencyGraphBuilder.DependencyEdge dependencyEdge : configuration.incomingEdges) {
                    incoming.add(dependencyEdge.from.moduleRevision);
                }
            }
            return incoming;
        }
    }

    /**
     * Represents a node in the dependency graph.
     */
    static class ConfigurationNode implements DependencyGraphNode {
        public final ModuleVersionResolveState moduleRevision;
        public final Set<DependencyEdge> incomingEdges = new LinkedHashSet<DependencyEdge>();
        public final Set<DependencyEdge> outgoingEdges = new LinkedHashSet<DependencyEdge>();
        public final ResolvedConfigurationIdentifier id;

        private final ConfigurationMetaData metaData;
        private final ResolveState resolveState;
        private ModuleResolutionFilter previousTraversal;

        private ConfigurationNode(ResolvedConfigurationIdentifier id, ModuleVersionResolveState moduleRevision, ResolveState resolveState) {
            this.id = id;
            this.moduleRevision = moduleRevision;
            this.resolveState = resolveState;
            this.metaData = moduleRevision.metaData.getConfiguration(id.getConfiguration());
            moduleRevision.addConfiguration(this);
        }

        @Override
        public ResolvedConfigurationIdentifier getNodeId() {
            return id;
        }

        @Override
        public ModuleVersionIdentifier toId() {
            return moduleRevision.id;
        }

        @Override
        public ComponentIdentifier getComponentId() {
            return moduleRevision.getComponentId();
        }

        @Override
        public ModuleVersionSelection getSelection() {
            return moduleRevision;
        }

        @Override
        public Set<DependencyGraphEdge> getIncomingEdges() {
            return Cast.uncheckedCast(incomingEdges);
        }

        @Override
        public Set<DependencyGraphEdge> getOutgoingEdges() {
            return Cast.uncheckedCast(outgoingEdges);
        }

        @Override
        public ConfigurationMetaData getMetaData() {
            return metaData;
        }

        @Override
        public String toString() {
            return String.format("%s(%s)", id.getId(), id.getConfiguration());
        }

        public boolean isTransitive() {
            return metaData.isTransitive();
        }

        public void visitOutgoingDependencies(Collection<DependencyEdge> target) {
            // If this configuration's version is in conflict, don't do anything
            // If not traversed before, add all selected outgoing edges
            // If traversed before, and the selected modules have changed, remove previous outgoing edges and add outgoing edges again with
            //    the new selections.
            // If traversed before, and the selected modules have not changed, ignore
            // If none of the incoming edges are transitive, then the node has no outgoing edges

            if (moduleRevision.state != ModuleState.Selected) {
                LOGGER.debug("version for {} is not selected. ignoring.", this);
                return;
            }

            List<DependencyEdge> transitiveIncoming = new ArrayList<DependencyEdge>();
            for (DependencyEdge edge : incomingEdges) {
                if (edge.isTransitive()) {
                    transitiveIncoming.add(edge);
                }
            }

            if (transitiveIncoming.isEmpty() && this != resolveState.root) {
                if (previousTraversal != null) {
                    removeOutgoingEdges();
                }
                if (incomingEdges.isEmpty()) {
                    LOGGER.debug("{} has no incoming edges. ignoring.", this);
                } else {
                    LOGGER.debug("{} has no transitive incoming edges. ignoring outgoing edges.", this);
                }
                return;
            }

            ModuleResolutionFilter resolutionFilter = getModuleResolutionFilter(transitiveIncoming);
            if (previousTraversal != null) {
                if (previousTraversal.acceptsSameModulesAs(resolutionFilter)) {
                    LOGGER.debug("Changed edges for {} selects same versions as previous traversal. ignoring", this);
                    // Don't need to traverse again, but hang on to the new filter as the set of artifact may have changed
                    previousTraversal = resolutionFilter;
                    return;
                }
                removeOutgoingEdges();
            }

            for (DependencyMetaData dependency : metaData.getDependencies()) {
                ModuleIdentifier targetModuleId = DefaultModuleIdentifier.newId(dependency.getRequested().getGroup(), dependency.getRequested().getName());
                if (isExcluded(resolutionFilter, targetModuleId)) {
                    continue;
                }
                DependencyEdge dependencyEdge = new DependencyEdge(this, dependency, resolutionFilter, resolveState);
                outgoingEdges.add(dependencyEdge);
                target.add(dependencyEdge);
            }
            previousTraversal = resolutionFilter;
        }

        private boolean isExcluded(ModuleResolutionFilter selector, ModuleIdentifier targetModuleId) {
            if(!selector.acceptModule(targetModuleId)) {
                LOGGER.debug("{} is excluded from {}.", targetModuleId, this);
                return true;
            }

            return false;
        }

        public void addIncomingEdge(DependencyEdge dependencyEdge) {
            incomingEdges.add(dependencyEdge);
            resolveState.onMoreSelected(this);
        }

        public void removeIncomingEdge(DependencyEdge dependencyEdge) {
            incomingEdges.remove(dependencyEdge);
            resolveState.onFewerSelected(this);
        }

        public boolean isSelected() {
            return moduleRevision.state == ModuleState.Selected;
        }

        private ModuleResolutionFilter getModuleResolutionFilter(List<DependencyEdge> transitiveEdges) {
            ModuleResolutionFilter resolutionFilter;
            if (transitiveEdges.isEmpty()) {
                resolutionFilter = DefaultModuleResolutionFilter.all();
            } else {
                resolutionFilter = transitiveEdges.get(0).getSelector();
                for (int i = 1; i < transitiveEdges.size(); i++) {
                    DependencyEdge dependencyEdge = transitiveEdges.get(i);
                    resolutionFilter = resolutionFilter.union(dependencyEdge.getSelector());
                }
            }
            resolutionFilter = resolutionFilter.intersect(DefaultModuleResolutionFilter.excludeAny(metaData.getExcludeRules()));
            return resolutionFilter;
        }

        public void removeOutgoingEdges() {
            for (DependencyEdge outgoingDependency : outgoingEdges) {
                outgoingDependency.removeFromTargetConfigurations();
            }
            outgoingEdges.clear();
            previousTraversal = null;
        }

        public void restart(ModuleVersionResolveState selected) {
            // Restarting this configuration after conflict resolution.
            // If this configuration belongs to the select version, queue ourselves up for traversal.
            // If not, then remove our incoming edges, which triggers them to be moved across to the selected configuration
            if (moduleRevision == selected) {
                resolveState.onMoreSelected(this);
            } else {
                for (DependencyEdge dependency : incomingEdges) {
                    dependency.restart(selected);
                }
                incomingEdges.clear();
            }
        }

        public void validate() {
            for (DependencyEdge incomingEdge : incomingEdges) {
                ConfigurationNode fromNode = incomingEdge.from;
                if (!fromNode.isSelected()) {
                    throw new IllegalStateException(String.format("Unexpected state %s for parent node for dependency from %s to %s.", fromNode.moduleRevision.state, fromNode, this));
                }
            }
        }

        public void deselect() {
            removeOutgoingEdges();
        }
    }

    private static class RootConfigurationNode extends ConfigurationNode {
        private RootConfigurationNode(ModuleVersionResolveState moduleRevision, ResolvedConfigurationIdentifier id, ResolveState resolveState) {
            super(id, moduleRevision, resolveState);
        }

        @Override
        public boolean isSelected() {
            return true;
        }

        @Override
        public void deselect() {
        }
    }

    /**
     * Resolution state for a given module version selector.
     */
    private static class ModuleVersionSelectorResolveState {
        final DependencyMetaData dependencyMetaData;
        final DependencyToComponentIdResolver resolver;
        final ResolveState resolveState;
        ModuleVersionResolveException failure;
        ModuleResolveState targetModule;
        ModuleVersionResolveState targetModuleRevision;
        BuildableComponentIdResolveResult idResolveResult;

        private ModuleVersionSelectorResolveState(DependencyMetaData dependencyMetaData, DependencyToComponentIdResolver resolver, ResolveState resolveState) {
            this.dependencyMetaData = dependencyMetaData;
            this.resolver = resolver;
            this.resolveState = resolveState;
            targetModule = resolveState.getModule(new DefaultModuleIdentifier(dependencyMetaData.getRequested().getGroup(), dependencyMetaData.getRequested().getName()));
        }

        @Override
        public String toString() {
            return dependencyMetaData.toString();
        }

        private ModuleVersionResolveException getFailure() {
            return failure != null ? failure : targetModuleRevision.getFailure();
        }

        public ComponentSelectionReason getSelectionReason() {
            return targetModuleRevision == null ? idResolveResult.getSelectionReason() : targetModuleRevision.getSelectionReason();
        }

        public ModuleVersionResolveState getSelected() {
            return targetModule.selected;
        }

        public ModuleResolveState getSelectedModule() {
            return targetModule;
        }

        /**
         * @return The module version, or null if there is a failure to resolve this selector.
         */
        public ModuleVersionResolveState resolveModuleRevisionId() {
            if (targetModuleRevision != null) {
                return targetModuleRevision;
            }
            if (failure != null) {
                return null;
            }

            idResolveResult = new DefaultBuildableComponentIdResolveResult();
            resolver.resolve(dependencyMetaData, idResolveResult);
            if (idResolveResult.getFailure() != null) {
                failure = idResolveResult.getFailure();
                return null;
            }

            targetModuleRevision = resolveState.getRevision(idResolveResult.getModuleVersionId());
            targetModuleRevision.addResolver(this);
            targetModuleRevision.selectionReason = idResolveResult.getSelectionReason();
            targetModule = targetModuleRevision.module;
            targetModule.addSelector(this);

            return targetModuleRevision;
        }

        public void restart(ModuleVersionResolveState moduleRevision) {
            this.targetModuleRevision = moduleRevision;
            this.targetModule = moduleRevision.module;
        }
    }

    private static class DirectDependencyForcingResolver implements ModuleConflictResolver {
        private final ModuleVersionResolveState root;

        private DirectDependencyForcingResolver(ModuleVersionResolveState root) {
            this.root = root;
        }

        public <T extends ComponentResolutionState> T select(Collection<? extends T> candidates) {
            for (ConfigurationNode configuration : root.configurations) {
                for (DependencyEdge outgoingEdge : configuration.outgoingEdges) {
                    if (outgoingEdge.dependencyMetaData.isForce() && candidates.contains(outgoingEdge.targetModuleRevision)) {
                        outgoingEdge.targetModuleRevision.selectionReason = VersionSelectionReasons.FORCED;
                        return (T) outgoingEdge.targetModuleRevision;
                    }
                }
            }
            return null;
        }
    }
}
