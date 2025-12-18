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
package org.sonarsource.compliancereports.reports;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Takes CategoryTreeNodes and recursively aggregates their rule keys
 */
public class ComplianceCategoryRules {
  // fully specified rules, such as "java:S001"
  private final Map<Integer, Set<RepositoryRuleKey>> repoRuleKeysByLevel = new HashMap<>();
  private final Set<RepositoryRuleKey> allRepoRuleKeys = new HashSet<>();

  // rule wildcards, such as "S001"
  private final Map<Integer, Set<String>> ruleKeysByLevel = new HashMap<>();
  private final Set<String> allRuleKeys = new HashSet<>();

  private final Map<String, ComplianceCategoryRules> children = new HashMap<>();
  private final int numberOfLevels;

  public ComplianceCategoryRules(CategoryTree.CategoryTreeNode categoryTreeNode) {
    this.numberOfLevels = categoryTreeNode.numberOfLevels();
    addRulesFromNode(categoryTreeNode);
  }

  public ComplianceCategoryRules(Set<CategoryTree.CategoryTreeNode> categoryTreeNodes) {
    this.numberOfLevels = categoryTreeNodes.stream()
      .findFirst()
      .map(CategoryTree.CategoryTreeNode::numberOfLevels)
      .orElse(0);
    for (CategoryTree.CategoryTreeNode categoryTreeNode : categoryTreeNodes) {
      addRulesFromNode(categoryTreeNode);
    }
  }

  private void addRulesFromNode(CategoryTree.CategoryTreeNode categoryTreeNode) {
    for (String ruleKey : categoryTreeNode.ruleKeys()) {
      if (!ruleKey.startsWith(":")) {
        // repo:rule
        addRepoRuleKey(categoryTreeNode, ruleKey);
      } else {
        // :rule wildcard
        addRuleKey(categoryTreeNode, ruleKey);
      }
    }
    for (CategoryTree.CategoryTreeNode child : categoryTreeNode.children()) {
      addRulesFromNode(child);
    }
  }

  private void addRuleKey(CategoryTree.CategoryTreeNode categoryTreeNode, String ruleKey) {
    String parsedRuleKey = ruleKey.substring(ruleKey.indexOf(":") + 1);
    allRuleKeys.add(parsedRuleKey);

    if (categoryTreeNode.levelIndex() != null) {
      for (int level = categoryTreeNode.levelIndex(); level < numberOfLevels; level++) {
        ruleKeysByLevel
          .computeIfAbsent(level, k -> new HashSet<>())
          .add(parsedRuleKey);
        if (!categoryTreeNode.levelsInclusive()) {
          break;
        }
      }
    }
  }

  private void addRepoRuleKey(CategoryTree.CategoryTreeNode categoryTreeNode, String ruleKey) {
    allRepoRuleKeys.add(RepositoryRuleKey.of(ruleKey));

    if (categoryTreeNode.levelIndex() != null) {
      for (int level = categoryTreeNode.levelIndex(); level < numberOfLevels; level++) {
        repoRuleKeysByLevel
          .computeIfAbsent(level, k -> new HashSet<>())
          .add(RepositoryRuleKey.of(ruleKey));
        if (!categoryTreeNode.levelsInclusive()) {
          break;
        }
      }
    }
  }

  public boolean containsRuleAtLevel(String ruleKey, @Nullable Integer levelIndex) {
    RepositoryRuleKey repoRuleKey = RepositoryRuleKey.of(ruleKey);
    return containsRuleAtLevel(repoRuleKey, levelIndex);
  }

  public boolean containsRuleAtLevel(RepositoryRuleKey repoRuleKey, @Nullable Integer levelIndex) {
    if (levelIndex != null) {
      return ruleKeysByLevel.getOrDefault(levelIndex, Set.of()).contains(repoRuleKey.rule()) ||
        repoRuleKeysByLevel.getOrDefault(levelIndex, Set.of()).contains(repoRuleKey);
    }
    return allRuleKeys.contains(repoRuleKey.rule()) || allRepoRuleKeys.contains(repoRuleKey);
  }

  public Map<String, ComplianceCategoryRules> getChildren() {
    return children;
  }

  public Set<RepositoryRuleKey> allRepoRuleKeys() {
    return allRepoRuleKeys;
  }

  public Set<String> allRuleKeys() {
    return allRuleKeys;
  }

  public Map<Integer, Set<RepositoryRuleKey>> getRepoRuleKeysByLevel() {
    return repoRuleKeysByLevel;
  }

  public Map<Integer, Set<String>> getRuleKeysByLevel() {
    return ruleKeysByLevel;
  }

  public boolean isEmpty() {
    return allRuleKeys.isEmpty() && allRepoRuleKeys.isEmpty();
  }
}
