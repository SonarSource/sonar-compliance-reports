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
package org.sonarsource.compliancereports.dao;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IssueStatsByRuleKeyDaoTestImpl implements IssueStatsByRuleKeyDao {

  private final Map<String, List<IssueStats>> dataStore = new HashMap<>();

  @Override
  public List<IssueStats> getIssueStats(String aggregationId, AggregationType aggregationType) {
    return dataStore.get(aggregationId);
  }

  @Override
  public Map<String, List<IssueStats>> getIssueStatsByAggregationIds(Collection<String> aggregationIds, AggregationType aggregationType) {
    return dataStore.entrySet()
      .stream()
      .filter(e -> aggregationIds.contains(e.getKey()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public void deleteAndInsertIssueStats(String aggregationId, AggregationType aggregationType, List<IssueStats> issueStats) {
    dataStore.remove(aggregationId);
    dataStore.put(aggregationId, issueStats);
  }

  @Override
  public IssueStats aggregateIssueStatsForBranchUuidAndRuleKey(String aggregationId, String ruleKey) {
    return dataStore.get(aggregationId).stream()
      .filter(stats -> stats.ruleKey().equals(ruleKey))
      .findFirst()
      .orElse(null);
  }

  @Override
  public void upsert(String aggregationId, AggregationType aggregationType, IssueStats updatedIssueStats) {
    dataStore.get(aggregationId)
      .replaceAll(stats -> stats.ruleKey().equals(updatedIssueStats.ruleKey()) ? updatedIssueStats : stats);
  }

  @Override
  public void deleteByAggregationAndRuleKey(String aggregationId, AggregationType aggregationType, String ruleKey) {
    dataStore.get(aggregationId).removeIf(stats -> stats.ruleKey().equals(ruleKey));
  }

  public Map<String, List<IssueStats>> getDataStore() {
    return dataStore;
  }
}
