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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

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

  public List<CategoryStats> getComplianceReport(String aggregationId, AggregationType aggregationType, ReportKey standard) {
    return getComplianceReport(aggregationId, aggregationType, standard, null, null);
  }

  public List<CategoryStats> getComplianceReport(String aggregationId, AggregationType aggregationType, ReportKey standard,
    @Nullable ReportKey cweStandard, @Nullable Integer levelIndex) {
    Map<String, ComplianceCategoryRules> rulesByCategory = metadataRules.getRulesByCategory(standard);
    Set<String> activeRuleKeys = activeRuleDao.getActiveRuleKeys(aggregationId, aggregationType);
    List<IssueStats> issueStatsList = issueStatsByRuleKeyDao.getIssueStats(aggregationId, aggregationType);

    return getCategoryStats(rulesByCategory, issueStatsList, activeRuleKeys, cweStandard, levelIndex);
  }

  private List<CategoryStats> getCategoryStats(Map<String, ComplianceCategoryRules> rulesByCategory,
    List<IssueStats> issueStatsList, Set<String> activeRuleKeys, @Nullable ReportKey cweStandard, @Nullable Integer levelIndex) {
    List<CategoryStats> results = new ArrayList<>();
    for (Map.Entry<String, ComplianceCategoryRules> e : rulesByCategory.entrySet()) {
      String categoryKey = e.getKey();
      ComplianceCategoryRules categoryRules = e.getValue();
      List<IssueStats> matchingIssueStats = issueStatsList.stream().filter(s -> categoryRules.containsRuleAtLevel(s.ruleKey(), levelIndex)).toList();
      if (cweStandard != null) {
        List<CategoryStats> cweCategoryStats = getCweCategoryStats(matchingIssueStats, cweStandard, activeRuleKeys);
        results.add(aggregateCategoryStats(categoryKey, matchingIssueStats, activeRuleKeys, cweCategoryStats));
      } else {
        var children = getCategoryStats(categoryRules.getChildren(), issueStatsList, activeRuleKeys, null, levelIndex).stream()
          // TODO: Sort by ordinal once this is fully supported in the metadata
          .sorted(Comparator.comparing(CategoryStats::categoryName))
          .toList();
        results.add(aggregateCategoryStats(categoryKey, matchingIssueStats, activeRuleKeys, children));
      }
    }
    return results;
  }

  private List<CategoryStats> getCweCategoryStats(List<IssueStats> matchingIssueStats, @Nullable ReportKey cweStandard,
    Set<String> activeRules) {
    if (cweStandard == null) {
      return List.of();
    }
    Map<String, ComplianceCategoryRules> cweRulesByCategory = metadataRules.getRulesByCategory(cweStandard);
    List<CategoryStats> report = new LinkedList<>();

    for (Map.Entry<String, ComplianceCategoryRules> e : cweRulesByCategory.entrySet()) {
      ComplianceCategoryRules cweCategoryRules = e.getValue();
      List<IssueStats> cweIssueStats = matchingIssueStats.stream().filter(s -> cweCategoryRules.containsRuleAtLevel(s.ruleKey(), null)).toList();
      if (!cweIssueStats.isEmpty()) {
        report.add(aggregateCategoryStats(e.getKey(), cweIssueStats, activeRules, List.of()));
      }
    }

    return report;
  }

  private static CategoryStats aggregateCategoryStats(String category, List<IssueStats> matchingIssueStats, Set<String> activeRuleKeys,
    List<CategoryStats> children) {
    AggregateCategoryStats aggregate = new AggregateCategoryStats();
    for (IssueStats issueStats : matchingIssueStats) {
      aggregate.add(issueStats);
      if (activeRuleKeys.contains(issueStats.ruleKey())) {
        aggregate.incrementActiveRules();
      }
    }

    return aggregate.toImmutable(category, children);
  }
}
