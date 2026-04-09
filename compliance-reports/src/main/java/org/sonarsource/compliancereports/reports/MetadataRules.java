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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonarsource.compliancereports.reports.CategoryTree.CategoryTreeNode;

import static org.sonarsource.compliancereports.reports.ComplianceCategoryRules.getCategoryNameToRulesInOrder;

public class MetadataRules {
  private final MetadataLoader metadataLoader;
  private final Map<ReportKey, Map<String, ComplianceCategoryRules>> rulesByCategoryCache = new ConcurrentHashMap<>();

  public MetadataRules(MetadataLoader metadataLoader) {
    this.metadataLoader = metadataLoader;
  }

  /**
   * Get rules for each category in the standard.
   * If a category has no rules associated with it, it's still returned in the map
   */
  public Map<String, ComplianceCategoryRules> getRulesByCategory(ReportKey standard) {
    return rulesByCategoryCache.computeIfAbsent(standard, this::computeRulesByCategory);
  }

  private Map<String, ComplianceCategoryRules> computeRulesByCategory(ReportKey standard) {
    Map<CategoryTreeNode, ComplianceCategoryRules> rulesPerCategory = new TreeMap<>(CategoryTree::categoryCompareTo);

    for (CategoryTreeNode categoryTreeNode : metadataLoader.getAllMetadata().get(standard).getChildren()) {
      putComplianceCategoryRulesIntoMap(categoryTreeNode, rulesPerCategory);
    }
    return getCategoryNameToRulesInOrder(rulesPerCategory);
  }

  private static void putComplianceCategoryRulesIntoMap(CategoryTreeNode categoryTreeNode, Map<CategoryTreeNode, ComplianceCategoryRules> rulesPerCategory) {
    var parent = new ComplianceCategoryRules(categoryTreeNode);
    rulesPerCategory.put(categoryTreeNode, parent);
    for (var child : categoryTreeNode.children()) {
      putComplianceCategoryRulesIntoMap(child, parent.getChildrenByNode());
    }
  }

  /**
   * Get all rules associated with each standard. All categories in each standard are merged together.
   *
   * @param categoriesByStandard specifies the standards to be returned what categories to collect rules from.
   */
  public Map<ReportKey, ComplianceCategoryRules> getRulesByStandard(Map<ReportKey, Set<String>> categoriesByStandard) {
    Map<ReportKey, CategoryTree> metadata = metadataLoader.getAllMetadata();
    Map<ReportKey, ComplianceCategoryRules> rulesByStandard = new LinkedHashMap<>();

    for (Map.Entry<ReportKey, Set<String>> e : categoriesByStandard.entrySet()) {
      ReportKey reportKey = e.getKey();
      Set<String> categories = e.getValue();
      if (!metadata.containsKey(reportKey)) {
        throw new IllegalArgumentException("Unknown standard: " + reportKey);
      }

      var filteredCategories = flattenCategories(categories, metadata.get(reportKey).getChildren(), new HashSet<>());
      ComplianceCategoryRules rules = new ComplianceCategoryRules(filteredCategories);
      rulesByStandard.put(e.getKey(), rules);
    }

    return rulesByStandard;
  }

  private static Set<CategoryTreeNode> flattenCategories(Set<String> categories, Set<CategoryTreeNode> nodes,
    Set<CategoryTreeNode> accumulator) {
    for (var node : nodes) {
      if (categories.contains(node.key())) {
        accumulator.add(node);
      }
      flattenCategories(categories, node.children(), accumulator);
    }
    return accumulator;
  }

  public Map<String, Long> getRuleCountByStandardCategory(ReportKey standard, Map<String, Long> countByRuleKey) {
    Map<String, ComplianceCategoryRules> rulesByCategory = getRulesByCategory(standard);
    Map<String, Long> ruleCountByCategory = new HashMap<>();
    Map<RepositoryRuleKey, Long> countByRepoRuleKey = countByRuleKey.entrySet().stream().collect(Collectors.toMap(
      e -> RepositoryRuleKey.of(e.getKey()), Map.Entry::getValue));

    for (Map.Entry<String, ComplianceCategoryRules> categoryEntry : rulesByCategory.entrySet()) {
      if (categoryEntry.getValue().isEmpty()) {
        ruleCountByCategory.put(categoryEntry.getKey(), 0L);
        continue;
      }
      long sum = 0L;
      for (Map.Entry<RepositoryRuleKey, Long> countEntry : countByRepoRuleKey.entrySet()) {
        if (categoryEntry.getValue().containsRuleAtLevel(countEntry.getKey(), null)) {
          sum += countEntry.getValue();
        }
      }
      ruleCountByCategory.put(categoryEntry.getKey(), sum);
    }

    return ruleCountByCategory;
  }

  public CategoryTree getCategoryTree(ReportKey reportKey) {
    CategoryTree tree = metadataLoader.getAllMetadata().get(reportKey);
    if (tree == null) {
      throw new IllegalArgumentException("Unknown standard: " + reportKey);
    }
    return tree;
  }

  public Map<ReportKey, CategoryTree> getCategoryTrees(Collection<ReportKey> reportKeys) {
    Map<ReportKey, CategoryTree> results = new HashMap<>();
    for (ReportKey key : reportKeys) {
      results.put(key, getCategoryTree(key));
    }
    return results;
  }

  /**
   * Exclude rule keys that are being filtered out by filters on other compliance standards
   */
  public Set<String> applyComplianceFiltersToFacet(Set<String> ruleKeys, ReportKey reportKey,
    @Nullable Map<ReportKey, Set<String>> filters) {
    if (filters == null) {
      return ruleKeys;
    }
    Map<ReportKey, Set<String>> activeFilters = filters.entrySet().stream()
      .filter(f -> !f.getKey().equals(reportKey))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Collection<ComplianceCategoryRules> rules = getRulesByStandard(activeFilters).values();

    return ruleKeys.stream()
      .filter(ruleKey -> rules.stream().allMatch(r -> r.containsRuleAtLevel(ruleKey, null)))
      .collect(Collectors.toSet());
  }
}
