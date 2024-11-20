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

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * A set of mock dynamo values/responses that can be used in unit tests.
 */
public class DynamoDbMocks {

    static final String defaultKeyAttribute = AbstractDynamoDbConfigurationSource.defaultKeyAttribute;
    static final String defaultValueAttribute = AbstractDynamoDbConfigurationSource.defaultValueAttribute;
    static final String defaultContextKeyAttribute = DynamoDbDeploymentContextTableCache.defaultContextKeyAttribute;
    static final String defaultContextValueAttribute = DynamoDbDeploymentContextTableCache.defaultContextValueAttribute;

    public static final Collection<Map<String, AttributeValue>> basicResultValues1 = new LinkedList<Map<String, AttributeValue>>();
    public static final Collection<Map<String, AttributeValue>> basicResultValues2 = new LinkedList<Map<String, AttributeValue>>();
    public static final ScanResponse basicScanResult1;
    public static final ScanResponse basicScanResult2;

    public static final Collection<Map<String, AttributeValue>> contextResultValues1 = new LinkedList<Map<String, AttributeValue>>();
    public static final Collection<Map<String, AttributeValue>> contextResultValues2 = new LinkedList<Map<String, AttributeValue>>();
    public static final ScanResponse contextScanResult1;
    public static final ScanResponse contextScanResult2;


    static {
        //Basic results config
        Map<String, AttributeValue> basicRow1 = new HashMap<String, AttributeValue>();
        basicRow1.put(defaultKeyAttribute, AttributeValue.builder().s("foo").build());
        basicRow1.put(defaultValueAttribute, AttributeValue.builder().s("bar").build());
        basicResultValues1.add(basicRow1);

        Map<String, AttributeValue> basicRow2 = new HashMap<String, AttributeValue>();
        basicRow2.put(defaultKeyAttribute, AttributeValue.builder().s("goo").build());
        basicRow2.put(defaultValueAttribute, AttributeValue.builder().s("goo").build());
        basicResultValues1.add(basicRow2);

        Map<String, AttributeValue> basicRow3 = new HashMap<String, AttributeValue>();
        basicRow3.put(defaultKeyAttribute, AttributeValue.builder().s("boo").build());
        basicRow3.put(defaultValueAttribute, AttributeValue.builder().s("who").build());
        basicResultValues1.add(basicRow3);

        //Result2
        Map<String, AttributeValue> updatedBasicRow = new HashMap<String, AttributeValue>();
        updatedBasicRow.put(defaultKeyAttribute, AttributeValue.builder().s("goo").build());
        updatedBasicRow.put(defaultValueAttribute, AttributeValue.builder().s("foo").build());
        basicResultValues2.add(updatedBasicRow);

        basicResultValues2.add(basicRow1);
        basicResultValues2.add(updatedBasicRow);
        basicResultValues2.add(basicRow3);

        basicScanResult1 = ScanResponse.builder().items(basicResultValues1).lastEvaluatedKey(null).build();
        basicScanResult2 = ScanResponse.builder().items(basicResultValues2).lastEvaluatedKey(null).build();

        //DeploymentContext results config
        Map<String, AttributeValue> contextRow1 = new HashMap<String, AttributeValue>();
        contextRow1.put(defaultKeyAttribute, AttributeValue.builder().s("foo").build());
        contextRow1.put(defaultValueAttribute, AttributeValue.builder().s("bar").build());
        contextRow1.put(defaultContextKeyAttribute, AttributeValue.builder().s("environment").build());
        contextRow1.put(defaultContextValueAttribute, AttributeValue.builder().s("test").build());
        contextResultValues1.add(contextRow1);

        Map<String, AttributeValue> contextRow2 = new HashMap<String, AttributeValue>();
        contextRow2.put(defaultKeyAttribute, AttributeValue.builder().s("goo").build());
        contextRow2.put(defaultValueAttribute, AttributeValue.builder().s("goo").build());
        contextRow2.put(defaultContextKeyAttribute, AttributeValue.builder().s("environment").build());
        contextRow2.put(defaultContextValueAttribute, AttributeValue.builder().s("test").build());
        contextResultValues1.add(contextRow2);

        Map<String, AttributeValue> contextRow3 = new HashMap<String, AttributeValue>();
        contextRow3.put(defaultKeyAttribute, AttributeValue.builder().s("boo").build());
        contextRow3.put(defaultValueAttribute, AttributeValue.builder().s("who").build());
        contextRow3.put(defaultContextKeyAttribute, AttributeValue.builder().s("environment").build());
        contextRow3.put(defaultContextValueAttribute, AttributeValue.builder().s("test").build());
        contextResultValues1.add(contextRow3);

        contextResultValues1.add(basicRow1);

        //Result2
        contextResultValues2.add(contextRow1);
        contextResultValues2.add(contextRow3);

        Map<String, AttributeValue> contextRow4 = new HashMap<String, AttributeValue>();
        contextRow4.put(defaultKeyAttribute, AttributeValue.builder().s("goo").build());
        contextRow4.put(defaultValueAttribute, AttributeValue.builder().s("foo").build());
        contextRow4.put(defaultContextKeyAttribute, AttributeValue.builder().s("environment").build());
        contextRow4.put(defaultContextValueAttribute, AttributeValue.builder().s("prod").build());
        contextResultValues2.add(contextRow4);

        Map<String, AttributeValue> updatedContextRow = new HashMap<String, AttributeValue>();
        updatedContextRow.put(defaultKeyAttribute, AttributeValue.builder().s("goo").build());
        updatedContextRow.put(defaultValueAttribute, AttributeValue.builder().s("foo").build());
        updatedContextRow.put(defaultContextKeyAttribute, AttributeValue.builder().s("environment").build());
        updatedContextRow.put(defaultContextValueAttribute, AttributeValue.builder().s("test").build());
        contextResultValues2.add(updatedContextRow);

        contextResultValues2.add(basicRow1);

        //Create results from initialized values
        contextScanResult1 = ScanResponse.builder().items(contextResultValues1).lastEvaluatedKey(null).build();
        contextScanResult2 = ScanResponse.builder().items(contextResultValues2).lastEvaluatedKey(null).build();
    }
}
