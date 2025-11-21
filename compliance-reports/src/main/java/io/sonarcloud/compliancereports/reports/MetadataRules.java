/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.rule.RuleKey;

public class MetadataRules {
  private final MetadataLoader metadataLoader;

  public MetadataRules(MetadataLoader metadataLoader) {
    this.metadataLoader = metadataLoader;
  }

  public ComplianceCategoryRules getRules(Map<ReportKey, String> categoriesByStandard) {
    Map<ReportKey, RuleBuckets> metadata = metadataLoader.getAllMetadata();

    Set<RepositoryRuleKey> repoRuleKeys = new HashSet<>();
    Set<String> ruleKeys = new HashSet<>();

    for (Map.Entry<ReportKey, String> e : categoriesByStandard.entrySet()) {
      RuleBuckets ruleBuckets = metadata.get(e.getKey());
      if (ruleBuckets == null) {
        throw new IllegalStateException("Unknown standard: " + e.getKey());
      }
      ruleBuckets.getBuckets()
        .stream()
        .filter(b -> b.key().equals(e.getValue()))
        .findFirst()
        .ifPresent(ruleBucket -> {
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
    return getRules(Map.of(standard, category));
  }

  public Map<String, Long> getRuleCountByStandardCategory(ReportKey standard, Map<String, Long> countByRuleKey) {
    RuleBuckets standardMetadata = metadataLoader.getAllMetadata().get(standard);
    Map<String, Long> wildcardCountByRuleKey = countByRuleKey.entrySet().stream().collect(Collectors.toMap(
      entry -> RuleKey.parse(entry.getKey()).rule(), Map.Entry::getValue, Long::sum));
    Map<String, Long> ruleCountByCategory = new HashMap<>();

    for (RuleBuckets.RuleBucket ruleBucket : standardMetadata.getBuckets()) {
      long sum = 0L;
      for (String ruleKey : ruleBucket.ruleKeys()) {
        if (ruleKey.startsWith(":")) {
          sum += wildcardCountByRuleKey.getOrDefault(ruleKey.substring(1), 0L);
        } else {
          sum += countByRuleKey.getOrDefault(ruleKey, 0L);
        }
      }
      ruleCountByCategory.put(ruleBucket.key(), sum);
    }

    return ruleCountByCategory;
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

  }
}
