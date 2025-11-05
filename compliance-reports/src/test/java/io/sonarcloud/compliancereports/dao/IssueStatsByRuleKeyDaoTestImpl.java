/*
 * Copyright (C) 2022-2025 SonarSource SA
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
  public List<IssueStats> getIssueStatsForProject(UUID projectId) {
    return dataStore.get(projectId);
  }

  @Override
  public void insertIssueStatsForProject(UUID projectId, List<IssueStats> issueStats) {
    dataStore.put(projectId, issueStats);
  }

  @Override
  public void deleteAllIssueStatsForProject(UUID projectId) {
    dataStore.remove(projectId);
  }

  public Map<UUID, List<IssueStats>> getDataStore() {
    return dataStore;
  }
}
