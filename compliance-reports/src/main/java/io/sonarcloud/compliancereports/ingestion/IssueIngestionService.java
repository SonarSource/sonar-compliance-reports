/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.ingestion;

import io.sonarcloud.compliancereports.dao.IssueStats;
import io.sonarcloud.compliancereports.dao.IssueStatsByRuleKeyDao;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class IssueIngestionService {

  private enum IssueStatus {
    TO_REVIEW,
    OPEN
  }

  private final IssueStatsByRuleKeyDao issueStatsByRuleKeyDao;

  public IssueIngestionService(IssueStatsByRuleKeyDao issueStatsByRuleKeyDao) {
    this.issueStatsByRuleKeyDao = issueStatsByRuleKeyDao;
  }

  public void ingest(UUID projectId, List<IssueFromAnalysis> issueData) {
    List<IssueStats> issueStats = calculateIssueStats(issueData);
    issueStatsByRuleKeyDao.deleteAllIssueStatsForProject(projectId);
    issueStatsByRuleKeyDao.insertIssueStatsForProject(projectId, issueStats);
  }

  private List<IssueStats> calculateIssueStats(List<IssueFromAnalysis> issueData) {
    List<IssueStats> results = new ArrayList<>();
    Map<String, List<IssueFromAnalysis>> issuesByRuleKey = issueData.stream()
      .collect(Collectors.groupingBy(IssueFromAnalysis::ruleKey));
    for (Map.Entry<String, List<IssueFromAnalysis>> entry : issuesByRuleKey.entrySet()) {
      results.add(calculateIssueStatsForIssuesWithRule(entry.getKey(), entry.getValue()));
    }
    return results;
  }

  private IssueStats calculateIssueStatsForIssuesWithRule(String ruleKey, List<IssueFromAnalysis> issues) {
    int issueCount = 0;
    int issueRating = 1;
    int hotspotsToReview = 0;
    int hotspotsReviewed = 0;

    for (IssueFromAnalysis issue : issues) {
      if (issue.isHotspot()) {
        if (IssueStatus.TO_REVIEW.toString().equals(issue.status())) {
          hotspotsToReview++;
        } else {
          hotspotsReviewed++;
        }
      } else if (IssueStatus.OPEN.toString().equals(issue.status())) {
        issueCount++;
        issueRating = Math.max(issueRating, issue.severity());
      }
    }

    return new IssueStats(ruleKey, issueCount, issueRating, hotspotsToReview, hotspotsReviewed);
  }

}
