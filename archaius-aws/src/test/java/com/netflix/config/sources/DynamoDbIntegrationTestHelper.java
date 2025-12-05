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

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamoDbIntegrationTestHelper {

  static void createTable(DynamoDbClient dbClient, String tableName) throws InterruptedException {
    // TODO check to make sure the table isn't being created or deleted.
    KeySchemaElement hashKey = KeySchemaElement.builder().attributeName(DynamoDbConfigurationSource.defaultKeyAttribute).keyType(KeyType.HASH).build();

    ProvisionedThroughput provisionedThroughput = ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build();

    dbClient.createTable(CreateTableRequest.builder().tableName(tableName).keySchema(hashKey).provisionedThroughput(provisionedThroughput).build());

    while (!dbClient.describeTable(DescribeTableRequest.builder().tableName(tableName).build()).table().tableStatus().name().equalsIgnoreCase("active")) {
      Thread.sleep(10000);
    }
  }

  static void addElements(DynamoDbClient dbClient, String tableName) {
    Map<String, List<WriteRequest>> requestMap = new HashMap<String, List<WriteRequest>>(1);
    List<WriteRequest> writeList = new ArrayList<WriteRequest>(3);

    Map<String, AttributeValue> item1 = new HashMap<String, AttributeValue>(1);
    item1.put(DynamoDbConfigurationSource.defaultKeyAttribute, AttributeValue.builder().s("test1").build());
    item1.put(DynamoDbConfigurationSource.defaultValueAttribute, AttributeValue.builder().s("val1").build());
    writeList.add(WriteRequest.builder().putRequest(PutRequest.builder().item(item1).build()).build());

    HashMap<String, AttributeValue> item2 = new HashMap<String, AttributeValue>(1);
    item2.put(DynamoDbConfigurationSource.defaultKeyAttribute, AttributeValue.builder().s("test2").build());
    item2.put(DynamoDbConfigurationSource.defaultValueAttribute, AttributeValue.builder().s("val2").build());
    writeList.add(WriteRequest.builder().putRequest(PutRequest.builder().item(item2).build()).build());

    HashMap<String, AttributeValue> item3 = new HashMap<String, AttributeValue>(1);
    item3.put(DynamoDbConfigurationSource.defaultKeyAttribute, AttributeValue.builder().s("test3").build());
    item3.put(DynamoDbConfigurationSource.defaultValueAttribute, AttributeValue.builder().s("val3").build());
    writeList.add(WriteRequest.builder().putRequest(PutRequest.builder().item(item3).build()).build());

    requestMap.put(tableName, writeList);

    BatchWriteItemRequest request = BatchWriteItemRequest.builder().requestItems(requestMap).build();
    dbClient.batchWriteItem(request);
  }

  static void updateValues(DynamoDbClient dbClient, String tableName) {

    Map<String, AttributeValue> key1 = new HashMap<String, AttributeValue>(1);
    key1.put("test1", AttributeValue.builder().s("HASH").build());

    Map<String, AttributeValueUpdate> item1 = new HashMap<String, AttributeValueUpdate>(1);
    item1.put(DynamoDbConfigurationSource.defaultValueAttribute,
      AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().s("vala").build()).build());

    dbClient.updateItem(UpdateItemRequest.builder().tableName(tableName).key(key1).attributeUpdates(item1).build());

    Map<String, AttributeValue> key2 = new HashMap<String, AttributeValue>(1);
    key2.put("test2", AttributeValue.builder().s("HASH").build());

    HashMap<String, AttributeValueUpdate> item2 = new HashMap<String, AttributeValueUpdate>(1);
    item2.put(DynamoDbConfigurationSource.defaultValueAttribute,
      AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().s("valb").build()).build());

    dbClient.updateItem(UpdateItemRequest.builder().tableName(tableName).key(key2).attributeUpdates(item2).build());

    Map<String, AttributeValue> key3 = new HashMap<String, AttributeValue>(1);
    key3.put("test3", AttributeValue.builder().s("HASH").build());

    HashMap<String, AttributeValueUpdate> item3 = new HashMap<String, AttributeValueUpdate>(1);
    item3.put(DynamoDbConfigurationSource.defaultValueAttribute,
      AttributeValueUpdate.builder().action(AttributeAction.PUT).value(AttributeValue.builder().s("valc").build()).build());

    dbClient.updateItem(UpdateItemRequest.builder().tableName(tableName).key(key3).attributeUpdates(item3).build());
  }

  static void removeTable(DynamoDbClient dbClient, String tableName) {
    // TODO check to make sure the table isn't being created or deleted.
    if (dbClient != null) {
      dbClient.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
    }
  }
}
