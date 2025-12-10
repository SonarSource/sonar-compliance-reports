/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class MetadataRules {
  private final MetadataLoader metadataLoader;

  public MetadataRules(MetadataLoader metadataLoader) {
    this.metadataLoader = metadataLoader;
  }

  /**
   * Get rules for each category in the standard.
   * If a category has no rules associated with it, it's still returned in the map
   */
  public Map<String, ComplianceCategoryRules> getRulesByCategory(ReportKey standard) {
    Map<String, ComplianceCategoryRules> rulesPerCategory = new LinkedHashMap<>();

    for (CategoryTree.CategoryTreeNode categoryTreeNode : metadataLoader.getAllMetadata().get(standard).getChildren()) {
      putComplianceCategoryRulesIntoMap(categoryTreeNode, rulesPerCategory);
    }
    return rulesPerCategory;
  }

  private void putComplianceCategoryRulesIntoMap(CategoryTree.CategoryTreeNode categoryTreeNode, Map<String, ComplianceCategoryRules> rulesPerCategory) {
    var parent = new ComplianceCategoryRules(categoryTreeNode);
    rulesPerCategory.put(categoryTreeNode.key(), parent);
    for (var child : categoryTreeNode.children()) {
      putComplianceCategoryRulesIntoMap(child, parent.getChildren());
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

      var filteredCategories = metadata.get(reportKey).getChildren().stream()
        .filter(cat -> categories.contains(cat.key()))
        .collect(Collectors.toSet());
      ComplianceCategoryRules rules = new ComplianceCategoryRules(filteredCategories);
      rulesByStandard.put(e.getKey(), rules);
    }

    return rulesByStandard;
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
