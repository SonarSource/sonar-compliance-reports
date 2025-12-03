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

  public Map<String, ComplianceCategoryRules> getRules(ReportKey standard) {
    Map<String, ComplianceCategoryRules> rulesPerCategory = new LinkedHashMap<>();

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

  public ComplianceCategoryRules getRules(ReportKey standard, Collection<String> categories) {
    return getRules(Map.of(standard, categories));
  }

  public Map<String, Long> getRuleCountByStandardCategory(ReportKey standard, Map<String, Long> countByRuleKey) {
    Map<String, ComplianceCategoryRules> rulesByCategory = getRules(standard);
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
    @Nullable Map<ReportKey, Collection<String>> filters) {
    if (filters == null) {
      return ruleKeys;
    }
    Map<ReportKey, Collection<String>> activeFilters = filters.entrySet().stream()
      .filter(f -> !f.getKey().equals(reportKey))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return ruleKeys.stream()
      .filter(ruleKey -> filtersIncludeRule(activeFilters, ruleKey))
      .collect(Collectors.toSet());
  }

  public boolean filtersIncludeRule(Map<ReportKey, Collection<String>> filters, String ruleKey) {
    for (Map.Entry<ReportKey, Collection<String>> filter : filters.entrySet()) {
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
