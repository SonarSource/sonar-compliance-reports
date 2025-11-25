/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.dao.ActiveRuleDao;
import io.sonarcloud.compliancereports.dao.IssueStats;
import io.sonarcloud.compliancereports.dao.IssueStatsByRuleKeyDao;
import io.sonarcloud.compliancereports.reports.RuleBuckets.RuleBucket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static io.sonarcloud.compliancereports.dao.AggregationType.PROJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComplianceReportServiceTest {

  private static final String PROJECT_ID = UUID.randomUUID().toString();
  private static final ReportKey REPORT_KEY = new ReportKey("owasp", "2025");

  private final IssueStatsByRuleKeyDao issueStatsByRuleKeyDao = mock();
  private final ActiveRuleDao activeRuleDao = mock();
  private final MetadataLoader metadataLoader = mock();
  private final MetadataRules metadataRules = new MetadataRules(metadataLoader);
  private final RuleBuckets ruleBuckets = mock();

  private final ComplianceReportService underTest = new ComplianceReportService(issueStatsByRuleKeyDao, activeRuleDao, metadataRules);

  @Test
  void whenGetComplianceReport_shouldReturnReportWithCorrectData() {
    setupMetadata(Set.of(
      new RuleBucket("a1", Set.of("java:1", "java:2", "java:3", "java:4", "java:5")),
      new RuleBucket("a2", Set.of("java:6", "java:7", "java:8", "java:9", "java:10")),
      new RuleBucket("a3", Set.of("java:11", "java:12", "java:13", "java:14", "java:15")),
      new RuleBucket("a4", Set.of("java:16", "java:17", "java:18", "java:19", "java:20")),
      new RuleBucket("a5", Set.of("java:21", "java:22", "java:23", "java:24", "java:25"))
    ));

    setupIssueStats(List.of(
      new IssueStats("java:1", 100, 3, 0, 0),
      new IssueStats("java:7", 0, 1, 3, 7),
      new IssueStats("java:11", 20, 1, 5, 5),
      new IssueStats("java:13", 30, 2, 5, 5),
      new IssueStats("java:19", 0, 1, 7, 3),
      new IssueStats("java:25", 0, 1, 10, 0)
    ));

    setupActiveRules(Set.of("java:1", "java:7", "java:13", "java:19", "java:25"));

    var report = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY);

    assertThat(report)
      .hasEntrySatisfying("a1", categoryStats -> assertCategoryStats(categoryStats, 100, 3, 1, 0, 1, Map.of(3, 100)))
      .hasEntrySatisfying("a2", categoryStats -> assertCategoryStats(categoryStats, 0, 1, 1, 3, 2, Map.of(1, 0)))
      .hasEntrySatisfying("a3", categoryStats -> assertCategoryStats(categoryStats, 50, 2, 1, 10, 3, Map.of(1, 20, 2, 30)))
      .hasEntrySatisfying("a4", categoryStats -> assertCategoryStats(categoryStats, 0, 1, 1, 7, 4, Map.of(1, 0)))
      .hasEntrySatisfying("a5", categoryStats -> assertCategoryStats(categoryStats, 0, 1, 1, 10, 5, Map.of(1, 0)));
  }

  @Test
  void whenGetComplianceReport_shouldReturnReportWithCategoryMatchesWildcardRules() {
    setupMetadata(Set.of(
      new RuleBucket("a1", Set.of(":S1", "java:S1")),
      new RuleBucket("a2", Set.of("java:S2"))
    ));

    setupIssueStats(List.of(
      new IssueStats("java:S1", 100, 3, 0, 0),
      new IssueStats("cpp:S1", 100, 3, 0, 0),
      new IssueStats("java:S2", 0, 1, 3, 7),
      new IssueStats("cpp:S2", 0, 1, 3, 7)
    ));

    setupActiveRules(Set.of("java:S1", "java:S2"));

    var report = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY);

    assertThat(report)
      .hasEntrySatisfying("a1", categoryStats -> assertCategoryStats(categoryStats, 200, 3, 1, 0, 1, Map.of(3, 200)))
      .hasEntrySatisfying("a2", categoryStats -> assertCategoryStats(categoryStats, 0, 1, 1, 3, 2, Map.of(1, 0)));
  }

  @Test
  void whenGetComplianceReport_shouldNotDoubleCountWithOverlappingConcreteAndWildcardRules() {
    setupMetadata(Set.of(new RuleBucket("a1", Set.of(":S1", "java:S1"))));

    setupIssueStats(List.of(new IssueStats("java:S1", 100, 3, 0, 0)));

    setupActiveRules(Set.of("java:S1"));

    var report = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY);

    assertThat(report)
      .hasEntrySatisfying("a1", categoryStats -> assertCategoryStats(categoryStats, 100, 3, 1, 0, 1, Map.of(3, 100)));
  }

  @Test
  void whenGetComplianceReport_shouldHandleMultipleColonsInIssueStatsRuleKeys() {
    setupMetadata(Set.of(new RuleBucket("a1", Set.of(":S1", "java:security:S1"))));

    setupIssueStats(List.of(new IssueStats("java:security:S1", 100, 3, 0, 0)));

    setupActiveRules(Set.of("java:security:S1"));

    var report = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY);

    assertThat(report)
      .hasEntrySatisfying("a1", categoryStats -> assertCategoryStats(categoryStats, 100, 3, 1, 0, 1, Map.of(3, 100)));
  }

  private void setupMetadata(Set<RuleBucket> buckets) {
    when(ruleBuckets.getBuckets()).thenReturn(buckets);
    when(metadataLoader.getAllMetadata()).thenReturn(Map.of(REPORT_KEY, ruleBuckets));
  }

  private void setupIssueStats(List<IssueStats> stats) {
    when(issueStatsByRuleKeyDao.getIssueStats(PROJECT_ID, PROJECT)).thenReturn(stats);
  }

  private void setupActiveRules(Set<String> ruleKeys) {
    when(activeRuleDao.getActiveRuleKeys(PROJECT_ID, PROJECT)).thenReturn(ruleKeys);
  }

  private void assertCategoryStats(Object categoryStats, int openIssues, int rating, int activeRules,
    int toReviewHotspots, int hotspotRating, Map<Integer, Integer> ratingDistribution) {
    assertThat(categoryStats)
      .extracting("openIssues", "rating", "activeRules", "toReviewHotspots", "hotspotRating", "ratingDistribution")
      .containsExactly(openIssues, rating, activeRules, toReviewHotspots, hotspotRating, ratingDistribution);
  }
}