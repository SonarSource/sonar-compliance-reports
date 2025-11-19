/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssueStatsByRuleKeyDaoTestImpl implements IssueStatsByRuleKeyDao {

  private final Map<String, List<IssueStats>> dataStore = new HashMap<>();

  @Override
  public List<IssueStats> getIssueStats(String aggregationId, AggregationType aggregationType) {
    return dataStore.get(aggregationId);
  }

  @Override
  public void deleteAndInsertIssueStats(String aggregationId, AggregationType aggregationType, List<IssueStats> issueStats) {
    dataStore.remove(aggregationId);
    dataStore.put(aggregationId, issueStats);
  }

  public Map<String, List<IssueStats>> getDataStore() {
    return dataStore;
  }
}
