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

  /**
   * Ingest issues from analysis in bulk
   */
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


  /**
   * Modify the hotspot count of an issue_stats_by_rule_key row by the given amount
   */
  public void adjustHotspotStats(String aggregationId, AggregationType aggregationType, String ruleKey, int adjustment) {
    var issueStats = issueStatsByRuleKeyDao.getIssueStats(aggregationId, aggregationType).stream()
      .filter(i -> i.ruleKey().equals(ruleKey))
      .findFirst()
      .orElse(new IssueStats(ruleKey, 0, 1, 1, 0, 0));

    var updatedIssueStats = new IssueStats(issueStats.ruleKey(), 0, issueStats.rating(), issueStats.mqrRating(),
      issueStats.hotspotCount() + adjustment, issueStats.hotspotsReviewed() - adjustment);
    if (updatedIssueStats.issueCount() != 0 || updatedIssueStats.hotspotCount() != 0 || updatedIssueStats.hotspotsReviewed() != 0) {
      issueStatsByRuleKeyDao.upsert(aggregationId, aggregationType, updatedIssueStats);
    } else {
      issueStatsByRuleKeyDao.deleteByAggregationAndRuleKey(aggregationId, aggregationType, ruleKey);
    }
  }

  /**
   * Modify the issue count of an issue_stats_by_rule_key row by the given amount
   */
  public void adjustIssueStats(String aggregationId, AggregationType aggregationType, String ruleKey, int issueRating, int issueMqrRating,
    int adjustment) {
    var issueStats = issueStatsByRuleKeyDao.getIssueStats(aggregationId, aggregationType).stream()
      .filter(i -> i.ruleKey().equals(ruleKey))
      .findFirst()
      .orElse(new IssueStats(ruleKey, 0, issueRating, issueMqrRating, 0, 0));

    var updatedIssueStats = getUpdatedIssueStats(aggregationId, ruleKey, issueStats, adjustment);
    if (updatedIssueStats.issueCount() != 0 || updatedIssueStats.hotspotCount() != 0 || updatedIssueStats.hotspotsReviewed() != 0) {
      issueStatsByRuleKeyDao.upsert(aggregationId, aggregationType, updatedIssueStats);
    } else {
      issueStatsByRuleKeyDao.deleteByAggregationAndRuleKey(aggregationId, aggregationType, ruleKey);
    }
  }

  private IssueStats getUpdatedIssueStats(String aggregationId, String ruleKey, IssueStats oldStats, int adjustment) {
    int newIssueCount = oldStats.issueCount() + adjustment;
    if (newIssueCount == 0) {
      return new IssueStats(oldStats.ruleKey(), newIssueCount, 1, 1, 0, 0);
    }

    var newIssueStats = issueStatsByRuleKeyDao.aggregateIssueStatsForBranchUuidAndRuleKey(aggregationId, ruleKey);
    int newRating = newIssueStats == null ? oldStats.rating() : newIssueStats.rating();
    int newMqrRating = newIssueStats == null ? oldStats.mqrRating() : newIssueStats.mqrRating();

    return new IssueStats(oldStats.ruleKey(), newIssueCount, newRating, newMqrRating, 0, 0);
  }

}
