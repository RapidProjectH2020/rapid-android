/*
 * Copyright 2010 the original author or authors.
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
package org.gradle.api.tasks.bundling;

import org.apache.tools.zip.ZipOutputStream;
import org.gradle.api.Incubating;
import org.gradle.api.internal.DocumentationRegistry;
import org.gradle.api.internal.file.archive.ZipCopyAction;
import org.gradle.api.internal.file.copy.*;

/**
 * Assembles a ZIP archive.
 *
 * The default is to compress the contents of the zip.
 */
public class Zip extends AbstractArchiveTask {
    public static final String ZIP_EXTENSION = "zip";
    private ZipEntryCompression entryCompression = ZipEntryCompression.DEFLATED;
    private boolean allowZip64;
    private String encoding;

    public Zip() {
        setExtension(ZIP_EXTENSION);
        allowZip64 = false;
    }

    protected ZipCompressor getCompressor() {
        switch (entryCompression) {
            case DEFLATED:
                return new DefaultZipCompressor(allowZip64, ZipOutputStream.DEFLATED);
            case STORED:
                return new DefaultZipCompressor(allowZip64, ZipOutputStream.STORED);
            default:
                throw new IllegalArgumentException(String.format("Unknown Compression type %s", entryCompression));
        }
    }

    @Override
    protected CopyAction createCopyAction() {
        DocumentationRegistry documentationRegistry = getServices().get(DocumentationRegistry.class);
        return new ZipCopyAction(getArchivePath(), getCompressor(), documentationRegistry, encoding);
    }

    /**
     * Returns the compression level of the entries of the archive. If set to {@link ZipEntryCompression#DEFLATED} (the default), each entry is
     * compressed using the DEFLATE algorithm. If set to {@link ZipEntryCompression#STORED} the entries of the archive are left uncompressed.
     *
     * @return the compression level of the archive contents.
     */
    public ZipEntryCompression getEntryCompression() {
        return entryCompression;
    }

    /**
     * Sets the compression level of the entries of the archive. If set to {@link ZipEntryCompression#DEFLATED} (the default), each entry is
     * compressed using the DEFLATE algorithm. If set to {@link ZipEntryCompression#STORED} the entries of the archive are left uncompressed.
     *
     * @param entryCompression {@code STORED} or {@code DEFLATED}
     */
    public void setEntryCompression(ZipEntryCompression entryCompression) {
        this.entryCompression = entryCompression;
    }

    /**
     * Enables building zips with more than 65535 files or bigger than 4GB.
     *
     * @see #isZip64()
     */
    @Incubating
    public void setZip64(boolean allowZip64) {
        this.allowZip64 = allowZip64;
    }

    /**
     * Whether the zip can contain more than 65535 files and/or support files greater than 4GB in size.
     * <p>
     * The standard zip format has hard limits on file size and count.
     * The <a href="http://en.wikipedia.org/wiki/Zip_(file_format)#ZIP64">Zip64 format extension</a>
     * practically removes these limits and is therefore required for building large zips.
     * <p>
     * However, not all Zip readers support the Zip64 extensions.
     * Notably, the {@link java.util.zip.ZipInputStream} JDK class does not support Zip64 for versions earlier than Java 7.
     * This means you should not enable this property if you are building JARs to be used with Java 6 and earlier runtimes.
     */
    @Incubating
    public boolean isZip64() {
        return allowZip64;
    }

    /**
     * The encoding to use for filenames and the file comment of the archive.
     *
     * @return null if using the platform's default character encoding.
     */
    public String getEncoding() {
        return this.encoding;
    }

    /**
     * The encoding to use for filenames and the file comment of the archive.
     * Defaults to the platform's default character encoding.
     * 
     * @param encoding the encoding value
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * DO NOT REMOVE.
     *
     * Do not use - kept for binary compatibility.
     */
    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    protected class ZipCopyActionImpl {
    }
}
