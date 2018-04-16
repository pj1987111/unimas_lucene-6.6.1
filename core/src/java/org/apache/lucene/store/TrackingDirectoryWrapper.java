/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.store;


import org.apache.lucene.codecs.lucene54.Lucene54DocValuesFormat;
import org.apache.lucene.util.unimas.PathUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A delegating Directory that records which files were
 * written to and deleted.
 */
public final class TrackingDirectoryWrapper extends FilterDirectory {

    private final Set<String> createdFileNames = Collections.synchronizedSet(new HashSet<String>());

    public TrackingDirectoryWrapper(Directory in) {
        super(in);
    }

    @Override
    public void deleteFile(String name) throws IOException {
//        in.deleteFile(name);
        Directory tempDir = in;
        if (PathUtil.newPath != null && PathUtil.newPath.length() > 0 &&
                (name.endsWith(Lucene54DocValuesFormat.DATA_EXTENSION)||name.endsWith(Lucene54DocValuesFormat.META_EXTENSION)
                        ||name.endsWith(Lucene54DocValuesFormat.INDEX_EXTENSION))) {
            tempDir = PathUtil.reCreatePath(in);
        }
        tempDir.deleteFile(name);
        createdFileNames.remove(name);
    }

    @Override
    public IndexOutput createOutput(String name, IOContext context) throws IOException {
//        in.createOutput(name, context);
        Directory tempDir = in;
        if (PathUtil.newPath != null && PathUtil.newPath.length() > 0 &&
                (name.endsWith(Lucene54DocValuesFormat.DATA_EXTENSION)||name.endsWith(Lucene54DocValuesFormat.META_EXTENSION)
                        ||name.endsWith(Lucene54DocValuesFormat.INDEX_EXTENSION))) {
            tempDir = PathUtil.reCreatePath(in);
        }
        IndexOutput output = tempDir.createOutput(name, context);
        createdFileNames.add(name);
        return output;
    }

    @Override
    public IndexOutput createTempOutput(String prefix, String suffix, IOContext context)
            throws IOException {
        IndexOutput tempOutput = in.createTempOutput(prefix, suffix, context);
        createdFileNames.add(tempOutput.getName());
        return tempOutput;
    }

    @Override
    public void copyFrom(Directory from, String src, String dest, IOContext context) throws IOException {
        in.copyFrom(from, src, dest, context);
        createdFileNames.add(dest);
    }

    @Override
    public void rename(String source, String dest) throws IOException {
        in.rename(source, dest);
        synchronized (createdFileNames) {
            createdFileNames.add(dest);
            createdFileNames.remove(source);
        }
    }

    /**
     * NOTE: returns a copy of the created files.
     */
    public Set<String> getCreatedFiles() {
        return new HashSet<>(createdFileNames);
    }

    public void clearCreatedFiles() {
        createdFileNames.clear();
    }
}
