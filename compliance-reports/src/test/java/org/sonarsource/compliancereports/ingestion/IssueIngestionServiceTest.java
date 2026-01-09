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
import static org.mockito.Mockito.when;
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

  @Test
  void onAdjustHotspotStats_shouldUpdateDataStore() {
    var initialHotspotCount = 1;
    var initialiHotspotsReviewed = 3;
    var adjustment = 1;
    when(dao.getIssueStats(PROJECT_ID, PROJECT))
      .thenReturn(List.of(new IssueStats("cs:100", 0, 1, 1, initialHotspotCount, initialiHotspotsReviewed)));

    underTest.adjustHotspotStats(PROJECT_ID, PROJECT, "cs:100", adjustment);

    verify(dao)
      .upsert(PROJECT_ID, PROJECT, new IssueStats("cs:100", 0, 1, 1, initialHotspotCount + adjustment, initialiHotspotsReviewed - adjustment));
  }

  @Test
  void onAdjustIssueStats_shouldUpdateDataStore() {
    var initialIssueCount = 3;
    var adjustment = -1;
    when(dao.getIssueStats(PROJECT_ID, PROJECT))
      .thenReturn(List.of(new IssueStats("java:1", initialIssueCount, 2, 2, 0, 0)));

    underTest.adjustIssueStats(PROJECT_ID, PROJECT, "java:1", 2, 2, adjustment);

    verify(dao)
      .upsert(PROJECT_ID, PROJECT, new IssueStats("java:1", initialIssueCount + adjustment, 2, 2, 0, 0));
  }
}
