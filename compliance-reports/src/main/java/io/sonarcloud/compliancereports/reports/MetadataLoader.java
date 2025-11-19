/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sonarcloud.compliancereports.reports.metadata.MetadataType;
import io.sonarcloud.compliancereports.reports.metadata.ReportMetadataSchema;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

@Singleton
public class MetadataLoader {

  private static final String METADATA_RESOURCE_DIR = "metadata/";
  private final Map<String, RuleBuckets> allMetadata;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Yaml yaml;

  public MetadataLoader(Set<MetadataType> metadataTypes) {
    final var options = new LoaderOptions();
    options.setMaxAliasesForCollections(175);
    yaml = new Yaml(options);

    allMetadata = metadataTypes.stream()
      .map(metadataType -> parseMetadata(metadataType).report())
      // produce buckets for each version in the report
      .flatMap(parsed -> parsed.versions().stream().map(version -> new RuleBuckets(parsed.key(), version)))
      .collect(Collectors.toMap(RuleBuckets::getKey, Function.identity()));
  }

  private ReportMetadataSchema parseMetadata(MetadataType type) {
    try {
      var resourceFileStream = getClass().getClassLoader().getResourceAsStream(METADATA_RESOURCE_DIR + type.getResourceFileName());
      // parse with SnakeYAML first to resolve YAML anchors/references
      // (https://stackoverflow.com/questions/40074700/jackson-yaml-support-for-anchors-and-references)
      var yamlObj = yaml.loadAs(resourceFileStream, Object.class);
      // convert to JSON, then reparse with Jackson so we can use records
      return objectMapper.readValue(objectMapper.writeValueAsString(yamlObj), ReportMetadataSchema.class);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load metadata: " + type.getResourceFileName(), e);
    }
  }

  public Map<String, RuleBuckets> getAllMetadata() {
    return allMetadata;
  }
}
