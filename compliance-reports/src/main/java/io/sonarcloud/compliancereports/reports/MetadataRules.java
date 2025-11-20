/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class MetadataRules {
  private final MetadataLoader metadataLoader;

  public MetadataRules(MetadataLoader metadataLoader) {
    this.metadataLoader = metadataLoader;
  }

  @CheckForNull
  public ComplianceCategoryRules getRules(@Nullable Map<ReportKey, String> categoriesByStandard) {
    if (categoriesByStandard == null) {
      return null;
    }

    Map<ReportKey, RuleBuckets> metadata = metadataLoader.getAllMetadata();

    Set<RepositoryRuleKey> repoRuleKeys = new HashSet<>();
    Set<String> ruleKeys = new HashSet<>();

    for (Map.Entry<ReportKey, String> e : categoriesByStandard.entrySet()) {
      RuleBuckets ruleBuckets = metadata.get(e.getKey());
      if (ruleBuckets == null) {
        continue;
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
