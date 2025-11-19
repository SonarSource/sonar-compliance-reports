/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports.metadata;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

@ServerSide
@ComputeEngineSide
public interface MetadataType {
  String getResourceFileName();
}
