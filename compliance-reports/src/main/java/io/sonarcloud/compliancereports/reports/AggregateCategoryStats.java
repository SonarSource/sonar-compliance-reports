/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.dao.IssueStats;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable accumulator for aggregating category statistics.
 * <p>**** THIS LOGIC IS INTENTIONALLY DUPLICATED TO ProjectAggregation ****</p>*
 * <p>If you're going to consolidate the logic here to a shared location, be sure to update ProjectAggregation as well!</p>
 */
class AggregateCategoryStats {
  private int openIssues = 0;
  private int toReviewHotspots = 0;
  private int reviewedHotspots = 0;
  private int rating = 1;
  private int activeRules = 0;
  private final Map<Integer, Integer> ratingDistribution = new HashMap<>();


  public void add(IssueStats issueStats) {
    addOpenIssues(issueStats.issueCount());
    addToReviewHotspots(issueStats.hotspotCount());
    addReviewedHotspots(issueStats.hotspotsReviewed());
    addToRatingDistribution(issueStats.rating(), issueStats.issueCount());
    updateRating(issueStats.rating());
  }

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

  public CategoryStats toImmutable(String category, List<CategoryStats> children) {
    int hotspotRating = computeSecurityReviewRating(toReviewHotspots, reviewedHotspots);

    return new CategoryStats(
      category,
      openIssues,
      toReviewHotspots,
      reviewedHotspots,
      rating,
      ratingDistribution,
      hotspotRating,
      activeRules,
      children
    );
  }

  private static int computeSecurityReviewRating(int hotspotsToReview, int hotspotsReviewed) {
    long total = (long) hotspotsToReview + (long) hotspotsReviewed;
    if (total == 0) {
      return 1;
    }
    double percent = hotspotsReviewed * 100.0D / total;

    if (percent >= 80.0D) {
      return 1;
    } else if (percent >= 70.0D) {
      return 2;
    } else if (percent >= 50.0D) {
      return 3;
    } else if (percent >= 30.0D) {
      return 4;
    }
    return 5;
  }
}

