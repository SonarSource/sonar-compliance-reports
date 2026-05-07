/*
 * Compliance Reports
 * Copyright (C) 2022-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.compliancereports.reports;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonarsource.compliancereports.reports.metadata.ComplianceStandardMetadata;
import org.sonarsource.compliancereports.reports.metadata.MetadataType;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

@Singleton
public class MetadataLoader {

  private static final String METADATA_RESOURCE_DIR = "metadata/";
  private final Map<ReportKey, CategoryTree> allMetadata;
  private final Map<String, ComplianceStandardMetadata.Report> standardMetadataByKey;
  private final ObjectMapper objectMapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final Yaml yaml;

  public MetadataLoader(Set<MetadataType> metadataTypes) {
    final var options = new LoaderOptions();
    options.setMaxAliasesForCollections(175);
    yaml = new Yaml(options);

    var parsedMetadata = metadataTypes.stream()
      .map(metadataType -> parseMetadata(metadataType).report())
      .toList();

    // produce a CategoryTree for each version in each standard
    allMetadata = parsedMetadata.stream()
      .flatMap(parsed -> parsed.versions().stream().map(version -> new CategoryTree(parsed.key(), version, parsed.levels())))
      .collect(Collectors.toMap(CategoryTree::getKey, Function.identity()));

    // produce a sanitized ComplianceStandardMetadata.Report object for each standard
    standardMetadataByKey = parsedMetadata.stream()
      .map(ComplianceStandardMetadata.Report::withoutRules)
      .collect(Collectors.toMap(ComplianceStandardMetadata.Report::key, Function.identity()));
  }

  private ComplianceStandardMetadata parseMetadata(MetadataType type) {
    try {
      var resourceFileStream = getClass().getClassLoader().getResourceAsStream(METADATA_RESOURCE_DIR + type.getResourceFileName());
      // parse with SnakeYAML first to resolve YAML anchors/references
      // (https://stackoverflow.com/questions/40074700/jackson-yaml-support-for-anchors-and-references)
      var yamlObj = yaml.loadAs(resourceFileStream, Object.class);
      // convert to JSON, then reparse with Jackson so we can use records
      return objectMapper.readValue(objectMapper.writeValueAsString(yamlObj), ComplianceStandardMetadata.class);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load metadata: " + type.getResourceFileName(), e);
    }
  }

  public Map<ReportKey, CategoryTree> getAllMetadata() {
    return allMetadata;
  }

  /**
   * @return the parsed compliance standard metadata with all rule mappings removed
   */
  public Map<String, ComplianceStandardMetadata.Report> getSanitizedMetadata() {
    return standardMetadataByKey;
  }

  public Set<String> getAllReportsAsStrings() {
    return allMetadata.keySet().stream().map(ReportKey::toString).collect(Collectors.toSet());
  }
}
