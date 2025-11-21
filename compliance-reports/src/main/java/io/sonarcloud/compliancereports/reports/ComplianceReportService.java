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
import java.util.stream.Stream;

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

  public Map<String, CategoryStats> getComplianceReport(String aggregationId, AggregationType aggregationType, ReportKey standard) {
    RuleBuckets ruleKeysByCategory = metadataLoader.getAllMetadata().get(standard);
    Set<String> activeRuleKeys = activeRuleDao.getActiveRuleKeys(aggregationId, aggregationType);
    Map<String, IssueStats> issueStatsByRuleKey = loadIssueStats(aggregationId, aggregationType);
    var issueStatsByRuleKeyAsWildcards = getIssueStatsByWildcard(issueStatsByRuleKey);
    Map<String, CategoryStats> report = new HashMap<>();

    for (RuleBuckets.RuleBucket bucket : ruleKeysByCategory.getBuckets()) {
      Set<String> withoutRedundantConcreteKeys = getDeDupedRuleKeys(bucket);
      AggregateCategoryStats aggregate = aggregateStatsForCategory(issueStatsByRuleKey, activeRuleKeys, withoutRedundantConcreteKeys, issueStatsByRuleKeyAsWildcards);

      int hotspotRating = computeSecurityReviewRating(aggregate.getToReviewHotspots(), aggregate.getReviewedHotspots());
      report.put(bucket.key(), aggregate.toImmutable(hotspotRating));
    }
    return report;
  }

  private static Map<String, Set<IssueStats>> getIssueStatsByWildcard(Map<String, IssueStats> issueStatsByRuleKey) {
    return issueStatsByRuleKey.keySet().stream()
      .collect(Collectors.toMap(
        ComplianceReportService::getRuleKeyWithColonPrefix,
        key -> Set.of(issueStatsByRuleKey.get(key)),
        (set1, set2) -> {
          Set<IssueStats> merged = Set.copyOf(set1);
          merged = Set.copyOf(Stream.concat(merged.stream(), set2.stream()).collect(Collectors.toSet()));
          return merged;
        }
      ));
  }

  private static AggregateCategoryStats aggregateStatsForCategory(Map<String, IssueStats> issueStatsByRuleKey, Set<String> activeRuleKeys,
    Set<String> ruleKeysInCategory, Map<String, Set<IssueStats>> issueStatsByRuleKeyAsWildcards) {
    AggregateCategoryStats aggregate = new AggregateCategoryStats();
    // For every rule in the category
    for (String ruleKey : ruleKeysInCategory) {
      Set<IssueStats> matchingIssueStats = getMatchingIssueStats(issueStatsByRuleKey, ruleKey, issueStatsByRuleKeyAsWildcards);
      for (IssueStats issueStats : matchingIssueStats) {

        aggregateIssueStats(issueStats, aggregate);

        if (activeRuleKeys.contains(issueStats.ruleKey())) {
          aggregate.incrementActiveRules();
        }
      }
    }
    return aggregate;
  }

  private static Set<IssueStats> getMatchingIssueStats(Map<String, IssueStats> issueStatsByRuleKey, String ruleKey, Map<String, Set<IssueStats>> issueStatsByRuleKeyAsWildcards) {
    if (isWildcardRuleKey(ruleKey)) {
      return issueStatsByRuleKeyAsWildcards.get(ruleKey);
    }
    return issueStatsByRuleKey.get(ruleKey) == null ? Set.of() : Set.of(issueStatsByRuleKey.get(ruleKey));
  }

  private static void aggregateIssueStats(IssueStats issueStats, AggregateCategoryStats aggregate) {
    aggregate.addOpenIssues(issueStats.issueCount());
    aggregate.addToReviewHotspots(issueStats.hotspotCount());
    aggregate.addReviewedHotspots(issueStats.hotspotsReviewed());
    aggregate.addToRatingDistribution(issueStats.rating(), issueStats.issueCount());
    aggregate.updateRating(issueStats.rating());
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

  private static String getRuleKeyWithColonPrefix(String key) {
    return key.substring(key.indexOf(":"));
  }

  /**
   * Return a set of rule keys where concrete rule keys are deduped if a wildcard rule key exists in the same category.
   * E.g. if both ":S1" and "java:S1" exist in the same category, only ":S1" will be kept.
   */
  private static Set<String> getDeDupedRuleKeys(RuleBuckets.RuleBucket bucket) {
    Set<String> wildcardRuleKeysInCategory = bucket.ruleKeys().stream()
      .filter(ComplianceReportService::isWildcardRuleKey)
      .collect(Collectors.toSet());
    return bucket.ruleKeys().stream().filter(rk -> isWildcardRuleKey(rk) || !wildcardRuleKeysInCategory.contains(getRuleKeyWithColonPrefix(rk)))
      .collect(Collectors.toSet());
  }

  private static boolean isWildcardRuleKey(String ruleKey) {
    return ruleKey.startsWith(":");
  }
}
