/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.wrangler;

/**
 * This class {@link PropertyIds} is a collection of static strings.
 */
public final class PropertyIds {
  // Id of the workspace
  public static final String ID = "id";

  // Name of the workspace
  public static final String NAME = "name";

  // Delimiter used to split record.
  public static final String DELIMITER = "delimiter";

  // Charset of the content.
  public static final String CHARSET = "charset";

  // Type of connection.
  public static final String CONNECTION_TYPE = "connection";

  // Name of the file.
  public static final String FILE_NAME  = "file";

  // URI of the source.
  public static final String URI = "uri";

  // Path from the URI.
  public static final String FILE_PATH = "path";

  // Type of sampler.
  public static final String SAMPLER_TYPE = "sampler";

  // Content Type of content being stored.
  public static final String CONTENT_TYPE = "Content-Type";

  // Topic.
  public static final String TOPIC = "topic";

  // Plugin type
  public static final String PLUGIN_TYPE = "plugin-type";

  // File format to use when reading data as a pipeline source
  public static final String FORMAT = "format";
  public static final String CONNECTION_ID = "connectionid";
  public static final String BROKER = "brokers";
  public static final String KEY_DESERIALIZER = "key.deserializer";
  public static final String VALUE_DESERIALIZER = "value.deserializer";

  public static final String PLUGIN_SPECIFICATION = "plugin.specification";
  
  // Key tab pricipal to be used
  public static final String PRINCIPAL = "principal";
  
  // Location of tab file
  public static final String KEYTAB_LOCATION = "keytabLocation";
  
  // Properties to be passed in kafka source plugin
  public static final String KAFKA_PROPERTIES = "kafkaProperties";
  
  // extra Kafka properties passed by user
  public static final String KAFAK_PRODUCER_PROPERTIES = "kafkaProducerProperties";
  
  // Namespace reference keytab file location
  public static final String NAMESPACE_KETAB_PATH = "pipeline.keytab.path";
  
  // Namespace reference principal
  public static final String NAMESPACE_PRINCIPAL_NAME = "pipeline.principal.name";
  
  // Namespace reference to enable/disable propagation of logged in user identity to downstream systems
  public static final String USER_IMPERSONATION_ENABLED = "system.user.impersonation.enabled";
  
  // Request header identifying actual user who sent request
  public static final String USER_ID = "CDAP-UserId";
}
