/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.dao;

import java.util.List;
import java.util.UUID;

public interface IssueStatsByRuleKeyDao {

  List<IssueStats> getIssueStats(String aggregationId, AggregationType aggregationType);

  void insertIssueStats(String aggregationId, AggregationType aggregationType, List<IssueStats> issueStats);

  void deleteAllIssueStats(String aggregationId, AggregationType aggregationType);
}
