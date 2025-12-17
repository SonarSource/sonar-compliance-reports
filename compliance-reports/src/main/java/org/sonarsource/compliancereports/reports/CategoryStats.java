/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonarsource.compliancereports.reports;

import java.util.List;
import java.util.Map;

public record CategoryStats(
  String categoryName,
  int openIssues,
  int toReviewHotspots,
  int reviewedHotspots,
  int rating,
  int mqrRating,
  Map<Integer, Integer> ratingDistribution,
  Map<Integer, Integer> mqrRatingDistribution,
  int hotspotRating,
  int activeRules,
  List<CategoryStats> children
) {}
