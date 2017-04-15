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

package org.gradle.api.internal.changedetection.state;

import org.gradle.api.internal.cache.StringInterner;
import org.gradle.internal.serialize.Decoder;
import org.gradle.internal.serialize.Encoder;
import org.gradle.internal.serialize.Serializer;

import java.util.HashMap;
import java.util.Map;

class DefaultFileSnapshotterSerializer implements Serializer<DefaultFileCollectionSnapshotter.FileCollectionSnapshotImpl> {
    private final StringInterner stringInterner;

    public DefaultFileSnapshotterSerializer(StringInterner stringInterner) {
        this.stringInterner = stringInterner;
    }

    public DefaultFileCollectionSnapshotter.FileCollectionSnapshotImpl read(Decoder decoder) throws Exception {
        Map<String, DefaultFileCollectionSnapshotter.IncrementalFileSnapshot> snapshots = new HashMap<String, DefaultFileCollectionSnapshotter.IncrementalFileSnapshot>();
        DefaultFileCollectionSnapshotter.FileCollectionSnapshotImpl snapshot = new DefaultFileCollectionSnapshotter.FileCollectionSnapshotImpl(snapshots);
        int snapshotsCount = decoder.readSmallInt();
        for (int i = 0; i < snapshotsCount; i++) {
            String key = stringInterner.intern(decoder.readString());
            byte fileSnapshotKind = decoder.readByte();
            if (fileSnapshotKind == 1) {
                snapshots.put(key, DefaultFileCollectionSnapshotter.DirSnapshot.getInstance());
            } else if (fileSnapshotKind == 2) {
                snapshots.put(key, DefaultFileCollectionSnapshotter.MissingFileSnapshot.getInstance());
            } else if (fileSnapshotKind == 3) {
                byte hashSize = decoder.readByte();
                byte[] hash = new byte[hashSize];
                decoder.readBytes(hash);
                snapshots.put(key, new DefaultFileCollectionSnapshotter.FileHashSnapshot(hash));
            } else {
                throw new RuntimeException("Unable to read serialized file collection snapshot. Unrecognized value found in the data stream.");
            }
        }
        return snapshot;
    }

    public void write(Encoder encoder, DefaultFileCollectionSnapshotter.FileCollectionSnapshotImpl value) throws Exception {
        encoder.writeSmallInt(value.snapshots.size());
        for (String key : value.snapshots.keySet()) {
            encoder.writeString(key);
            DefaultFileCollectionSnapshotter.IncrementalFileSnapshot incrementalFileSnapshot = value.snapshots.get(key);
            if (incrementalFileSnapshot instanceof DefaultFileCollectionSnapshotter.DirSnapshot) {
                encoder.writeByte((byte) 1);
            } else if (incrementalFileSnapshot instanceof DefaultFileCollectionSnapshotter.MissingFileSnapshot) {
                encoder.writeByte((byte) 2);
            } else if (incrementalFileSnapshot instanceof DefaultFileCollectionSnapshotter.FileHashSnapshot) {
                encoder.writeByte((byte) 3);
                byte[] hash = ((DefaultFileCollectionSnapshotter.FileHashSnapshot) incrementalFileSnapshot).hash;
                encoder.writeByte((byte) hash.length);
                encoder.writeBytes(hash);
            }
        }
    }
}
