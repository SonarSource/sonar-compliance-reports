/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IssueStatsByRuleKeyDaoTestImpl implements IssueStatsByRuleKeyDao {

  private final Map<UUID, List<IssueStats>> dataStore = new HashMap<>();

  @Override
  public List<IssueStats> getIssueStats(UUID aggregationId, AggregationType aggregationType) {
    return dataStore.get(aggregationId);
  }

  @Override
  public void insertIssueStats(UUID aggregationId, AggregationType aggregationType, List<IssueStats> issueStats) {
    dataStore.put(aggregationId, issueStats);
  }

  @Override
  public void deleteAllIssueStats(UUID aggregationId, AggregationType aggregationType) {
    dataStore.remove(aggregationId);
  }

  public Map<UUID, List<IssueStats>> getDataStore() {
    return dataStore;
  }
}
