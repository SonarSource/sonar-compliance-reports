/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonarsource.compliancereports.ingestion;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonarsource.compliancereports.dao.IssueStats;
import org.sonarsource.compliancereports.dao.IssueStatsByRuleKeyDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonarsource.compliancereports.dao.AggregationType.PROJECT;

class IssueIngestionServiceTest {

  private static final String PROJECT_ID = UUID.randomUUID().toString();
  private final IssueStatsByRuleKeyDao dao = mock();
  private final IssueIngestionService underTest = new IssueIngestionService(dao);

  @Test
  void onIngestion_shouldPopulateDataStore() {
    var issues = List.of(
      new IssueFromAnalysis("java:1", "OPEN", false, 2, 2),
      new IssueFromAnalysis("java:1", "OPEN", false, 2, 2),
      new IssueFromAnalysis("java:1", "OPEN", false, 2, 2),
      new IssueFromAnalysis("python:42", "OPEN", false, 3, 3),
      new IssueFromAnalysis("python:42", "OPEN", false, 3, 3),
      new IssueFromAnalysis("python:42", "FIXED", false, 3, 3),
      new IssueFromAnalysis("cs:100", "TO_REVIEW", true, 5, 5),
      new IssueFromAnalysis("cs:100", "REVIEWED", true, 5, 5)
    );

    underTest.ingest(PROJECT_ID, PROJECT, issues);
    ArgumentCaptor<List<IssueStats>> issuesCaptor = ArgumentCaptor.captor();

    verify(dao).deleteAndInsertIssueStats(eq(PROJECT_ID), eq(PROJECT), issuesCaptor.capture());
    var capturedIssues = issuesCaptor.getValue();

    assertThat(capturedIssues)
      .containsExactlyInAnyOrder(
        new IssueStats("java:1", 3, 2, 2, 0, 0),
        new IssueStats("python:42", 3, 3, 3, 0, 0),
        new IssueStats("cs:100", 0, 1, 1, 1, 1)
      );
  }
}
