/*
 * Copyright (C) 2022-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

package io.sonarcloud.compliancereports.ingestion;

import io.sonarcloud.compliancereports.dao.IssueStats;
import io.sonarcloud.compliancereports.dao.IssueStatsByRuleKeyDao;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IssueIngestionServiceTest {

  private static final UUID PROJECT_ID = UUID.randomUUID();
  private final IssueStatsByRuleKeyDao dao = mock();
  private final IssueIngestionService underTest = new IssueIngestionService(dao);

  @Test
  void onIngestion_shouldPopulateDataStore() {
    var issues = List.of(
      new IssueFromAnalysis("java:1", false, 2),
      new IssueFromAnalysis("java:1", false, 2),
      new IssueFromAnalysis("java:1", false, 2),
      new IssueFromAnalysis("python:42", false, 3),
      new IssueFromAnalysis("python:42", false, 3),
      new IssueFromAnalysis("cs:100", true, 5)
    );

    underTest.ingest(PROJECT_ID, issues);

    verify(dao).deleteAllIssueStatsForProject(PROJECT_ID);
    ArgumentCaptor<List<IssueStats>> issuesCaptor = ArgumentCaptor.captor();
    verify(dao).insertIssueStatsForProject(eq(PROJECT_ID), issuesCaptor.capture());
    var capturedIssues = issuesCaptor.getValue();

    assertThat(capturedIssues)
      .hasSize(3)
      .containsExactlyInAnyOrder(
        new IssueStats("java:1", 3, 2, 0, 1),
        new IssueStats("python:42", 2, 3, 0, 1),
        new IssueStats("cs:100", 0, 1, 1, 5)
      );
  }
}