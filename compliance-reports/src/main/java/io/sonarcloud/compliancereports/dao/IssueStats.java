/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.dao;

public record IssueStats(String ruleKey, int issueCount, int rating, int mqrRating, int hotspotCount, int hotspotsReviewed) {
}
