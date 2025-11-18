/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

public record CategoryStats(int openIssues, int toReviewHotspots, int reviewedHotspots, int rating, int hotspotRating, int activeRules) {
}
