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

package org.gradle.api.internal.artifacts.repositories.resolver;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.gradle.api.Nullable;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.artifacts.ModuleVersionPublisher;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ComponentResolvers;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ConfiguredModuleComponentRepository;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.DependencyResolverIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ModuleComponentRepositoryAccess;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser.DescriptorParseContext;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser.MetaDataParseException;
import org.gradle.api.internal.component.ArtifactType;
import org.gradle.internal.SystemProperties;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.component.external.model.*;
import org.gradle.internal.component.model.*;
import org.gradle.internal.hash.HashUtil;
import org.gradle.internal.hash.HashValue;
import org.gradle.internal.resolve.ArtifactResolveException;
import org.gradle.internal.resolve.result.*;
import org.gradle.internal.resource.local.*;
import org.gradle.internal.resource.transfer.CacheAwareExternalResourceAccessor;
import org.gradle.internal.resource.transport.ExternalResourceRepository;
import org.gradle.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.*;

public abstract class ExternalResourceResolver implements ModuleVersionPublisher, ConfiguredModuleComponentRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalResourceResolver.class);

    private List<ResourcePattern> ivyPatterns = new ArrayList<ResourcePattern>();
    private List<ResourcePattern> artifactPatterns = new ArrayList<ResourcePattern>();
    private String name;
    private ComponentResolvers componentResolvers;

    private final ExternalResourceRepository repository;
    private final boolean local;
    private final CacheAwareExternalResourceAccessor cachingResourceAccessor;
    private final LocallyAvailableResourceFinder<ModuleComponentArtifactMetaData> locallyAvailableResourceFinder;
    private final FileStore<ModuleComponentArtifactMetaData> artifactFileStore;

    private final VersionLister versionLister;

    public ExternalResourceResolver(String name,
                                    boolean local,
                                    ExternalResourceRepository repository,
                                    CacheAwareExternalResourceAccessor cachingResourceAccessor,
                                    VersionLister versionLister,
                                    LocallyAvailableResourceFinder<ModuleComponentArtifactMetaData> locallyAvailableResourceFinder,
                                    FileStore<ModuleComponentArtifactMetaData> artifactFileStore) {
        this.name = name;
        this.local = local;
        this.cachingResourceAccessor = cachingResourceAccessor;
        this.versionLister = versionLister;
        this.repository = repository;
        this.locallyAvailableResourceFinder = locallyAvailableResourceFinder;
        this.artifactFileStore = artifactFileStore;
    }

    public String getId() {
        return DependencyResolverIdentifier.forExternalResourceResolver(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDynamicResolveMode() {
        return false;
    }

    public void setComponentResolvers(ComponentResolvers resolver) {
        this.componentResolvers = resolver;
    }

    protected ExternalResourceRepository getRepository() {
        return repository;
    }

    public boolean isLocal() {
        return local;
    }

    private void doListModuleVersions(DependencyMetaData dependency, BuildableModuleVersionListingResolveResult result) {
        ModuleIdentifier module  = new DefaultModuleIdentifier(dependency.getRequested().getGroup(), dependency.getRequested().getName());
        Set<String> versions = new LinkedHashSet<String>();
        VersionPatternVisitor visitor = versionLister.newVisitor(module, versions, result);

        // List modules based on metadata files (artifact version is not considered in listVersionsForAllPatterns())
        IvyArtifactName metaDataArtifact = getMetaDataArtifactName(dependency.getRequested().getName());
        listVersionsForAllPatterns(ivyPatterns, metaDataArtifact, visitor);

        // List modules with missing metadata files
        for (IvyArtifactName otherArtifact : getDependencyArtifactNames(dependency.getRequested().getName(), dependency.getArtifacts())) {
            listVersionsForAllPatterns(artifactPatterns, otherArtifact, visitor);
        }
        result.listed(versions);
    }

    private void listVersionsForAllPatterns(List<ResourcePattern> patternList, IvyArtifactName ivyArtifactName, VersionPatternVisitor visitor) {
        for (ResourcePattern resourcePattern : patternList) {
            visitor.visit(resourcePattern, ivyArtifactName);
        }
    }

    protected void doResolveComponentMetaData(ModuleComponentIdentifier moduleComponentIdentifier, ComponentOverrideMetadata prescribedMetaData, BuildableModuleComponentMetaDataResolveResult result) {
        resolveStaticDependency(moduleComponentIdentifier, prescribedMetaData, result, createArtifactResolver());
    }

    protected final void resolveStaticDependency(ModuleComponentIdentifier moduleVersionIdentifier, ComponentOverrideMetadata prescribedMetaData, BuildableModuleComponentMetaDataResolveResult result, ExternalResourceArtifactResolver artifactResolver) {
        MutableModuleComponentResolveMetaData metaDataArtifactMetaData = parseMetaDataFromArtifact(moduleVersionIdentifier, artifactResolver, result);
        if (metaDataArtifactMetaData != null) {
            LOGGER.debug("Metadata file found for module '{}' in repository '{}'.", moduleVersionIdentifier, getName());
            result.resolved(metaDataArtifactMetaData);
            return;
        }

        MutableModuleComponentResolveMetaData metaDataFromDefaultArtifact = createMetaDataFromDefaultArtifact(moduleVersionIdentifier, prescribedMetaData, artifactResolver, result);
        if (metaDataFromDefaultArtifact != null) {
            LOGGER.debug("Found artifact but no meta-data for module '{}' in repository '{}', using default meta-data.", moduleVersionIdentifier, getName());
            result.resolved(metaDataFromDefaultArtifact);
            return;
        }

        LOGGER.debug("No meta-data file or artifact found for module '{}' in repository '{}'.", moduleVersionIdentifier, getName());
        result.missing();
    }

    @Nullable
    protected MutableModuleComponentResolveMetaData parseMetaDataFromArtifact(ModuleComponentIdentifier moduleComponentIdentifier, ExternalResourceArtifactResolver artifactResolver, ResourceAwareResolveResult result) {
        ModuleComponentArtifactMetaData artifact = getMetaDataArtifactFor(moduleComponentIdentifier);
        LocallyAvailableExternalResource metaDataResource = artifactResolver.resolveArtifact(artifact, result);
        if (metaDataResource == null) {
            return null;
        }

        ExternalResourceResolverDescriptorParseContext context = new ExternalResourceResolverDescriptorParseContext(componentResolvers);
        return parseMetaDataFromResource(moduleComponentIdentifier, metaDataResource, context);
    }

    private MutableModuleComponentResolveMetaData createMetaDataFromDefaultArtifact(ModuleComponentIdentifier moduleComponentIdentifier, ComponentOverrideMetadata overrideMetadata, ExternalResourceArtifactResolver artifactResolver, ResourceAwareResolveResult result) {
        Set<IvyArtifactName> artifacts = overrideMetadata.getArtifacts();
        for (IvyArtifactName artifact : getDependencyArtifactNames(moduleComponentIdentifier.getModule(), artifacts)) {
            if (artifactResolver.artifactExists(new DefaultModuleComponentArtifactMetaData(moduleComponentIdentifier, artifact), result)) {
                return createDefaultComponentResolveMetaData(moduleComponentIdentifier, artifacts);
            }
        }
        return null;
    }

    protected abstract MutableModuleComponentResolveMetaData createDefaultComponentResolveMetaData(ModuleComponentIdentifier moduleComponentIdentifier, Set<IvyArtifactName> artifacts);

    protected abstract MutableModuleComponentResolveMetaData parseMetaDataFromResource(ModuleComponentIdentifier moduleComponentIdentifier, LocallyAvailableExternalResource cachedResource, DescriptorParseContext context);

    private Set<IvyArtifactName> getDependencyArtifactNames(String moduleName, Set<IvyArtifactName> artifacts) {
        Set<IvyArtifactName> artifactSet = Sets.newLinkedHashSet();
        artifactSet.addAll(artifacts);

        if (artifactSet.isEmpty()) {
            artifactSet.add(new DefaultIvyArtifactName(moduleName, "jar", "jar", Collections.<String, String>emptyMap()));
        }

        return artifactSet;
    }

    protected void checkMetadataConsistency(ModuleComponentIdentifier expectedId, ModuleComponentResolveMetaData metadata) throws MetaDataParseException {
        List<String> errors = new ArrayList<String>();
        if (!expectedId.getGroup().equals(metadata.getId().getGroup())) {
            errors.add("bad group: expected='" + expectedId.getGroup() + "' found='" + metadata.getId().getGroup() + "'");
        }
        if (!expectedId.getModule().equals(metadata.getId().getName())) {
            errors.add("bad module name: expected='" + expectedId.getModule() + "' found='" + metadata.getId().getName() + "'");
        }
        if (!expectedId.getVersion().equals(metadata.getId().getVersion())) {
            errors.add("bad version: expected='" + expectedId.getVersion() + "' found='" + metadata.getId().getVersion() + "'");
        }
        if (errors.size() > 0) {
            throw new MetaDataParseException(String.format("inconsistent module metadata found. Descriptor: %s Errors: %s",
                    metadata.getId(), Joiner.on(SystemProperties.getInstance().getLineSeparator()).join(errors)));
        }
    }

    protected abstract boolean isMetaDataArtifact(ArtifactType artifactType);

    protected Set<ModuleComponentArtifactMetaData> findOptionalArtifacts(ModuleComponentResolveMetaData module, String type, String classifier) {
        ModuleComponentArtifactMetaData artifact = module.artifact(type, "jar", classifier);
        if (createArtifactResolver(module.getSource()).artifactExists(artifact, new DefaultResourceAwareResolveResult())) {
            return ImmutableSet.of(artifact);
        }
        return Collections.emptySet();
    }

    private ModuleDescriptorArtifactMetaData getMetaDataArtifactFor(ModuleComponentIdentifier moduleComponentIdentifier) {
        IvyArtifactName ivyArtifactName = getMetaDataArtifactName(moduleComponentIdentifier.getModule());
        DefaultModuleComponentArtifactMetaData defaultModuleComponentArtifactMetaData = new DefaultModuleComponentArtifactMetaData(moduleComponentIdentifier, ivyArtifactName);
        return new DefaultModuleDescriptorArtifactMetaData(defaultModuleComponentArtifactMetaData);
    }

    protected abstract IvyArtifactName getMetaDataArtifactName(String moduleName);

    protected void resolveArtifact(ComponentArtifactMetaData componentArtifact, ModuleSource moduleSource, BuildableArtifactResolveResult result) {
        ModuleComponentArtifactMetaData artifact = (ModuleComponentArtifactMetaData) componentArtifact;

        File localFile;
        try {
            localFile = download(artifact, moduleSource, result);
        } catch (Throwable e) {
            result.failed(new ArtifactResolveException(artifact.getId(), e));
            return;
        }

        if (localFile != null) {
            result.resolved(localFile);
        } else {
            result.notFound(artifact.getId());
        }
    }

    protected File download(ModuleComponentArtifactMetaData artifact, ModuleSource moduleSource, BuildableArtifactResolveResult result) {
        LocallyAvailableExternalResource artifactResource = createArtifactResolver(moduleSource).resolveArtifact(artifact, result);
        if (artifactResource == null) {
            return null;
        }

        return artifactResource.getLocalResource().getFile();
    }

    protected ExternalResourceArtifactResolver createArtifactResolver() {
        return createArtifactResolver(ivyPatterns, artifactPatterns);
    }

    protected ExternalResourceArtifactResolver createArtifactResolver(List<ResourcePattern> ivyPatterns, List<ResourcePattern> artifactPatterns) {
        return new DefaultExternalResourceArtifactResolver(repository, locallyAvailableResourceFinder, ivyPatterns, artifactPatterns, artifactFileStore, cachingResourceAccessor);
    }

    protected ExternalResourceArtifactResolver createArtifactResolver(ModuleSource moduleSource) {
        return createArtifactResolver();
    }

    public void publish(IvyModulePublishMetaData moduleVersion) throws IOException {
        for (IvyModuleArtifactPublishMetaData artifact : moduleVersion.getArtifacts()) {
            publish(new DefaultModuleComponentArtifactMetaData(artifact.getId()), artifact.getFile());
        }
    }

    private void publish(ModuleComponentArtifactMetaData artifact, File src) throws IOException {
        ResourcePattern destinationPattern;
        if ("ivy".equals(artifact.getName().getType()) && !ivyPatterns.isEmpty()) {
            destinationPattern = ivyPatterns.get(0);
        } else if (!artifactPatterns.isEmpty()) {
            destinationPattern = artifactPatterns.get(0);
        } else {
            throw new IllegalStateException("impossible to publish " + artifact + " using " + this + ": no artifact pattern defined");
        }
        URI destination = destinationPattern.getLocation(artifact).getUri();

        put(src, destination);
        LOGGER.info("Published {} to {}", artifact, destination);
    }

    private void put(File src, URI destination) throws IOException {
        repository.withProgressLogging().put(new FileLocalResource(src), destination);
        putChecksum(src, destination);
    }

    private void putChecksum(File source, URI destination) throws IOException {
        byte[] checksumFile = createChecksumFile(source, "SHA1", 40);
        URI checksumDestination = URI.create(destination + ".sha1");
        repository.put(new ByteArrayLocalResource(checksumFile), checksumDestination);
    }

    private byte[] createChecksumFile(File src, String algorithm, int checksumLength) {
        HashValue hash = HashUtil.createHash(src, algorithm);
        String formattedHashString = hash.asZeroPaddedHexString(checksumLength);
        try {
            return formattedHashString.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    protected void addIvyPattern(ResourcePattern pattern) {
        ivyPatterns.add(pattern);
    }

    protected void addArtifactPattern(ResourcePattern pattern) {
        artifactPatterns.add(pattern);
    }

    public List<String> getIvyPatterns() {
        return CollectionUtils.collect(ivyPatterns, new Transformer<String, ResourcePattern>() {
            public String transform(ResourcePattern original) {
                return original.getPattern();
            }
        });
    }

    public List<String> getArtifactPatterns() {
        return CollectionUtils.collect(artifactPatterns, new Transformer<String, ResourcePattern>() {
            public String transform(ResourcePattern original) {
                return original.getPattern();
            }
        });
    }

    protected void setIvyPatterns(Iterable<? extends ResourcePattern> patterns) {
        ivyPatterns.clear();
        CollectionUtils.addAll(ivyPatterns, patterns);
    }

    protected void setArtifactPatterns(List<ResourcePattern> patterns) {
        artifactPatterns = patterns;
    }

    public abstract boolean isM2compatible();

    protected abstract class AbstractRepositoryAccess implements ModuleComponentRepositoryAccess {
        public void resolveModuleArtifacts(ComponentResolveMetaData component, ArtifactType artifactType, BuildableArtifactSetResolveResult result) {
            ModuleComponentResolveMetaData moduleMetaData = (ModuleComponentResolveMetaData) component;

            if (artifactType == ArtifactType.JAVADOC) {
                resolveJavadocArtifacts(moduleMetaData, result);
            } else if (artifactType == ArtifactType.SOURCES) {
                resolveSourceArtifacts(moduleMetaData, result);
            } else if (isMetaDataArtifact(artifactType)) {
                resolveMetaDataArtifacts(moduleMetaData, result);
            }
        }

        public void resolveModuleArtifacts(ComponentResolveMetaData component, ComponentUsage componentUsage, BuildableArtifactSetResolveResult result) {
             resolveConfigurationArtifacts((ModuleComponentResolveMetaData) component, componentUsage, result);
        }

        protected abstract void resolveConfigurationArtifacts(ModuleComponentResolveMetaData module, ComponentUsage usage, BuildableArtifactSetResolveResult result);

        protected abstract void resolveMetaDataArtifacts(ModuleComponentResolveMetaData module, BuildableArtifactSetResolveResult result);

        protected abstract void resolveJavadocArtifacts(ModuleComponentResolveMetaData module, BuildableArtifactSetResolveResult result);

        protected abstract void resolveSourceArtifacts(ModuleComponentResolveMetaData module, BuildableArtifactSetResolveResult result);

    }

    protected abstract class LocalRepositoryAccess extends AbstractRepositoryAccess {
        @Override
        public String toString() {
            return "local > " + ExternalResourceResolver.this.toString();
        }

        public final void listModuleVersions(DependencyMetaData dependency, BuildableModuleVersionListingResolveResult result) {
        }

        public final void resolveComponentMetaData(ModuleComponentIdentifier moduleComponentIdentifier, ComponentOverrideMetadata requestMetaData, BuildableModuleComponentMetaDataResolveResult result) {
        }

        protected final void resolveMetaDataArtifacts(ModuleComponentResolveMetaData module, BuildableArtifactSetResolveResult result) {
            ModuleDescriptorArtifactMetaData artifact = getMetaDataArtifactFor(module.getComponentId());
            result.resolved(Collections.singleton(artifact));
        }

        public void resolveArtifact(ComponentArtifactMetaData artifact, ModuleSource moduleSource, BuildableArtifactResolveResult result) {

        }
    }

    protected abstract class RemoteRepositoryAccess extends AbstractRepositoryAccess {
        @Override
        public String toString() {
            return "remote > " + ExternalResourceResolver.this.toString();
        }

        public final void listModuleVersions(DependencyMetaData dependency, BuildableModuleVersionListingResolveResult result) {
            doListModuleVersions(dependency, result);
        }

        public final void resolveComponentMetaData(ModuleComponentIdentifier moduleComponentIdentifier, ComponentOverrideMetadata requestMetaData, BuildableModuleComponentMetaDataResolveResult result) {
            doResolveComponentMetaData(moduleComponentIdentifier, requestMetaData, result);
        }

        @Override
        public void resolveModuleArtifacts(ComponentResolveMetaData component, ArtifactType artifactType, BuildableArtifactSetResolveResult result) {
            super.resolveModuleArtifacts(component, artifactType, result);
            checkArtifactsResolved(component, artifactType, result);
        }

        @Override
        public void resolveModuleArtifacts(ComponentResolveMetaData component, ComponentUsage componentUsage, BuildableArtifactSetResolveResult result) {
            super.resolveModuleArtifacts(component, componentUsage, result);
            checkArtifactsResolved(component, componentUsage, result);
        }

        private void checkArtifactsResolved(ComponentResolveMetaData component, Object context, BuildableArtifactSetResolveResult result) {
            if (!result.hasResult()) {
                result.failed(new ArtifactResolveException(component.getComponentId(),
                        String.format("Cannot locate %s for '%s' in repository '%s'", context, component, name)));
            }
        }

        protected final void resolveMetaDataArtifacts(ModuleComponentResolveMetaData module, BuildableArtifactSetResolveResult result) {
            // Meta data  artifacts are determined locally
        }

        public void resolveArtifact(ComponentArtifactMetaData artifact, ModuleSource moduleSource, BuildableArtifactResolveResult result) {
            ExternalResourceResolver.this.resolveArtifact(artifact, moduleSource, result);
        }
    }
}
