/*
 * Copyright (C) 2022-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

package io.sonarcloud.compliancereports.dao;

import java.util.List;
import java.util.UUID;

public interface IssueStatsByRuleKeyDao {

  List<IssueStats> getIssueStatsForProject(UUID projectId);

  void insertIssueStatsForProject(UUID projectId, List<IssueStats> issueStats);

  void deleteAllIssueStatsForProject(UUID projectId);
}
