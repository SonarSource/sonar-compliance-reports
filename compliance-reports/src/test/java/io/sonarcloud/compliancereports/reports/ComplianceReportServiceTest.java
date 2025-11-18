/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.dao.ActiveRuleDao;
import io.sonarcloud.compliancereports.dao.IssueStats;
import io.sonarcloud.compliancereports.dao.IssueStatsByRuleKeyDao;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComplianceReportServiceTest {

  private static final UUID PROJECT_ID = UUID.randomUUID();
  private final IssueStatsByRuleKeyDao issueStatsByRuleKeyDao = mock();
  private final ActiveRuleDao activeRuleDao = mock();
  private final MetadataLoader metadataLoader = mock();
  private final RuleBuckets ruleBuckets = mock();

  private final ComplianceReportService underTest = new ComplianceReportService(issueStatsByRuleKeyDao, activeRuleDao, metadataLoader);

  @Test
  void whenGetComplianceReport_shouldReturnReportWithCorrectData() {
    when(ruleBuckets.getBuckets())
      .thenReturn(Set.of(
        new RuleBuckets.RuleBucket("a1", Set.of("java:1", "java:2", "java:3", "java:4", "java:5")),
        new RuleBuckets.RuleBucket("a2", Set.of("java:6", "java:7", "java:8", "java:9", "java:10")),
        new RuleBuckets.RuleBucket("a3", Set.of("java:11", "java:12", "java:13", "java:14", "java:15")),
        new RuleBuckets.RuleBucket("a4", Set.of("java:16", "java:17", "java:18", "java:19", "java:20")),
        new RuleBuckets.RuleBucket("a5", Set.of("java:21", "java:22", "java:23", "java:24", "java:25"))
      ));

    when(metadataLoader.getAllMetadata())
      .thenReturn(Map.of("owasp2025", ruleBuckets));

    when(issueStatsByRuleKeyDao.getIssueStatsForProject(PROJECT_ID))
      .thenReturn(List.of(
        new IssueStats("java:1", 100, 3, 0, 0),
        new IssueStats("java:7", 0, 1, 3, 7),
        new IssueStats("java:11", 20, 1, 5, 5),
        new IssueStats("java:13", 30, 2, 5, 5),
        new IssueStats("java:19", 0, 1, 7, 3),
        new IssueStats("java:25", 0, 1, 10, 0)
      ));

    when(activeRuleDao.getActiveRuleKeysForProject(PROJECT_ID))
      .thenReturn(Set.of("java:1", "java:7", "java:13", "java:19", "java:25"));

    var report = underTest.getComplianceReportForProject(PROJECT_ID, "owasp2025");

    assertThat(report)
      .hasEntrySatisfying("a1", (categoryStats -> {
        assertThat(categoryStats)
          .extracting("openIssues", "rating", "activeRules", "toReviewHotspots", "hotspotRating", "ratingDistribution")
          .containsExactly(100, 3, 1, 0, 1, Map.of(3, 100));
      }))
      .hasEntrySatisfying("a2", (categoryStats -> {
        assertThat(categoryStats)
          .extracting("openIssues", "rating", "activeRules", "toReviewHotspots", "hotspotRating", "ratingDistribution")
          .containsExactly(0, 1, 1, 3, 2, Map.of(1, 0));
      }))
      .hasEntrySatisfying("a3", (categoryStats -> {
        assertThat(categoryStats)
          .extracting("openIssues", "rating", "activeRules", "toReviewHotspots", "hotspotRating", "ratingDistribution")
          .containsExactly(50, 2, 1, 10, 3, Map.of(1, 20, 2, 30));
      }))
      .hasEntrySatisfying("a4", (categoryStats -> {
        assertThat(categoryStats)
          .extracting("openIssues", "rating", "activeRules", "toReviewHotspots", "hotspotRating", "ratingDistribution")
          .containsExactly(0, 1, 1, 7, 4, Map.of(1, 0));
      }))
      .hasEntrySatisfying("a5", (categoryStats -> {
        assertThat(categoryStats)
          .extracting("openIssues", "rating", "activeRules", "toReviewHotspots", "hotspotRating", "ratingDistribution")
          .containsExactly(0, 1, 1, 10, 5, Map.of(1, 0));
      }));
  }
}