/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import java.util.Map;

public record CategoryStats(
  int openIssues,
  int toReviewHotspots,
  int reviewedHotspots,
  int rating,
  Map<Integer, Integer> ratingDistribution,
  int hotspotRating,
  int activeRules
) {}
