/*
 * Copyright (C) 2022-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.reports.metadata.MetadataType;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

@Singleton
public class MetadataLoader {

  public static final String METADATA_RESOURCE_DIR = "metadata/";
  private final Map<String, MetadataType> classNameToTypeBeans;
  private final Map<String, String> typeToStringContent;

  public MetadataLoader(Map<String, MetadataType> classNameToTypeBeans) {
    this.classNameToTypeBeans = classNameToTypeBeans;
    this.typeToStringContent = classNameToTypeBeans.keySet().stream().collect(Collectors.toMap(key -> key, this::getMetadataAsString, (a, b) -> b, HashMap::new));
  }

  public String getMetadataAsString(String metadataType) {
    MetadataType type = classNameToTypeBeans.get(metadataType);
    if (type == null) {
      throw new IllegalStateException("Unable to get bean for: " + metadataType);
    }
    try {
      return IOUtils.toString(requireNonNull(getClass().getClassLoader().getResourceAsStream(METADATA_RESOURCE_DIR + type.getResourceFileName())), UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load metadata: " + metadataType);
    }
  }

  public Map<String, String> getAllMetadata() {
    return typeToStringContent;
  }
}
