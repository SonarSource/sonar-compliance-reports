/*
 * Copyright (C) 2022-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

package io.sonarcloud.compliancereports.ingestion;

public record IssueFromAnalysis(String ruleKey, boolean isHotspot, int severity) {
}
