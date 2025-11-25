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
import io.sonarcloud.compliancereports.reports.MetadataRules.ComplianceCategoryRules;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class ComplianceReportService {

  private final IssueStatsByRuleKeyDao issueStatsByRuleKeyDao;
  private final ActiveRuleDao activeRuleDao;
  private final MetadataRules metadataRules;

  public ComplianceReportService(IssueStatsByRuleKeyDao issueStatsByRuleKeyDao, ActiveRuleDao activeRuleDao, MetadataRules metadataRules) {
    this.issueStatsByRuleKeyDao = issueStatsByRuleKeyDao;
    this.activeRuleDao = activeRuleDao;
    this.metadataRules = metadataRules;
  }

  public Map<String, CategoryStats> getComplianceReport(String aggregationId, AggregationType aggregationType, ReportKey standard) {
    Map<String, ComplianceCategoryRules> rulesByCategory = metadataRules.getRules(standard);
    Set<String> activeRuleKeys = activeRuleDao.getActiveRuleKeys(aggregationId, aggregationType);
    Map<String, IssueStats> issueStatsByRuleKey = loadIssueStats(aggregationId, aggregationType);

    Map<String, CategoryStats> report = new HashMap<>();

    for (Map.Entry<String, ComplianceCategoryRules> e : rulesByCategory.entrySet()) {
      ComplianceCategoryRules categoryRules = e.getValue();
      List<IssueStats> matchingIssueStats = new LinkedList<>();
      for (Map.Entry<String, IssueStats> issueStatsEntry : issueStatsByRuleKey.entrySet()) {
        if (categoryRules.contains(issueStatsEntry.getKey())) {
          matchingIssueStats.add(issueStatsEntry.getValue());
        }
      }

      report.put(e.getKey(), aggregateCategoryStats(matchingIssueStats, activeRuleKeys));
    }

    return report;
  }

  private CategoryStats aggregateCategoryStats(List<IssueStats> matchingIssueStats, Set<String> activeRuleKeys) {
    AggregateCategoryStats aggregate = new AggregateCategoryStats();
    for (IssueStats issueStats : matchingIssueStats) {
      aggregateIssueStats(issueStats, aggregate);
      if (activeRuleKeys.contains(issueStats.ruleKey())) {
        aggregate.incrementActiveRules();
      }
    }

    return aggregate.toImmutable();
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
}
