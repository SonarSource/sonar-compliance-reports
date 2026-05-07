/*
 * Compliance Reports
 * Copyright (C) 2022-2026 SonarSource Sàrl
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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonarsource.compliancereports.dao.ActiveRuleDao;
import org.sonarsource.compliancereports.dao.IssueStats;
import org.sonarsource.compliancereports.dao.IssueStatsByRuleKeyDao;
import org.sonarsource.compliancereports.reports.CategoryTree.CategoryTreeNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonarsource.compliancereports.dao.AggregationType.PROJECT;

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
      categoryNode("a1", Set.of("java:1", "java:2", "java:3", "java:4", "java:5")),
      categoryNode("a2", Set.of("java:6", "java:7", "java:8", "java:9", "java:10")),
      categoryNode("a3", Set.of("java:11", "java:12", "java:13", "java:14", "java:15")),
      categoryNode("a4", Set.of("java:16", "java:17", "java:18", "java:19", "java:20")),
      categoryNode("a5", Set.of("java:21", "java:22", "java:23", "java:24", "java:25"))
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
        leafCategory("a1", 100, 0, 0, 3, 4, Map.of(3, 100), Map.of(4, 100), 1, 1),
        leafCategory("a2", 0, 3, 7, 1, 2, Map.of(1, 0), Map.of(2, 0), 2, 1),
        leafCategory("a3", 50, 10, 10, 2, 3, Map.of(1, 20, 2, 30), Map.of(1, 30, 3, 20), 3, 1),
        leafCategory("a4", 0, 7, 3, 1, 5, Map.of(1, 0), Map.of(5, 0), 4, 1),
        leafCategory("a5", 0, 10, 0, 1, 1, Map.of(1, 0), Map.of(1, 0), 5, 1));
  }

  @Test
  void whenGetComplianceReportWithCwe_shouldReturnReportWithCorrectData() {
    setupMetadata(List.of(
      categoryNode("a1", Set.of("java:1", "java:2", "java:3")),
      categoryNode("a2", Set.of("java:4", "java:5", "java:6")),
      categoryNode("a3", Set.of("java:7", "java:8", "java:9"))
    ));
    ReportKey cweReport = new ReportKey("cwe", "v1");
    setupMetadata(cweReport, List.of(
      categoryNode("cwe-1", Set.of("java:1", "java:4", "java:7")),
      categoryNode("cwe-2", Set.of("java:1", "java:2")),
      categoryNode("cwe-3", Set.of("java:1", "java:4", "java:8"))
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
        categoryWithChildren("a1", 101, 3, 7, 3, 3, Map.of(1, 1, 3, 100), Map.of(1, 1, 3, 100), 2, 2,
          List.of(
            leafCategory("cwe-1", 100, 0, 0, 3, 3, Map.of(3, 100), Map.of(3, 100), 1, 1),
            leafCategory("cwe-2", 101, 3, 7, 3, 3, Map.of(1, 1, 3, 100), Map.of(1, 1, 3, 100), 2, 2),
            leafCategory("cwe-3", 100, 0, 0, 3, 3, Map.of(3, 100), Map.of(3, 100), 1, 1))
        ),
        categoryWithChildren("a2", 20, 5, 5, 1, 1, Map.of(1, 20), Map.of(1, 20), 3, 1, List.of(
          leafCategory("cwe-1", 20, 5, 5, 1, 1, Map.of(1, 20), Map.of(1, 20), 3, 1),
          leafCategory("cwe-3", 20, 5, 5, 1, 1, Map.of(1, 20), Map.of(1, 20), 3, 1))
        ),
        categoryWithChildren("a3", 31, 12, 8, 2, 2, Map.of(1, 1, 2, 30), Map.of(1, 1, 2, 30), 4, 2, List.of(
          leafCategory("cwe-1", 30, 5, 5, 2, 2, Map.of(2, 30), Map.of(2, 30), 3, 1))
        )
      );
  }

  @Test
  void whenGetComplianceReportWithLevels_shouldReturnReportWithCorrectData() {
    setupMetadata(List.of(
      new CategoryTreeNode("cat1", Set.of(), Set.of(
        new CategoryTreeNode("cat1.1", Set.of("java:9"), Set.of(), 0, true, 3, null, null),
        new CategoryTreeNode("cat1.2", Set.of("java:1"), Set.of(), 1, true, 3, null, null),
        new CategoryTreeNode("cat1.3", Set.of("java:4"), Set.of(), 2, true, 3, null, null)
      ), null, true, 3, null, null),
      new CategoryTreeNode("cat2", Set.of(), Set.of(
        new CategoryTreeNode("cat2.2", Set.of(), Set.of(
          new CategoryTreeNode("cat2.2.1", Set.of("java:9"), Set.of(), 0, true, 3, null, null),
          new CategoryTreeNode("cat2.2.2", Set.of("java:9", "java:1"), Set.of(), 2, true, 3, null, null)
        ), null, true, 3, null, null)
      ), null, true, 3, null, null)
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
        categoryWithChildren("cat1", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1,
          List.of(
            leafCategory("cat1.1", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1),
            leafCategory("cat1.2", 0, 0, 0, 1, 1, Map.of(), Map.of(), 1, 0),
            leafCategory("cat1.3", 0, 0, 0, 1, 1, Map.of(), Map.of(), 1, 0))
        ),
        categoryWithChildren("cat2", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1, List.of(
          categoryWithChildren("cat2.2", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1,3 ), 4, 1, List.of(
            leafCategory("cat2.2.1", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1),
            leafCategory("cat2.2.2", 0, 0, 0, 1, 1, Map.of(), Map.of(), 1, 0)
          )))
        )
      );

    assertThat(reportLevel2)
      .containsOnly(
        categoryWithChildren("cat1", 103, 7, 3, 3, 3, Map.of(2, 3, 3, 100), Map.of(1, 3, 3, 100), 4, 2,
          List.of(
            leafCategory("cat1.1", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1),
            leafCategory("cat1.2", 100, 0, 0, 3, 3, Map.of(3, 100), Map.of(3, 100), 1, 1),
            leafCategory("cat1.3", 0, 0, 0, 1, 1, Map.of(), Map.of(), 1, 0))
        ),
        categoryWithChildren("cat2", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1, List.of(
          categoryWithChildren("cat2.2", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1,3 ), 4, 1, List.of(
            leafCategory("cat2.2.1", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1),
            leafCategory("cat2.2.2", 0, 0, 0, 1, 1, Map.of(), Map.of(), 1, 0)
          )))
        )
      );

    assertThat(reportLevel3)
      .containsOnly(
        categoryWithChildren("cat1", 123, 12, 8, 3, 3, Map.of(2, 3, 3, 100, 1, 20), Map.of(1, 23, 3, 100), 4, 3,
          List.of(
            leafCategory("cat1.1", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1),
            leafCategory("cat1.2", 100, 0, 0, 3, 3, Map.of(3, 100), Map.of(3, 100), 1, 1),
            leafCategory("cat1.3", 20, 5, 5, 1, 1, Map.of(1, 20), Map.of(1, 20), 3, 1))
        ),
        categoryWithChildren("cat2", 103, 7, 3, 3, 3, Map.of(2, 3, 3, 100), Map.of(1,3, 3, 100), 4, 2, List.of(
          categoryWithChildren("cat2.2", 103, 7, 3, 3, 3, Map.of(2, 3, 3, 100), Map.of(1,3, 3, 100), 4, 2, List.of(
            leafCategory("cat2.2.1", 3, 7, 3, 2, 1, Map.of(2, 3), Map.of(1, 3), 4, 1),
            leafCategory("cat2.2.2", 103, 7, 3, 3, 3, Map.of(2, 3, 3, 100), Map.of(1, 3, 3, 100), 4, 2)
          )))
        )
      );
  }

  @Test
  void whenGetComplianceReport_shouldReturnReportWithCategoryMatchesWildcardRules() {
    setupMetadata(List.of(
      categoryNode("a1", Set.of(":S1", "java:S1")),
      categoryNode("a2", Set.of("java:S2")),
      categoryNode("a3", Set.of("cpp:"))
    ));

    setupIssueStats(List.of(
      new IssueStats("java:S1", 100, 3, 3, 0, 0),
      new IssueStats("cpp:S1", 100, 3, 3, 0, 0),
      new IssueStats("java:S2", 0, 1, 1, 3, 7),
      new IssueStats("cpp:S2", 0, 1, 1, 3, 7),
      new IssueStats("cpp:S3", 13, 1, 1, 0, 0)
    ));

    setupActiveRules(Set.of("java:S1", "java:S2", "cpp:S3"));

    var report = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY);

    assertThat(report).containsOnly(
      leafCategory("a1", 200, 0, 0, 3, 3, Map.of(3, 200), Map.of(3, 200), 1, 1),
      leafCategory("a2", 0, 3, 7, 1, 1, Map.of(1, 0), Map.of(1, 0), 2, 1),
      leafCategory("a3", 113, 3, 7, 3, 3, Map.of(1, 13, 3, 100), Map.of(1, 13, 3, 100), 2, 1)
    );
  }

  @Test
  void whenGetComplianceReport_shouldNotDoubleCountWithOverlappingConcreteAndWildcardRules() {
    setupMetadata(List.of(categoryNode("a1", Set.of(":S1", "java:S1"))));

    setupIssueStats(List.of(new IssueStats("java:S1", 100, 3, 3, 0, 0)));

    setupActiveRules(Set.of("java:S1"));

    var report = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY);

    assertThat(report).containsOnly(
      leafCategory("a1", 100, 0, 0, 3, 3, Map.of(3, 100), Map.of(3, 100), 1, 1)
    );
  }

  @Test
  void whenGetComplianceReport_shouldHandleMultipleColonsInIssueStatsRuleKeys() {
    setupMetadata(List.of(categoryNode("a1", Set.of(":S1", "java:security:S1"))));

    setupIssueStats(List.of(new IssueStats("java:security:S1", 100, 3, 3, 0, 0)));

    setupActiveRules(Set.of("java:security:S1"));

    var report = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY);

    assertThat(report).containsOnly(
      leafCategory("a1", 100, 0, 0, 3, 3, Map.of(3, 100), Map.of(3, 100), 1, 1)
    );
  }

  @Test
  void whenGetComplianceReport_shouldReturnInOrderWhenOrdinalsAreProvided() {
    setupMetadata(List.of(
      new CategoryTreeNode("a2", Set.of("java:2"), Set.of(), null, false, 0, 2, null),
      new CategoryTreeNode("a1", Set.of("java:1"), Set.of(), null, false, 0, 3, null),
      new CategoryTreeNode("a3", Set.of("java:3"), Set.of(), null, false, 0, 1, null) // Should be returned first
    ));

    setupIssueStats(List.of(
      new IssueStats("java:1", 10, 1, 1, 0, 0),
      new IssueStats("java:2", 20, 2, 2, 0, 0),
      new IssueStats("java:3", 30, 3, 3, 0, 0)
    ));

    setupActiveRules(Set.of("java:1", "java:2", "java:3"));

    var report = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY);

    assertThat(report)
      .containsExactly(
        leafCategory("a3", 30, 0, 0, 3, 3, Map.of(3, 30), Map.of(3, 30), 1, 1),
        leafCategory("a2", 20, 0, 0, 2, 2, Map.of(2, 20), Map.of(2, 20), 1, 1),
        leafCategory("a1", 10, 0, 0, 1, 1, Map.of(1, 10), Map.of(1, 10), 1, 1)
      );
  }

  @Test
  void whenGetComplianceReport_shouldReturnInNameOrderWhenOrdinalsAreNotPresent() {
    setupMetadata(List.of(
      categoryNode("a2", Set.of("java:2")),
      categoryNode("a1", Set.of("java:1")), // Should be returned first
      categoryNode("a3", Set.of("java:3"))
    ));

    setupIssueStats(List.of(
      new IssueStats("java:1", 10, 1, 1, 0, 0),
      new IssueStats("java:2", 20, 2, 2, 0, 0),
      new IssueStats("java:3", 30, 3, 3, 0, 0)
    ));

    setupActiveRules(Set.of("java:1", "java:2", "java:3"));

    var report = underTest.getComplianceReport(PROJECT_ID, PROJECT, REPORT_KEY);

    assertThat(report)
      .containsExactly(
        leafCategory("a1", 10, 0, 0, 1, 1, Map.of(1, 10), Map.of(1, 10), 1, 1),
        leafCategory("a2", 20, 0, 0, 2, 2, Map.of(2, 20), Map.of(2, 20), 1, 1),
        leafCategory("a3", 30, 0, 0, 3, 3, Map.of(3, 30), Map.of(3, 30), 1, 1)
      );
  }

  @Test
  void whenGetCategoryTree_shouldReturnTreeForKnownReportKey() {
    setupMetadata(List.of(categoryNode("a1", Set.of("java:1"))));
    var tree = underTest.getCategoryTree(REPORT_KEY);
    assertThat(tree).isEqualTo(metadataMap.get(REPORT_KEY));
  }

  @Test
  void whenGetCategoryTree_shouldThrowForUnknownReportKey() {
    assertThatThrownBy(() -> underTest.getCategoryTree(new ReportKey("unknown", "v999")))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void whenGetComplianceReportForAggregations_shouldReturnReportsForEachAggregation() {
    setupMetadata(List.of(
      categoryNode("a1", Set.of("java:1")),
      categoryNode("a2", Set.of("java:2")),
      categoryNode("a3", Set.of("java:3"))
    ));

    String projectUuid1 = UUID.randomUUID().toString();
    String projectUuid2 = UUID.randomUUID().toString();
    String projectUuid3 = UUID.randomUUID().toString();

    Map<String, List<IssueStats>> issueStatsByAggregationId = Map.of(
      projectUuid1, List.of(
        new IssueStats("java:1", 10, 1, 1, 0, 0),
        new IssueStats("java:2", 20, 2, 2, 0, 0),
        new IssueStats("java:3", 30, 3, 3, 0, 0)
      ),
      projectUuid2, List.of(
        new IssueStats("java:1", 1, 3, 3, 0, 0),
        new IssueStats("java:2", 2, 1, 1, 0, 0),
        new IssueStats("java:3", 3, 2, 2, 0, 0)
      ),
      projectUuid3, List.of(
        new IssueStats("java:1", 0, 1, 1, 3, 3),
        new IssueStats("java:2", 0, 1, 1, 17, 0),
        new IssueStats("java:3", 0, 1, 1, 2, 21)
      )
    );

    setupIssueStatsForMultipleAggregations(issueStatsByAggregationId);

    var reportsByAggregationId = underTest.getComplianceReportForAggregations(issueStatsByAggregationId.keySet(), PROJECT, REPORT_KEY);

    assertThat(reportsByAggregationId)
      .hasSize(3)
      .hasEntrySatisfying(projectUuid1, report ->
        assertThat(report)
          .containsExactly(
            leafCategory("a1", 10, 0, 0, 1, 1, Map.of(1, 10), Map.of(1, 10), 1, 0),
            leafCategory("a2", 20, 0, 0, 2, 2, Map.of(2, 20), Map.of(2, 20), 1, 0),
            leafCategory("a3", 30, 0, 0, 3, 3, Map.of(3, 30), Map.of(3, 30), 1, 0)
          )
      )
      .hasEntrySatisfying(projectUuid2, report ->
        assertThat(report)
          .containsExactly(
            leafCategory("a1", 1, 0, 0, 3, 3, Map.of(3, 1), Map.of(3, 1), 1, 0),
            leafCategory("a2", 2, 0, 0, 1, 1, Map.of(1, 2), Map.of(1, 2), 1, 0),
            leafCategory("a3", 3, 0, 0, 2, 2, Map.of(2, 3), Map.of(2, 3), 1, 0)
          )
      )
      .hasEntrySatisfying(projectUuid3, report ->
        assertThat(report)
          .containsExactly(
            leafCategory("a1", 0, 3, 3, 1, 1, Map.of(1, 0), Map.of(1, 0), 3, 0),
            leafCategory("a2", 0, 17, 0, 1, 1, Map.of(1, 0), Map.of(1, 0), 5, 0),
            leafCategory("a3", 0, 2, 21, 1, 1, Map.of(1, 0), Map.of(1, 0), 1, 0)
          )
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

  private void setupIssueStatsForMultipleAggregations(Map<String, List<IssueStats>> statsByAggregationId) {
    when(issueStatsByRuleKeyDao.getIssueStatsByAggregationIds(statsByAggregationId.keySet(), PROJECT))
      .thenReturn(statsByAggregationId);
  }

  private void setupActiveRules(Set<String> ruleKeys) {
    when(activeRuleDao.getActiveRuleKeys(PROJECT_ID, PROJECT)).thenReturn(ruleKeys);
  }

  private CategoryTreeNode categoryNode(String name, Set<String> ruleKeys) {
    return new CategoryTreeNode(name, ruleKeys, Set.of(), null, false, 0, null, null);
  }

  private CategoryStats leafCategory(
    String categoryName,
    int openIssues,
    int toReviewHotspots,
    int reviewedHotspots,
    int rating,
    int mqrRating,
    Map<Integer, Integer> ratingDistribution,
    Map<Integer, Integer> mqrRatingDistribution,
    int hotspotRating,
    int activeRules
  ) {
    return new CategoryStats(
      categoryName,
      openIssues,
      toReviewHotspots,
      reviewedHotspots,
      rating,
      mqrRating,
      ratingDistribution,
      mqrRatingDistribution,
      hotspotRating,
      activeRules,
      List.of()
    );
  }

  private CategoryStats categoryWithChildren(
    String categoryName,
    int openIssues,
    int toReviewHotspots,
    int reviewedHotspots,
    int rating,
    int mqrRating,
    Map<Integer, Integer> ratingDistribution,
    Map<Integer, Integer> mqrRatingDistribution,
    int hotspotRating,
    int activeRules,
    List<CategoryStats> children
  ) {
    return new CategoryStats(
      categoryName,
      openIssues,
      toReviewHotspots,
      reviewedHotspots,
      rating,
      mqrRating,
      ratingDistribution,
      mqrRatingDistribution,
      hotspotRating,
      activeRules,
      children
    );
  }
}
