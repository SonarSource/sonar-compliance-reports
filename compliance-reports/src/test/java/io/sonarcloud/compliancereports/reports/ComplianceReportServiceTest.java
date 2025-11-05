/*
 * Copyright (C) 2022-2025 SonarSource SA
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
  private final Map<String, Map<String, Set<String>>> metadata = Map.of(
    "owasp2025", Map.of(
      "a1", Set.of("java:1", "java:2", "java:3", "java:4", "java:5"),
      "a2", Set.of("java:6", "java:7", "java:8", "java:9", "java:10"),
      "a3", Set.of("java:11", "java:12", "java:13", "java:14", "java:15"),
      "a4", Set.of("java:16", "java:17", "java:18", "java:19", "java:20")
    )
  );
  private final ComplianceReportService underTest = new ComplianceReportService(issueStatsByRuleKeyDao, activeRuleDao, metadata);

  @Test
  void whenGetComplianceReport_shouldReturnReportWithCorrectData() {
    when(issueStatsByRuleKeyDao.getIssueStatsForProject(PROJECT_ID))
      .thenReturn(List.of(
        new IssueStats("java:1", 4, 3, 0, 1),
        new IssueStats("java:7", 15, 5, 0, 1),
        new IssueStats("java:13", 2, 1, 0, 1),
        new IssueStats("java:19", 100, 4, 0, 1)
      ));

    when(activeRuleDao.getActiveRuleKeysForProject(PROJECT_ID))
      .thenReturn(Set.of("java:1", "java:7", "java:13", "java:19"));

    var report = underTest.getComplianceReportForProject(PROJECT_ID, "owasp2025");

    assertThat(report)
      .hasEntrySatisfying("a1", (categoryStats -> {
        assertThat(categoryStats)
          .extracting("totalIssues", "rating", "activeRules")
          .containsExactly(4, 3, 1);
      }))
      .hasEntrySatisfying("a2", (categoryStats -> {
        assertThat(categoryStats)
          .extracting("totalIssues", "rating", "activeRules")
          .containsExactly(15, 5, 1);
      }))
      .hasEntrySatisfying("a3", (categoryStats -> {
        assertThat(categoryStats)
          .extracting("totalIssues", "rating", "activeRules")
          .containsExactly(2, 1, 1);
      }))
      .hasEntrySatisfying("a4", (categoryStats -> {
        assertThat(categoryStats)
          .extracting("totalIssues", "rating", "activeRules")
          .containsExactly(100, 4, 1);
      }));
  }
}