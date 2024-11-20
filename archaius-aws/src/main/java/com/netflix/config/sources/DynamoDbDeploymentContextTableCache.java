/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.config.sources;

import com.netflix.config.DeploymentContext;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.config.PropertyWithDeploymentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * This leverages some of the semantics of the PollingSource in order to have one place where the full table scan from
 * Dynamo is cached.  It is mean to be consumed but a number of DeploymentContext aware sources to keep them from all
 * having to load the table separately.
 */
public class DynamoDbDeploymentContextTableCache extends AbstractDynamoDbConfigurationSource<PropertyWithDeploymentContext> {
    private static Logger log = LoggerFactory.getLogger(DynamoDbDeploymentContextTableCache.class);

    //Property names
    static final String contextKeyAttributePropertyName = "com.netflix.config.dynamo.contextKeyAttributeName";
    static final String contextValueAttributePropertyName = "com.netflix.config.dynamo.contextValueAttributeName";

    //Property defaults
    static final String defaultContextKeyAttribute = "contextKey";
    static final String defaultContextValueAttribute = "contextValue";

    //Dynamic Properties
    private final DynamicStringProperty contextKeyAttributeName = DynamicPropertyFactory.getInstance()
            .getStringProperty(contextKeyAttributePropertyName, defaultContextKeyAttribute);
    private final DynamicStringProperty contextValueAttributeName = DynamicPropertyFactory.getInstance()
            .getStringProperty(contextValueAttributePropertyName, defaultContextValueAttribute);

    // Delay defaults
    static final int defaultInitialDelayMillis = 30000;
    static final int defaultDelayMillis = 60000;

    private final int initialDelayMillis;
    private final int delayMillis;

    private ScheduledExecutorService executor;
    private volatile Map<String, PropertyWithDeploymentContext> cachedTable = new HashMap<String, PropertyWithDeploymentContext>();

    public DynamoDbDeploymentContextTableCache(DynamoDbClient dbClient) {
        this(dbClient, defaultInitialDelayMillis, defaultDelayMillis);
    }

    public DynamoDbDeploymentContextTableCache(DynamoDbClient dbClient, int initialDelayMillis, int delayMillis) {
        super(dbClient);
        this.initialDelayMillis = initialDelayMillis;
        this.delayMillis = delayMillis;
        start();
    }

    private synchronized void schedule(Runnable runnable) {
        executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "pollingDynamoTableCache");
                t.setDaemon(true);
                return t;
            }
        });
        executor.scheduleWithFixedDelay(runnable, initialDelayMillis, delayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop polling the source table
     */
    public void stop() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    private void start() {
        cachedTable = loadPropertiesFromTable(tableName.get());
        schedule(getPollingRunnable());
    }

    private Runnable getPollingRunnable() {
        return new Runnable() {
            public void run() {
                log.debug("Dynamo cached polling started");
                try {
                    Map<String, PropertyWithDeploymentContext> newMap = loadPropertiesFromTable(tableName.get());
                    cachedTable = newMap;
                } catch (Throwable e) {
                    log.error("Error getting result from polling source", e);
                    return;
                }
            }
        };
    }

    /**
     * Scan the table in dynamo and create a map with the results.  In this case the map has a complex type as the value,
     * so that Deployment Context is taken into account.
     *
     * @param table
     * @return
     */
    @Override
    protected Map<String, PropertyWithDeploymentContext> loadPropertiesFromTable(String table) {
        Map<String, PropertyWithDeploymentContext> propertyMap = new HashMap<String, PropertyWithDeploymentContext>();
        Map<String, AttributeValue> lastKeysEvaluated = null;
        do {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(table)
                    .exclusiveStartKey(lastKeysEvaluated)
                    .build();
            ScanResponse result = dbScanWithThroughputBackOff(scanRequest);
            for (Map<String, AttributeValue> item : result.items()) {
                String keyVal = item.get(keyAttributeName.get()).s();

                //Need to deal with the fact that these attributes might not exist
                DeploymentContext.ContextKey contextKey = item.containsKey(contextKeyAttributeName.get()) ? DeploymentContext.ContextKey.valueOf(item.get(contextKeyAttributeName.get()).s()) : null;
                String contextVal = item.containsKey(contextValueAttributeName.get()) ? item.get(contextValueAttributeName.get()).s() : null;
                String key = keyVal + ";" + contextKey + ";" + contextVal;
                propertyMap.put(key,
                        new PropertyWithDeploymentContext(
                                contextKey,
                                contextVal,
                                keyVal,
                                item.get(valueAttributeName.get()).s()
                        ));
            }
            lastKeysEvaluated = result.lastEvaluatedKey();
        } while (!lastKeysEvaluated.isEmpty());
        return propertyMap;
    }

    /**
     * Get the current values in the cache.
     *
     * @return
     */
    public Collection<PropertyWithDeploymentContext> getProperties() {
        return cachedTable.values();
    }
}
