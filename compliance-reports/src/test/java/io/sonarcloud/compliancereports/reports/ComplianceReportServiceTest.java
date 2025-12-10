/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.dao.ActiveRuleDao;
import io.sonarcloud.compliancereports.dao.IssueStats;
import io.sonarcloud.compliancereports.dao.IssueStatsByRuleKeyDao;
import io.sonarcloud.compliancereports.reports.CategoryTree.CategoryTreeNode;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
  private final Map<ReportKey, CategoryTree> metadataMap = new HashMap<>();
  private final ComplianceReportService underTest = new ComplianceReportService(issueStatsByRuleKeyDao, activeRuleDao, metadataRules);

  @BeforeEach
  void beforeEach() {
    when(metadataLoader.getAllMetadata()).thenReturn(metadataMap);
  }

  @Test
  void whenGetComplianceReport_shouldReturnReportWithCorrectData() {
    setupMetadata(List.of(
      new CategoryTreeNode("a1", Set.of("java:1", "java:2", "java:3", "java:4", "java:5"), Set.of(), null, false, 0),
      new CategoryTreeNode("a2", Set.of("java:6", "java:7", "java:8", "java:9", "java:10"), Set.of(), null, false, 0),
      new CategoryTreeNode("a3", Set.of("java:11", "java:12", "java:13", "java:14", "java:15"), Set.of(), null, false, 0),
      new CategoryTreeNode("a4", Set.of("java:16", "java:17", "java:18", "java:19", "java:20"), Set.of(), null, false, 0),
      new CategoryTreeNode("a5", Set.of("java:21", "java:22", "java:23", "java:24", "java:25"), Set.of(), null, false, 0)
    ));

    setupIssueStats(List.of(
      new IssueStats("java:1", 100, 3, 4, 0, 0),
      new IssueStats("java:7", 0, 1, 2, 3, 7),
      new IssueStats("java:11", 20, 1, 3, 5, 5),
      new IssueStats("java:13", 30, 2, 1, 5, 5),
      new IssueStats("java:19", 0, 1, 5, 7, 3),
      new IssueStats("java:25", 0, 1, 1, 10, 0)
    ));

    setupActiveRules(Set.of("java:1", "java:7", "java:13", "java:19", "java:25"));

    var report = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY);

    assertThat(report)
      .containsOnly(
        new CategoryStats("a1", 100, 0, 0, 3, 4, Map.of(3, 100), Map.of(4, 100), 1, 1, List.of()),
        new CategoryStats("a2", 0, 3, 7, 1, 2, Map.of(1, 0), Map.of(2, 0), 2, 1, List.of()),
        new CategoryStats("a3", 50, 10, 10, 2, 3, Map.of(1, 20, 2, 30), Map.of(1, 30, 3, 20), 3, 1, List.of()),
        new CategoryStats("a4", 0, 7, 3, 1, 5, Map.of(1, 0), Map.of(5, 0), 4, 1, List.of()),
        new CategoryStats("a5", 0, 10, 0, 1, 1, Map.of(1, 0), Map.of(1, 0), 5, 1, List.of()));
  }

  @Test
  void whenGetComplianceReportWithCwe_shouldReturnReportWithCorrectData() {
    setupMetadata(List.of(
      new CategoryTreeNode("a1", Set.of("java:1", "java:2", "java:3"), Set.of(), null, false, 0),
      new CategoryTreeNode("a2", Set.of("java:4", "java:5", "java:6"), Set.of(), null, false, 0),
      new CategoryTreeNode("a3", Set.of("java:7", "java:8", "java:9"), Set.of(), null, false, 0)
    ));
    ReportKey cweReport = new ReportKey("cwe", "v1");
    setupMetadata(cweReport, List.of(
      new CategoryTreeNode("cwe-1", Set.of("java:1", "java:4", "java:7"), Set.of(), null, false, 0),
      new CategoryTreeNode("cwe-2", Set.of("java:1", "java:2"), Set.of(), null, false, 0),
      new CategoryTreeNode("cwe-3", Set.of("java:1", "java:4", "java:8"), Set.of(), null, false, 0)
    ));

    setupIssueStats(List.of(
      new IssueStats("java:1", 100, 3, 3, 0, 0),
      new IssueStats("java:2", 1, 1, 1, 3, 7),
      new IssueStats("java:4", 20, 1, 1, 5, 5),
      new IssueStats("java:7", 30, 2, 2, 5, 5),
      new IssueStats("java:9", 1, 1, 1, 7, 3)
    ));

    setupActiveRules(Set.of("java:1", "java:2", "java:4", "java:7", "java:9"));

    var report = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY, cweReport, null);

    assertThat(report)
      .containsOnly(
        new CategoryStats("a1", 101, 3, 7, 3, 3, Map.of(1, 1, 3, 100), Map.of(1, 1, 3, 100), 2, 2,
          List.of(
            new CategoryStats("cwe-1", 100, 0, 0, 3, 3, Map.of(3, 100), Map.of(3, 100), 1, 1, List.of()),
            new CategoryStats("cwe-2", 101, 3, 7, 3, 3, Map.of(1, 1, 3, 100), Map.of(1, 1, 3, 100), 2, 2, List.of()),
            new CategoryStats("cwe-3", 100, 0, 0, 3, 3, Map.of(3, 100), Map.of(3, 100), 1, 1, List.of()))
        ),
        new CategoryStats("a2", 20, 5, 5, 1, 1, Map.of(1, 20), Map.of(1, 20), 3, 1, List.of(
          new CategoryStats("cwe-1", 20, 5, 5, 1, 1, Map.of(1, 20), Map.of(1, 20), 3, 1, List.of()),
          new CategoryStats("cwe-3", 20, 5, 5, 1, 1, Map.of(1, 20), Map.of(1, 20), 3, 1, List.of()))
        ),
        new CategoryStats("a3", 31, 12, 8, 2, 2, Map.of(1, 1, 2, 30), Map.of(1, 1, 2, 30), 4, 2, List.of(
          new CategoryStats("cwe-1", 30, 5, 5, 2, 2, Map.of(2, 30), Map.of(2, 30), 3, 1, List.of()))
        )
      );
  }

  @Test
  void whenGetComplianceReportWithLevels_shouldReturnReportWithCorrectData() {
    setupMetadata(List.of(
      new CategoryTreeNode("cat1", Set.of(), Set.of(
        new CategoryTreeNode("cat1.1", Set.of("java:9"), Set.of(), 0, true, 3),
        new CategoryTreeNode("cat1.2", Set.of("java:1"), Set.of(), 1, true, 3),
        new CategoryTreeNode("cat1.3", Set.of("java:4"), Set.of(), 2, true, 3)
      ), null, true, 3),
      new CategoryTreeNode("cat2", Set.of(), Set.of(
        new CategoryTreeNode("cat2.2", Set.of(), Set.of(
          new CategoryTreeNode("cat2.2.1", Set.of("java:9"), Set.of(), 0, true, 3),
          new CategoryTreeNode("cat2.2.2", Set.of("java:9", "java:1"), Set.of(), 2, true, 3)
        ), null, true, 3)
      ), null, true, 3)
    ));

    setupIssueStats(List.of(
      new IssueStats("java:1", 100, 3, 3, 0, 0),
      new IssueStats("java:2", 1, 1, 1, 3, 7),
      new IssueStats("java:4", 20, 1, 1, 5, 5),
      new IssueStats("java:7", 30, 2, 2, 5, 5),
      new IssueStats("java:9", 3, 2, 1, 7, 3)
    ));

    setupActiveRules(Set.of("java:1", "java:2", "java:4", "java:7", "java:9"));

    var reportLevel1 = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY, null, 0);
    var reportLevel2 = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY, null, 1);
    var reportLevel3 = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY, null, 2);

    assertThat(reportLevel1)
      .containsOnly(
        new CategoryStats("cat1", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1,
          List.of(
            new CategoryStats("cat1.1", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1, List.of()),
            new CategoryStats("cat1.2", 0, 0, 0, 1, 1, Map.of(), Map.of(), 1, 0, List.of()),
            new CategoryStats("cat1.3", 0, 0, 0, 1, 1, Map.of(), Map.of(), 1, 0, List.of()))
        ),
        new CategoryStats("cat2", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1, List.of(
          new CategoryStats("cat2.2", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1,3 ), 4, 1, List.of(
            new CategoryStats("cat2.2.1", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1, List.of()),
            new CategoryStats("cat2.2.2", 0, 0, 0, 1, 1, Map.of(), Map.of(), 1, 0, List.of())
          )))
        )
      );

    assertThat(reportLevel2)
      .containsOnly(
        new CategoryStats("cat1", 103, 7, 3, 3, 3, Map.of(2, 3, 3, 100), Map.of(1, 3, 3, 100), 4, 2,
          List.of(
            new CategoryStats("cat1.1", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1, List.of()),
            new CategoryStats("cat1.2", 100, 0, 0, 3, 3, Map.of(3, 100), Map.of(3, 100), 1, 1, List.of()),
            new CategoryStats("cat1.3", 0, 0, 0, 1, 1, Map.of(), Map.of(), 1, 0, List.of()))
        ),
        new CategoryStats("cat2", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1, List.of(
          new CategoryStats("cat2.2", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1,3 ), 4, 1, List.of(
            new CategoryStats("cat2.2.1", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1, List.of()),
            new CategoryStats("cat2.2.2", 0, 0, 0, 1, 1, Map.of(), Map.of(), 1, 0, List.of())
          )))
        )
      );

    assertThat(reportLevel3)
      .containsOnly(
        new CategoryStats("cat1", 123, 12, 8, 3, 3, Map.of(2, 3, 3, 100, 1, 20), Map.of(1, 23, 3, 100), 4, 3,
          List.of(
            new CategoryStats("cat1.1", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1, List.of()),
            new CategoryStats("cat1.2", 100, 0, 0, 3, 3, Map.of(3, 100), Map.of(3, 100), 1, 1, List.of()),
            new CategoryStats("cat1.3", 20, 5, 5, 1, 1, Map.of(1, 20), Map.of(1, 20), 3, 1, List.of()))
        ),
        new CategoryStats("cat2", 103, 7, 3, 3, 3, Map.of(2, 3, 3, 100), Map.of(1,3, 3, 100), 4, 2, List.of(
          new CategoryStats("cat2.2", 103, 7, 3, 3, 3, Map.of(2, 3, 3, 100), Map.of(1,3, 3, 100), 4, 2, List.of(
            new CategoryStats("cat2.2.1", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1, List.of()),
            new CategoryStats("cat2.2.2", 103, 7, 3, 3, 3, Map.of(2, 3, 3, 100), Map.of(1, 3, 3, 100), 4, 2, List.of())
          )))
        )
      );
  }

  @Test
  void whenGetComplianceReport_shouldReturnReportWithCategoryMatchesWildcardRules() {
    setupMetadata(List.of(
      new CategoryTreeNode("a1", Set.of(":S1", "java:S1"), Set.of(), null, false, 0),
      new CategoryTreeNode("a2", Set.of("java:S2"), Set.of(), null, false, 0)
    ));

    setupIssueStats(List.of(
      new IssueStats("java:S1", 100, 3, 3, 0, 0),
      new IssueStats("cpp:S1", 100, 3, 3, 0, 0),
      new IssueStats("java:S2", 0, 1, 1, 3, 7),
      new IssueStats("cpp:S2", 0, 1, 1, 3, 7)
    ));

    setupActiveRules(Set.of("java:S1", "java:S2"));

    var report = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY);

    assertThat(report).containsOnly(
      new CategoryStats("a1", 200, 0, 0, 3, 3, Map.of(3, 200), Map.of(3, 200), 1, 1, List.of()),
      new CategoryStats("a2", 0, 3, 7, 1, 1, Map.of(1, 0), Map.of(1, 0), 2, 1, List.of())
    );
  }

  @Test
  void whenGetComplianceReport_shouldNotDoubleCountWithOverlappingConcreteAndWildcardRules() {
    setupMetadata(List.of(new CategoryTreeNode("a1", Set.of(":S1", "java:S1"), Set.of(), null, false, 0)));

    setupIssueStats(List.of(new IssueStats("java:S1", 100, 3, 3, 0, 0)));

    setupActiveRules(Set.of("java:S1"));

    var report = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY);

    assertThat(report).containsOnly(
      new CategoryStats("a1", 100, 0, 0, 3, 3, Map.of(3, 100), Map.of(3, 100), 1, 1, List.of())
    );
  }

  @Test
  void whenGetComplianceReport_shouldHandleMultipleColonsInIssueStatsRuleKeys() {
    setupMetadata(List.of(new CategoryTreeNode("a1", Set.of(":S1", "java:security:S1"), Set.of(), null, false, 0)));

    setupIssueStats(List.of(new IssueStats("java:security:S1", 100, 3, 3, 0, 0)));

    setupActiveRules(Set.of("java:security:S1"));

    var report = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY);

    assertThat(report).containsOnly(
      new CategoryStats("a1", 100, 0, 0, 3, 3, Map.of(3, 100), Map.of(3, 100), 1, 1, List.of())
    );
  }

  private void setupMetadata(List<CategoryTreeNode> buckets) {
    setupMetadata(REPORT_KEY, buckets);
  }

  private void setupMetadata(ReportKey reportKey, List<CategoryTreeNode> buckets) {
    CategoryTree ruleBuckets = mock();
    when(ruleBuckets.getChildren()).thenReturn(new LinkedHashSet<>(buckets));
    metadataMap.put(reportKey, ruleBuckets);
  }

  private void setupIssueStats(List<IssueStats> stats) {
    when(issueStatsByRuleKeyDao.getIssueStats(PROJECT_ID, PROJECT)).thenReturn(stats);
  }

  private void setupActiveRules(Set<String> ruleKeys) {
    when(activeRuleDao.getActiveRuleKeys(PROJECT_ID, PROJECT)).thenReturn(ruleKeys);
  }
}