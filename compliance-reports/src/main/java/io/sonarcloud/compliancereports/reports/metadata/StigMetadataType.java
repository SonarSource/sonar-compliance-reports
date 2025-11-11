/*
 * Copyright (C) 2022-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports.metadata;

public class StigMetadataType implements MetadataType {
  @Override
  public String getResourceFileName() {
    return "STIG/stig_asd-V5R3.yaml";
  }
}
