/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.reports.metadata.MetadataType;
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
    void shouldLoadEmptyMetadataIfNoneExists() {
      var underTest = new MetadataLoader(Set.of());
      assertThat(underTest.getAllMetadata()).isEmpty();
    }

    @Test
    void shouldLoadMetadata() {
      MetadataType metadataType = () -> "TestMetadata.yml";
      MetadataLoader metaDataLoader = new MetadataLoader(Set.of(metadataType));
      Map<String, RuleBuckets> metadata = metaDataLoader.getAllMetadata();

      assertThat(metadata)
        .hasEntrySatisfying("testV1", buckets -> {
          assertThat(buckets.getBuckets()).hasSize(3);
        });
    }
  }
}