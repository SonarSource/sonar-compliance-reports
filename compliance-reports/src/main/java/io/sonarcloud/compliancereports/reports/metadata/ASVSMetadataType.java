/*
 * Copyright (C) 2022-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports.metadata;

public class ASVSMetadataType implements MetadataType {
  @Override
  public String getResourceFileName() {
    return "ASVS/asvs_4.yaml";
  }
}
