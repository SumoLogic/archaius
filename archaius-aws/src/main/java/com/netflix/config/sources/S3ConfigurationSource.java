
package com.netflix.config.sources;

import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * A polled configuration source backed by a file on Amazon S3. When successfully retrieved, the file is decoded using
 * {@link java.util.Properties#load(InputStream)} - just like {@link com.netflix.config.sources.URLConfigurationSource}.
 *
 * Poll requests throw exceptions in line with {@link software.amazon.awssdk.services.s3.S3Client#getObject(GetObjectRequest)} and
 * {@link java.util.Properties#load(InputStream)} for the obvious reasons (file not found, bad credentials, no network connection, malformed file...)
 *
 * @author Michael Tandy
 */
public class S3ConfigurationSource implements PolledConfigurationSource {
  private final S3Client client;
  private final String bucketName;
  private final String key;

  /**
   * Create the instance with the provided {@link software.amazon.awssdk.services.s3.S3Client}, bucket and key. Suitable for injecting a custom client for
   * testing, messing around with endpoints etc.
   * 
   * @param client
   *          to be used to retrieve the object.
   * @param bucketName
   *          The S3 bucket containing the configuration file.
   * @param key
   *          The key of the file within that bucket.
   */
  public S3ConfigurationSource(S3Client client, String bucketName, String key) {
    this.client = client;
    this.bucketName = bucketName;
    this.key = key;
  }

  @Override
  public PollResult poll(boolean initial, Object checkPoint) throws IOException, AwsServiceException {
    GetObjectRequest s3request = GetObjectRequest.builder().bucket(bucketName).key(key).build();
    ResponseInputStream<GetObjectResponse> is = null;
    try {
      is = client.getObject(s3request);
      Map<String, Object> resultMap = inputStreamToMap(is);
      return PollResult.createFull(resultMap);

    } finally {
      if (is != null)
        is.close();
    }
  }

  protected Map<String, Object> inputStreamToMap(InputStream is) throws IOException {
    // Copied from URLConfigurationSource so behaviour is consistent.
    URLConfigurationSource u;
    Map<String, Object> map = new HashMap<String, Object>();
    Properties props = new Properties();
    props.load(is);
    for (Entry<Object, Object> entry : props.entrySet()) {
      map.put((String) entry.getKey(), entry.getValue());
    }
    return Collections.unmodifiableMap(map);
  }

}
