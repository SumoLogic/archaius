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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static com.netflix.config.sources.DynamoDbIntegrationTestHelper.*;
import static org.junit.Assert.assertEquals;

/**
 * User: gorzell
 * Date: 8/7/12
 */
public class DynamoDbConfigurationSourceIntegrationTest {
    private static final String tableName = DynamoDbConfigurationSource.defaultTable + "UNITTEST";
    private static DynamoDbClient dbClient;

    @BeforeClass
    public static void setUpClass() throws Exception {
        try {
            dbClient = DynamoDbClient.builder().credentialsProvider(DefaultCredentialsProvider.create()).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.setProperty("com.netflix.config.dynamo.tableName", tableName);
        if (dbClient != null) {
            createTable(dbClient, tableName);
            addElements(dbClient, tableName);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (dbClient != null) removeTable(dbClient, tableName);
    }

    // @Test // disabled as it requires additional setup
    public void testPoll() throws Exception {
        if (dbClient != null) {
            DynamoDbConfigurationSource testConfigSource = new DynamoDbConfigurationSource(dbClient);
            PollResult result = testConfigSource.poll(true, null);
            assertEquals(3, result.getComplete().size());
            assertEquals("val1", result.getComplete().get("test1"));
            assertEquals("val2", result.getComplete().get("test2"));
            assertEquals("val3", result.getComplete().get("test3"));
        }
    }

    // @Test // disabled as it requires additional setup
    public void testUpdate() throws Exception {
        if (dbClient != null) {
            DynamoDbConfigurationSource testConfigSource = new DynamoDbConfigurationSource(dbClient);

            PollResult result = testConfigSource.poll(true, null);
            assertEquals(3, result.getComplete().size());
            assertEquals("val1", result.getComplete().get("test1"));
            assertEquals("val2", result.getComplete().get("test2"));
            assertEquals("val3", result.getComplete().get("test3"));

            updateValues(dbClient, tableName);
            result = testConfigSource.poll(true, null);
            assertEquals(3, result.getComplete().size());
            assertEquals("vala", result.getComplete().get("test1"));
            assertEquals("valb", result.getComplete().get("test2"));
            assertEquals("valc", result.getComplete().get("test3"));
        }
    }
}
