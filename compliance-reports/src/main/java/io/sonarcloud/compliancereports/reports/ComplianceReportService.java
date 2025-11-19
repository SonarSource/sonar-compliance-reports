/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.dao.ActiveRuleDao;
import io.sonarcloud.compliancereports.dao.AggregationType;
import io.sonarcloud.compliancereports.dao.IssueStats;
import io.sonarcloud.compliancereports.dao.IssueStatsByRuleKeyDao;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class ComplianceReportService {

  private final IssueStatsByRuleKeyDao issueStatsByRuleKeyDao;
  private final ActiveRuleDao activeRuleDao;
  private final MetadataLoader metadataLoader;

  public ComplianceReportService(IssueStatsByRuleKeyDao issueStatsByRuleKeyDao, ActiveRuleDao activeRuleDao,
    MetadataLoader metadataLoader) {
    this.issueStatsByRuleKeyDao = issueStatsByRuleKeyDao;
    this.activeRuleDao = activeRuleDao;
    this.metadataLoader = metadataLoader;
  }

  public Map<String, CategoryStats> getComplianceReport(String aggregationId, AggregationType aggregationType, String standard) {
    RuleBuckets ruleKeysByCategory = metadataLoader.getAllMetadata().get(standard);
    Set<String> activeRuleKeys = activeRuleDao.getActiveRuleKeys(aggregationId, aggregationType);
    Map<String, IssueStats> issueStatsByRuleKey = loadIssueStats(aggregationId, aggregationType);
    Map<String, CategoryStats> report = new HashMap<>();

    for (RuleBuckets.RuleBucket bucket : ruleKeysByCategory.getBuckets()) {
      int openIssues = 0;
      int toReviewHotspots = 0;
      int reviewedHotspots = 0;
      int rating = 1;
      int activeRules = 0;
      Map<Integer, Integer> ratingDistribution = new HashMap<>();

      // For every rule in the category
      for (String ruleKey : bucket.ruleKeys()) {
        IssueStats issueStats = issueStatsByRuleKey.get(ruleKey);
        if (issueStats == null) {
          continue;
        }
        openIssues += issueStats.issueCount();
        toReviewHotspots += issueStats.hotspotCount();
        reviewedHotspots += issueStats.hotspotsReviewed();

        ratingDistribution.compute(
          issueStats.rating(),
          (k, v) -> (v == null ? 0 : v) + issueStats.issueCount()
        );

        rating = Math.max(rating, issueStats.rating());
        if (activeRuleKeys.contains(ruleKey)) {
          activeRules++;
        }
      }
      int hotspotRating = computeSecurityReviewRating(toReviewHotspots, reviewedHotspots);

      var categoryStats = new CategoryStats(
        openIssues,
        toReviewHotspots,
        reviewedHotspots,
        rating,
        ratingDistribution,
        hotspotRating,
        activeRules
      );

      report.put(bucket.key(), categoryStats);
    }
    return report;
  }

  private Map<String, IssueStats> loadIssueStats(String aggregationId, AggregationType aggregationType) {
    return issueStatsByRuleKeyDao.getIssueStats(aggregationId, aggregationType).stream()
      .collect(Collectors.toMap(IssueStats::ruleKey, Function.identity()));
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
