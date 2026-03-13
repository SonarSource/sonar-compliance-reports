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

import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonarsource.compliancereports.dao.ActiveRuleDao;
import org.sonarsource.compliancereports.dao.AggregationType;
import org.sonarsource.compliancereports.dao.IssueStats;
import org.sonarsource.compliancereports.dao.IssueStatsByRuleKeyDao;

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

  public CategoryTree getCategoryTree(ReportKey reportKey) {
    return metadataRules.getCategoryTree(reportKey);
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

  public Map<String, List<CategoryStats>> getComplianceReportForAggregations(Collection<String> aggregationIds, AggregationType aggregationType,
    ReportKey standard) {
    return getComplianceReportForAggregations(aggregationIds, aggregationType, standard, null, null);
  }

  public Map<String, List<CategoryStats>> getComplianceReportForAggregations(Collection<String> aggregationIds, AggregationType aggregationType,
    ReportKey standard, @Nullable ReportKey cweStandard, @Nullable Integer levelIndex) {
    Map<String, ComplianceCategoryRules> rulesByCategory = metadataRules.getRulesByCategory(standard);
    Map<String, List<IssueStats>> issueStatsListByAggregationId = issueStatsByRuleKeyDao.getIssueStatsByAggregationIds(aggregationIds, aggregationType);

    return issueStatsListByAggregationId.entrySet()
      .stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        e -> getCategoryStats(rulesByCategory, e.getValue(), Set.of(), cweStandard, levelIndex)
      ));
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
