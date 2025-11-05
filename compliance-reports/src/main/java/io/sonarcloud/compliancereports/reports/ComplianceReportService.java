/*
 * Copyright (C) 2022-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.dao.ActiveRuleDao;
import io.sonarcloud.compliancereports.dao.IssueStatsByRuleKeyDao;
import io.sonarcloud.compliancereports.dao.IssueStats;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class ComplianceReportService {

  private final IssueStatsByRuleKeyDao issueStatsByRuleKeyDao;
  private final ActiveRuleDao activeRuleDao;
  private final Map<String, Map<String, Set<String>>> ruleMetadataByStandard;

  public ComplianceReportService(IssueStatsByRuleKeyDao issueStatsByRuleKeyDao, ActiveRuleDao activeRuleDao,
    Map<String, Map<String, Set<String>>> ruleMetadataByStandard) {
    this.issueStatsByRuleKeyDao = issueStatsByRuleKeyDao;
    this.activeRuleDao = activeRuleDao;
    this.ruleMetadataByStandard = ruleMetadataByStandard;
  }

  public Map<String, CategoryStats> getComplianceReportForProject(UUID projectId, String standard) {
    Map<String, Set<String>> ruleKeysByCategory = ruleMetadataByStandard.get(standard);
    Set<String> activeRuleKeys = activeRuleDao.getActiveRuleKeysForProject(projectId);
    Map<String, IssueStats> issueStatsByRuleKey = loadIssueStatsForProject(projectId);
    Map<String, CategoryStats> report = new HashMap<>();

    // TODO: Do this recursively for subcategories
    // For every category in the standard
    for (Map.Entry<String, Set<String>> entry : ruleKeysByCategory.entrySet()) {
      String category = entry.getKey();
      Set<String> ruleSet = entry.getValue();
      int totalIssues = 0;
      int totalHotspots = 0;
      int rating = 1;
      int hotspotRating = 1;
      int activeRules = 0;

      // For every rule in the category
      for (String ruleKey : ruleSet) {
        IssueStats issueStats = issueStatsByRuleKey.get(ruleKey);
        if (issueStats == null) {
          continue;
        }
        totalIssues += issueStats.issueCount();
        totalHotspots += issueStats.hotspotCount();
        rating = Math.max(rating, issueStats.rating());
        hotspotRating = Math.max(hotspotRating, issueStats.hotspotRating());
        if (activeRuleKeys.contains(ruleKey)) {
          activeRules++;
        }
      }

      var categoryStats = new CategoryStats(totalIssues, totalHotspots, rating, hotspotRating, activeRules);
      report.put(category, categoryStats);
    }
    return report;
  }

  private Map<String, IssueStats> loadIssueStatsForProject(UUID projectId) {
    return issueStatsByRuleKeyDao.getIssueStatsForProject(projectId).stream()
      .collect(Collectors.toMap(IssueStats::ruleKey, Function.identity()));
  }

}
