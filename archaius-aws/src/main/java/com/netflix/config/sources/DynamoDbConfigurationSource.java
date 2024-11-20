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

import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * User: gorzell
 * Date: 8/6/12
 * This source can be used for basic Dynamo support where there is no scoping of the properties.  It assume that you
 * provide a table with just key value pairs and the last value read wins if there are multiple rows with the same key.
 */
public class DynamoDbConfigurationSource extends AbstractDynamoDbConfigurationSource<Object> implements PolledConfigurationSource {
    private static final Logger log = LoggerFactory.getLogger(DynamoDbConfigurationSource.class);

    public DynamoDbConfigurationSource(DynamoDbClient dbClient) {
        super(dbClient);
    }

    @Override
    protected synchronized Map<String, Object> loadPropertiesFromTable(String table) {
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        Map<String, AttributeValue> lastKeysEvaluated = null;
        do {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(table)
                    .exclusiveStartKey(lastKeysEvaluated)
                    .build();
            ScanResponse result = dbScanWithThroughputBackOff(scanRequest);
            for (Map<String, AttributeValue> item : result.items()) {
                propertyMap.put(item.get(keyAttributeName.get()).s(), item.get(valueAttributeName.get()).s());
            }
            lastKeysEvaluated = result.lastEvaluatedKey();
        } while (!lastKeysEvaluated.isEmpty());
        return propertyMap;
    }

    @Override
    public PollResult poll(boolean initial, Object checkPoint) throws Exception {
        String table = tableName.get();
        Map<String, Object> map = loadPropertiesFromTable(table);
        log.debug("Successfully polled Dynamo for a new configuration based on table:" + table);
        return PollResult.createFull(map);
    }
}
