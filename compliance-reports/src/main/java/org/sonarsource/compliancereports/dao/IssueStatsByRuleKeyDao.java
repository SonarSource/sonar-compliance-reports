/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonarsource.compliancereports.dao;

import java.util.List;

public interface IssueStatsByRuleKeyDao {

  List<IssueStats> getIssueStats(String aggregationId, AggregationType aggregationType);

  void deleteAndInsertIssueStats(String aggregationId, AggregationType aggregationType, List<IssueStats> issueStats);
}
