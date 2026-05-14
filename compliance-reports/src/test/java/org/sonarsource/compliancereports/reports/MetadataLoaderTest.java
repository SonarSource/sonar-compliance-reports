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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonarsource.compliancereports.reports.metadata.ComplianceStandardMetadata;
import org.sonarsource.compliancereports.reports.metadata.MetadataType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetadataLoaderTest {

  @Test
  void shouldLoadEmptyMetadataIfNoneExists() {
    var underTest = new MetadataLoader(Set.of());
    assertThat(underTest.getAllMetadata()).isEmpty();
  }

  @Nested
  class WhenGettingMetadataAsString {
    @Test
    void shouldThrowUnableToGetFileContents() {
      MetadataType faultyMetadataType = () -> "NonExistentFile.yml";
      Set<MetadataType> metadataTypes = Set.of(faultyMetadataType);
      assertThatThrownBy(() -> new MetadataLoader(metadataTypes))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to load metadata: NonExistentFile.yml");
    }

    @Test
    void shouldLoadMetadata() {
      MetadataType metadataType = () -> "TestMetadata.yml";
      MetadataLoader metaDataLoader = new MetadataLoader(Set.of(metadataType));
      Map<ReportKey, CategoryTree> metadata = metaDataLoader.getAllMetadata();

      assertThat(metadata)
        .hasEntrySatisfying(new ReportKey("test", "V1"), buckets -> {
          assertThat(buckets.getChildren()).hasSize(3);
        });
    }

    @Test
    void shouldReturnStandardNames() {
      MetadataType metadataType = () -> "TestMetadata.yml";
      MetadataLoader metaDataLoader = new MetadataLoader(Set.of(metadataType));
      Set<String> standardNames = metaDataLoader.getAllReportsAsStrings();

      assertThat(standardNames).containsOnly("test:V1", "test:V2");
    }
  }

  @Nested
  class WhenGettingSanitizedMetadata {
    @Test
    void shouldLoadEmptyMetadataIfNoneExists() {
      var underTest = new MetadataLoader(Set.of());
      assertThat(underTest.getSanitizedMetadata()).isEmpty();
    }

    @Test
    void shouldLoadMetadata() {
      MetadataType metadataType = () -> "MetadataWithInclusiveLevels.yml";
      MetadataLoader metaDataLoader = new MetadataLoader(Set.of(metadataType));
      Map<String, ComplianceStandardMetadata.Report> metadata = metaDataLoader.getSanitizedMetadata();

      assertThat(metadata)
        .hasEntrySatisfying("levels-test", report -> {
          assertThat(report.versions()).hasSize(1);
          List<ComplianceStandardMetadata.Report.Category> categories = report.versions().get(0).categories();
          assertThat(categories).hasSize(1);
          assertThat(flatten(categories, new ArrayList<>()).stream()
            .flatMap(c -> c.rules() == null ? Stream.of() : c.rules().stream())
            .toList()).isEmpty();
        });
    }

    private static List<ComplianceStandardMetadata.Report.Category> flatten(List<ComplianceStandardMetadata.Report.Category> nodes,
      List<ComplianceStandardMetadata.Report.Category> accumulator) {
      for (var node : nodes) {
        accumulator.add(node);
        if (node.subcategories() != null) {
          flatten(node.subcategories(), accumulator);
        }
      }
      return accumulator;
    }
  }
}
