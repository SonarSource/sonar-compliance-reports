/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.ingestion;

public record IssueFromAnalysis(String ruleKey, String status, boolean isHotspot, int severity) {
}
