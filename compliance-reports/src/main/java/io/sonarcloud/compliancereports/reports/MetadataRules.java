/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class MetadataRules {
  private final MetadataLoader metadataLoader;

  public MetadataRules(MetadataLoader metadataLoader) {
    this.metadataLoader = metadataLoader;
  }

  public Map<String, ComplianceCategoryRules> getRules(ReportKey standard) {
    Map<String, ComplianceCategoryRules> rulesPerCategory = new HashMap<>();

    for (RuleBuckets.RuleBucket ruleBucket : metadataLoader.getAllMetadata().get(standard).getBuckets()) {
      Set<RepositoryRuleKey> repoRuleKeys = new HashSet<>();
      Set<String> ruleKeys = new HashSet<>();

      for (String ruleKey : ruleBucket.ruleKeys()) {
        if (!ruleKey.startsWith(":")) {
          // repo:rule
          repoRuleKeys.add(RepositoryRuleKey.of(ruleKey));
        } else {
          // :rule wildcard
          ruleKeys.add(ruleKey.substring(ruleKey.indexOf(":") + 1));
        }
      }
      rulesPerCategory.put(ruleBucket.key(), new ComplianceCategoryRules(repoRuleKeys, ruleKeys));
    }
    return rulesPerCategory;
  }

  public ComplianceCategoryRules getRules(Map<ReportKey, Collection<String>> categoriesByStandard) {
    Map<ReportKey, RuleBuckets> metadata = metadataLoader.getAllMetadata();

    Set<RepositoryRuleKey> repoRuleKeys = new HashSet<>();
    Set<String> ruleKeys = new HashSet<>();

    for (Map.Entry<ReportKey, Collection<String>> e : categoriesByStandard.entrySet()) {
      RuleBuckets ruleBuckets = metadata.get(e.getKey());
      if (ruleBuckets == null) {
        throw new IllegalStateException("Unknown standard: " + e.getKey());
      }
      ruleBuckets.getBuckets()
        .stream()
        .filter(ruleBucket -> e.getValue().contains(ruleBucket.key()))
        .forEach(ruleBucket -> {
          for (String ruleKey : ruleBucket.ruleKeys()) {
            if (!ruleKey.startsWith(":")) {
              // repo:rule
              repoRuleKeys.add(RepositoryRuleKey.of(ruleKey));
            } else {
              // :rule wildcard
              ruleKeys.add(ruleKey.substring(ruleKey.indexOf(":") + 1));
            }
          }
        });
    }

    return new ComplianceCategoryRules(repoRuleKeys, ruleKeys);
  }

  public ComplianceCategoryRules getRules(ReportKey standard, String category) {
    return getRules(Map.of(standard, List.of(category)));
  }

  public Map<String, Long> getRuleCountByStandardCategory(ReportKey standard, Map<String, Long> countByRuleKey) {
    Map<String, ComplianceCategoryRules> rulesByCategory = getRules(standard);
    Map<String, Long> ruleCountByCategory = new HashMap<>();

    for (Map.Entry<String, ComplianceCategoryRules> categoryEntry : rulesByCategory.entrySet()) {
      long sum = 0L;
      for (Map.Entry<String, Long> countEntry : countByRuleKey.entrySet()) {
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
  public Set<String> applyComplianceFiltersToFacet(Set<String> ruleKeys, ReportKey reportKey, @Nullable Map<ReportKey, String> filters) {
    if (filters == null) {
      return ruleKeys;
    }
    Map<ReportKey, String> activeFilters = filters.entrySet().stream()
      .filter(f -> !f.getKey().equals(reportKey))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return ruleKeys.stream()
      .filter(ruleKey -> filtersIncludeRule(activeFilters, ruleKey))
      .collect(Collectors.toSet());
  }

  public boolean filtersIncludeRule(Map<ReportKey, String> filters, String ruleKey) {
    for (Map.Entry<ReportKey, String> filter : filters.entrySet()) {
      ComplianceCategoryRules categoryRules = getRules(filter.getKey(), filter.getValue());
      if (!categoryRules.contains(ruleKey)) {
        return false;
      }
    }
    return true;
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
    Collection<RepositoryRuleKey> repoRuleKeys,
    // rule wildcards, such as "S001"
    Collection<String> ruleKeys
  ) {

    public boolean contains(String ruleKey) {
      RepositoryRuleKey repoRuleKey = RepositoryRuleKey.of(ruleKey);
      return ruleKeys.contains(repoRuleKey.rule()) || repoRuleKeys.contains(repoRuleKey);
    }
  }
}
