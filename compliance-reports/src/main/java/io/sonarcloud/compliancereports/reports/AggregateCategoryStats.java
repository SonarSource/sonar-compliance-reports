/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import java.util.HashMap;
import java.util.Map;

/**
 * Mutable accumulator for aggregating category statistics.
 */
class AggregateCategoryStats {
  private int openIssues = 0;
  private int toReviewHotspots = 0;
  private int reviewedHotspots = 0;
  private int rating = 1;
  private int activeRules = 0;
  private final Map<Integer, Integer> ratingDistribution = new HashMap<>();

  public void addOpenIssues(int count) {
    this.openIssues += count;
  }

  public void addToReviewHotspots(int count) {
    this.toReviewHotspots += count;
  }

  public void addReviewedHotspots(int count) {
    this.reviewedHotspots += count;
  }

  public void updateRating(int newRating) {
    this.rating = Math.max(this.rating, newRating);
  }

  public void addToRatingDistribution(int rating, int count) {
    this.ratingDistribution.compute(rating, (k, v) -> (v == null ? 0 : v) + count);
  }

  public void incrementActiveRules() {
    this.activeRules++;
  }

  public int getToReviewHotspots() {
    return toReviewHotspots;
  }

  public int getReviewedHotspots() {
    return reviewedHotspots;
  }

  public CategoryStats toImmutable(int hotspotRating) {
    return new CategoryStats(
      openIssues,
      toReviewHotspots,
      reviewedHotspots,
      rating,
      ratingDistribution,
      hotspotRating,
      activeRules
    );
  }
}

