/*
 * Copyright (C) 2022-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.reports.metadata.ASVSMetadataType;
import io.sonarcloud.compliancereports.reports.metadata.CweMetadataType;
import io.sonarcloud.compliancereports.reports.metadata.MetadataType;
import io.sonarcloud.compliancereports.reports.metadata.StigMetadataType;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataLoaderTest {

  @Nested
  class WhenGettingMetadataAsString {
    @Test
    void shouldThrowUnableToGetFileContents() {
      MetadataType faultyMetadataType = () -> "NonExistentFile.yml";
      assertThatThrownBy(() -> new MetadataLoader(Map.of("FaultyMetadataType", faultyMetadataType)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to load metadata: FaultyMetadataType");
    }

    @Test
    void shouldThrowUnableToGetBeanException() {
      MetadataLoader loader = new MetadataLoader(Map.of());
      Exception exception = assertThrows(IllegalStateException.class, () -> loader.getMetadataAsString("NonExistentMetadataType"));
      assertEquals("Unable to get bean for: NonExistentMetadataType", exception.getMessage());
    }

    @Test
    void shouldGetCweMetadata() {
      MetadataType cweMetadataType = new CweMetadataType();
      MetadataLoader metaDataLoader = new MetadataLoader(Map.of("CweMetadataType", cweMetadataType));
      String metadataContent = metaDataLoader.getMetadataAsString("CweMetadataType");
      assertNotNull(metadataContent);
      assertFalse(metadataContent.isEmpty());
      String expectedHead = """
        report:
          name: CWE - Common Weakness Enumeration
          description: CWE - Common Weakness Enumeration""";
      assertTrue(metadataContent.startsWith(expectedHead));
    }

    @Test
    void shouldGetASVSMetadata() {
      MetadataType asvsMetadataType = new ASVSMetadataType();
      MetadataLoader metaDataLoader = new MetadataLoader(Map.of("ASVSMetadataType", asvsMetadataType));
      String metadataContent = metaDataLoader.getMetadataAsString("ASVSMetadataType");
      assertNotNull(metadataContent);
      assertFalse(metadataContent.isEmpty());
      String expectedHead = """
        report:
          name: Application Security Verification Standard Project""";
      assertTrue(metadataContent.startsWith(expectedHead));
    }

    @Test
    void shouldGetStigMetadata() {
      MetadataType stigMetadataType = new StigMetadataType();
      MetadataLoader metaDataLoader = new MetadataLoader(Map.of("StigMetadataType", stigMetadataType));
      String metadataContent = metaDataLoader.getMetadataAsString("StigMetadataType");
      assertNotNull(metadataContent);
      assertFalse(metadataContent.isEmpty());
      String expectedHead = """
        report:
          name: Application Security and Development Security Technical Implementation Guide""";
      assertTrue(metadataContent.startsWith(expectedHead));
    }
  }

  @Nested
  class WhenGettingAllMetadata {
    @Test
    void shouldGetAllMetadata() {
      MetadataLoader metaDataLoader = new MetadataLoader(Map.of(
        "CweMetadataType", new CweMetadataType(),
        "ASVSMetadataType", new ASVSMetadataType(),
        "StigMetadataType", new StigMetadataType()
      ));
      Map<String, String> allMetadata = metaDataLoader.getAllMetadata();
      assertThat(allMetadata.keySet()).containsExactlyInAnyOrder(
        "CweMetadataType",
        "ASVSMetadataType",
        "StigMetadataType"
      );
      assertThat(allMetadata.values())
        .allSatisfy(value -> assertThat(value).startsWith("report:"));
    }
  }
}