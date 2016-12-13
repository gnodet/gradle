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

package org.gradle.internal.cleanup;

import org.gradle.cache.CacheBuilder;
import org.gradle.cache.CacheRepository;
import org.gradle.cache.PersistentCache;
import org.gradle.cache.internal.FileLockManager;
import org.gradle.internal.Factory;
import org.gradle.util.GradleVersion;

import java.io.File;
import java.util.Collections;

import static org.gradle.cache.internal.filelock.LockOptionsBuilder.mode;

public class PersistedGradleVersionCleanupStrategy implements BuildOutputCleanupStrategy {

    private final CacheRepository cacheRepository;
    private final File cacheBaseDir;

    public PersistedGradleVersionCleanupStrategy(CacheRepository cacheRepository, File cacheBaseDir) {
        this.cacheRepository = cacheRepository;
        this.cacheBaseDir = cacheBaseDir;
    }

    @Override
    public boolean needsCleanup() {
        final PersistentCache cache = createCache();

        try {
            return cache.useCache("build cleanup cache", new Factory<Boolean>() {
                @Override
                public Boolean create() {
                    File markerFile = new File(cache.getBaseDir(), "built.bin");
                    return !markerFile.exists();
                }
            });
        } finally {
            cache.close();
        }
    }

    private PersistentCache createCache() {
        return cacheRepository
            .cache(cacheBaseDir)
            .withCrossVersionCache(CacheBuilder.LockTarget.CachePropertiesFile)
            .withDisplayName("persisted gradle version state cache")
            .withLockOptions(mode(FileLockManager.LockMode.None).useCrossVersionImplementation())
            .withProperties(Collections.singletonMap("gradle.version", GradleVersion.current().getVersion()))
            .open();
    }
}
