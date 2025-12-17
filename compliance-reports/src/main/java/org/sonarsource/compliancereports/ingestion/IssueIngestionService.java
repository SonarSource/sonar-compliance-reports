/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonarsource.compliancereports.ingestion;

import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonarsource.compliancereports.dao.AggregationType;
import org.sonarsource.compliancereports.dao.IssueStats;
import org.sonarsource.compliancereports.dao.IssueStatsByRuleKeyDao;

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

  public void ingest(String aggregationId, AggregationType aggregationType, List<IssueFromAnalysis> issueData) {
    List<IssueStats> issueStats = calculateIssueStats(issueData);
    issueStatsByRuleKeyDao.deleteAndInsertIssueStats(aggregationId, aggregationType, issueStats);
  }

  private static List<IssueStats> calculateIssueStats(List<IssueFromAnalysis> issueData) {
    List<IssueStats> results = new ArrayList<>();
    Map<String, List<IssueFromAnalysis>> issuesByRuleKey = issueData.stream()
      .collect(Collectors.groupingBy(IssueFromAnalysis::ruleKey));
    for (Map.Entry<String, List<IssueFromAnalysis>> entry : issuesByRuleKey.entrySet()) {
      results.add(calculateIssueStatsForIssuesWithRule(entry.getKey(), entry.getValue()));
    }
    return results;
  }

  private static IssueStats calculateIssueStatsForIssuesWithRule(String ruleKey, List<IssueFromAnalysis> issues) {
    int issueCount = 0;
    int issueRating = 1;
    int issueMqrRating = 1;
    int hotspotsToReview = 0;
    int hotspotsReviewed = 0;

    for (IssueFromAnalysis issue : issues) {
      if (issue.isHotspot()) {
        if (IssueStatus.TO_REVIEW.toString().equals(issue.status())) {
          hotspotsToReview++;
        } else {
          hotspotsReviewed++;
        }
      } else {
        issueCount++;
        issueRating = Math.max(issueRating, issue.severity());
        issueMqrRating = Math.max(issueMqrRating, issue.mqrSeverity());
      }
    }

    return new IssueStats(ruleKey, issueCount, issueRating, issueMqrRating, hotspotsToReview, hotspotsReviewed);
  }

}
