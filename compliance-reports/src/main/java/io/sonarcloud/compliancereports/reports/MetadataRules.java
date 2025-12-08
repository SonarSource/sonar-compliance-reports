/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.reports.RuleBuckets.RuleBucket;
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

    for (RuleBucket ruleBucket : metadataLoader.getAllMetadata().get(standard).getBuckets()) {
      ComplianceCategoryRules rules = new ComplianceCategoryRules(new HashSet<>(), new HashSet<>());
      collectRules(ruleBucket, rules);
      rulesPerCategory.put(ruleBucket.key(), rules);
    }
    return rulesPerCategory;
  }

  private void collectRules(RuleBuckets.RuleBucket ruleBucket, ComplianceCategoryRules rules) {
    for (String ruleKey : ruleBucket.ruleKeys()) {
      if (!ruleKey.startsWith(":")) {
        // repo:rule
        rules.repoRuleKeys().add(RepositoryRuleKey.of(ruleKey));
      } else {
        // :rule wildcard
        rules.ruleKeys().add(ruleKey.substring(ruleKey.indexOf(":") + 1));
      }
    }
  }

  /**
   * Get all rules associated with each standard. All categories in each standard are merged together.
   *
   * @param categoriesByStandard specifies the standards to be returned what categories to collect rules from.
   */
  public Map<ReportKey, ComplianceCategoryRules> getRulesByStandard(Map<ReportKey, Set<String>> categoriesByStandard) {
    Map<ReportKey, RuleBuckets> metadata = metadataLoader.getAllMetadata();
    Map<ReportKey, ComplianceCategoryRules> rulesByStandard = new LinkedHashMap<>();

    for (Map.Entry<ReportKey, Set<String>> e : categoriesByStandard.entrySet()) {
      if (!metadata.containsKey(e.getKey())) {
        throw new IllegalArgumentException("Unknown standard: " + e.getKey());
      }
      ComplianceCategoryRules rules = new ComplianceCategoryRules(new HashSet<>(), new HashSet<>());
      for (RuleBucket ruleBucket : metadata.get(e.getKey()).getBuckets()) {
        if (!e.getValue().contains(ruleBucket.key())) {
          continue;
        }
        collectRules(ruleBucket, rules);
      }

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
        if (categoryEntry.getValue().contains(countEntry.getKey())) {
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
      .filter(ruleKey -> rules.stream().allMatch(r -> r.contains(ruleKey)))
      .collect(Collectors.toSet());
  }

  public record RepositoryRuleKey(String repository, String rule) {
    @Override
    public String toString() {
      return repository + ":" + rule;
    }

    public static RepositoryRuleKey of(String ruleKey) {
      int pos = ruleKey.indexOf(':');
      String repo = ruleKey.substring(0, pos);
      String key = ruleKey.substring(pos + 1);
      return new RepositoryRuleKey(repo, key);
    }
  }

  public record ComplianceCategoryRules(
    // fully specified rules, such as "java:S001"
    Set<RepositoryRuleKey> repoRuleKeys,
    // rule wildcards, such as "S001"
    Set<String> ruleKeys
  ) {

    public boolean contains(String ruleKey) {
      RepositoryRuleKey repoRuleKey = RepositoryRuleKey.of(ruleKey);
      return contains(repoRuleKey);
    }

    public boolean contains(RepositoryRuleKey repoRuleKey) {
      return ruleKeys.contains(repoRuleKey.rule()) || repoRuleKeys.contains(repoRuleKey);
    }

    public boolean isEmpty() {
      return ruleKeys.isEmpty() && repoRuleKeys.isEmpty();
    }
  }
}
