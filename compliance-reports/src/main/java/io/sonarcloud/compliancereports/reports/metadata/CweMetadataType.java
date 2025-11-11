/*
 * Copyright (C) 2022-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports.metadata;

import jakarta.inject.Singleton;

@Singleton
public class CweMetadataType implements MetadataType {
  @Override
  public String getResourceFileName() {
    return "CWE/cwe.yaml";
  }
}
