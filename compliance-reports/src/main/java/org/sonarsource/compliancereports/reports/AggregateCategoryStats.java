/*
 * Compliance Reports
 * Copyright (C) 2025-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.compliancereports.reports;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonarsource.compliancereports.dao.IssueStats;

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
  private int mqrRating = 1;
  private int activeRules = 0;
  private final Map<Integer, Integer> ratingDistribution = new HashMap<>();
  private final Map<Integer, Integer> mqrRatingDistribution = new HashMap<>();

  public void add(IssueStats issueStats) {
    addOpenIssues(issueStats.issueCount());
    addToReviewHotspots(issueStats.hotspotCount());
    addReviewedHotspots(issueStats.hotspotsReviewed());
    addToRatingDistribution(issueStats.rating(), issueStats.issueCount());
    addToMqrRatingDistribution(issueStats.mqrRating(), issueStats.issueCount());
    updateRating(issueStats.rating());
    updateMqrRating(issueStats.mqrRating());
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

  public void updateMqrRating(int newMqrRating) {
    this.mqrRating = Math.max(this.mqrRating, newMqrRating);
  }

  public void addToRatingDistribution(int rating, int count) {
    this.ratingDistribution.compute(rating, (k, v) -> (v == null ? 0 : v) + count);
  }

  public void addToMqrRatingDistribution(int mqrRating, int count) {
    this.mqrRatingDistribution.compute(mqrRating, (k, v) -> (v == null ? 0 : v) + count);
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
      mqrRating,
      ratingDistribution,
      mqrRatingDistribution,
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
