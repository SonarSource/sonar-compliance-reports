/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.reports.metadata.ASVSMetadataType;
import io.sonarcloud.compliancereports.reports.metadata.CweMetadataType;
import io.sonarcloud.compliancereports.reports.metadata.MetadataType;
import io.sonarcloud.compliancereports.reports.metadata.StigMetadataType;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetadataLoaderTest {

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
    void shouldGetCweMetadata() {
      MetadataType cweMetadataType = new CweMetadataType();
      MetadataLoader metaDataLoader = new MetadataLoader(Set.of(cweMetadataType));
      Map<String, RuleBuckets> metadata = metaDataLoader.getAllMetadata();

      assertThat(metadata)
        .hasEntrySatisfying("cwe", cweBuckets -> {
          assertThat(cweBuckets.getBuckets()).hasSize(969);
        })
        .hasEntrySatisfying("cweTop25_2024", cweBuckets -> {
          assertThat(cweBuckets.getBuckets()).hasSize(25);
        });
    }

    @Test
    void shouldGetASVSMetadata() {
      MetadataType asvsMetadataType = new ASVSMetadataType();
      MetadataLoader metaDataLoader = new MetadataLoader(Set.of(asvsMetadataType));
      Map<String, RuleBuckets> metadata = metaDataLoader.getAllMetadata();

      assertThat(metadata)
        .hasEntrySatisfying("asvs4.0.3", asvsBuckets -> {
          assertThat(asvsBuckets.getBuckets()).hasSize(369);
        });
    }

    @Test
    void shouldGetStigMetadata() {
      MetadataType stigMetadataType = new StigMetadataType();
      MetadataLoader metaDataLoader = new MetadataLoader(Set.of(stigMetadataType));
      Map<String, RuleBuckets> metadata = metaDataLoader.getAllMetadata();

      assertThat(metadata)
        .hasEntrySatisfying("stigASD_V5R3", stigBuckets -> {
          assertThat(stigBuckets.getBuckets()).hasSize(286);
        });
    }
  }

  @Nested
  class WhenGettingAllMetadata {
    @Test
    void shouldGetAllMetadata() {
      MetadataLoader metaDataLoader = new MetadataLoader(Set.of(new CweMetadataType(), new ASVSMetadataType(), new StigMetadataType()));
      Map<String, RuleBuckets> allMetadata = metaDataLoader.getAllMetadata();
      assertThat(allMetadata.keySet()).containsExactlyInAnyOrder("cwe", "cweTop25_2024", "asvs4.0.3", "stigASD_V5R3");
    }
  }
}