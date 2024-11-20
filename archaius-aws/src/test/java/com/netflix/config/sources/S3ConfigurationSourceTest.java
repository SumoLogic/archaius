
package com.netflix.config.sources;

import com.netflix.config.PollResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class S3ConfigurationSourceTest {

    final static boolean INITIAL = false;
    final static Object CHECK_POINT = null;

    HttpServer fakeS3;
    S3Client client;

    public S3ConfigurationSourceTest() {
    }

    @Before
    public void setup() throws Exception {
        fakeS3 = createHttpServer();
        client = S3Client.builder()
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .forcePathStyle(true)
                .region(Region.US_EAST_1)
                .endpointOverride(new URI("http://localhost:8069"))
                .build();
    }

    @After
    public void teardown() {
        fakeS3.stop(5);
    }

    @Test
    public void testPoll_shouldLoadSomeData() throws Exception {
        S3ConfigurationSource instance = new S3ConfigurationSource(client, "bucketname", "standard-key.txt");
        PollResult result = instance.poll(INITIAL, CHECK_POINT);

        assertNotNull(result);
        assertEquals("true",result.getComplete().get("loaded"));
        assertEquals(1,result.getComplete().size());
    }

    @Test(expected = AwsServiceException.class)
    public void testPoll_fileNotFound() throws Exception {
        S3ConfigurationSource instance = new S3ConfigurationSource(client, "bucketname", "404.txt");
        PollResult result = instance.poll(INITIAL, CHECK_POINT);

        assertNotNull(result);
        assertEquals("true",result.getComplete().get("loaded"));
        assertEquals(1,result.getComplete().size());
    }


    public HttpServer createHttpServer() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8069), 0);

        // create and register our handler
        httpServer.createContext("/bucketname/standard-key.txt",new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                byte[] response = "loaded=true".getBytes("UTF-8");
                    // RFC 2616 says HTTP headers are case-insensitive - but the
                // Amazon S3 client will crash if ETag has a different
                // capitalisation. And this HttpServer normalises the names
                // of headers using "ETag"->"Etag" if you use put, add or
                // set. But not if you use 'putAll' so that's what I use.
                Map<String, List<String>> responseHeaders = new HashMap();
                responseHeaders.put("ETag", Collections.singletonList("\"TEST-ETAG\""));
                responseHeaders.put("Content-Type", Collections.singletonList("text/plain"));
                exchange.getResponseHeaders().putAll(responseHeaders);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            }
        });

        httpServer.createContext("/bucketname/404.txt",new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                byte[] response = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<Error>\n" +
                        "  <Code>NoSuchKey</Code>\n" +
                        "  <Message>The resource you requested does not exist</Message>\n" +
                        "  <Resource>/bucketname/404.txt</Resource> \n" +
                        "</Error>").getBytes("UTF-8");
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND,response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            }
        });

        httpServer.start();
        return httpServer;
    }
}
