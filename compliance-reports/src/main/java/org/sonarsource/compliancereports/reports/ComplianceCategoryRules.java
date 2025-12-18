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

  // repo wildcards, such as "secrets:"
  private final Map<Integer, Set<String>> reposByLevel = new HashMap<>();
  private final Set<String> allRepos = new HashSet<>();

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
      if (ruleKey.startsWith(":")) {
        // :rule wildcard
        addRuleKey(categoryTreeNode, ruleKey);
      } else if (ruleKey.endsWith(":")) {
        // repo: wildcard
        addRepo(categoryTreeNode, ruleKey);
      } else {
        // repo:rule
        addRepoRuleKey(categoryTreeNode, ruleKey);
      }
    }
    for (CategoryTree.CategoryTreeNode child : categoryTreeNode.children()) {
      addRulesFromNode(child);
    }
  }

  private void addRuleKey(CategoryTree.CategoryTreeNode categoryTreeNode, String ruleKey) {
    addToCollections(allRuleKeys, ruleKeysByLevel, categoryTreeNode, ruleKey.substring(ruleKey.indexOf(":") + 1));
  }

  private void addRepo(CategoryTree.CategoryTreeNode categoryTreeNode, String repo) {
    addToCollections(allRepos, reposByLevel, categoryTreeNode, repo.substring(0, repo.indexOf(":")));
  }

  private void addRepoRuleKey(CategoryTree.CategoryTreeNode categoryTreeNode, String ruleKey) {
    addToCollections(allRepoRuleKeys, repoRuleKeysByLevel, categoryTreeNode, RepositoryRuleKey.of(ruleKey));
  }

  private <T> void addToCollections(Set<T> collection, Map<Integer, Set<T>> collectionByLevel, CategoryTree.CategoryTreeNode node, T item) {
    collection.add(item);

    if (node.levelIndex() != null) {
      for (int level = node.levelIndex(); level < numberOfLevels; level++) {
        collectionByLevel
          .computeIfAbsent(level, k -> new HashSet<>())
          .add(item);
        if (!node.levelsInclusive()) {
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
        reposByLevel.getOrDefault(levelIndex, Set.of()).contains(repoRuleKey.repository()) ||
        repoRuleKeysByLevel.getOrDefault(levelIndex, Set.of()).contains(repoRuleKey);
    }
    return allRuleKeys.contains(repoRuleKey.rule()) || allRepos.contains(repoRuleKey.repository()) || allRepoRuleKeys.contains(repoRuleKey);
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

  public Set<String> allRepos() {
    return allRepos;
  }

  public Map<Integer, Set<RepositoryRuleKey>> getRepoRuleKeysByLevel() {
    return repoRuleKeysByLevel;
  }

  public Map<Integer, Set<String>> getRuleKeysByLevel() {
    return ruleKeysByLevel;
  }

  public Map<Integer, Set<String>> getReposByLevel() {
    return reposByLevel;
  }

  public boolean isEmpty() {
    return allRuleKeys.isEmpty() && allRepoRuleKeys.isEmpty();
  }
}
