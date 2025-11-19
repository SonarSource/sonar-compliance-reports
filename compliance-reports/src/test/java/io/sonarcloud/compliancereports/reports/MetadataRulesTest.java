/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.reports.MetadataRules.ComplianceCategoryRules;
import io.sonarcloud.compliancereports.reports.MetadataRules.RepositoryRuleKey;
import io.sonarcloud.compliancereports.reports.metadata.MetadataType;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataRulesTest {
  private final MetadataLoader metaDataLoader = new MetadataLoader(Set.of(() -> "TestMetadata.yml"));
  private final MetadataRules metadataRules = new MetadataRules(metaDataLoader);


  @Test
  void getRules_returns_rules_and_wildcards() {
    ComplianceCategoryRules rules = metadataRules.getRules(Map.of("testV1", "category1"));
    assertThat(rules.repoRuleKeys()).containsOnly(RepositoryRuleKey.of("java:S001"));
    assertThat(rules.ruleKeys()).containsOnly("2", "3");
  }

  @Test
  void RepositoryRuleKey_parses_repo_and_key() {
    RepositoryRuleKey repositoryRuleKey = RepositoryRuleKey.of("repo:rule");
    assertThat(repositoryRuleKey.repository()).isEqualTo("repo");
    assertThat(repositoryRuleKey.rule()).isEqualTo("rule");
  }

  @Test
  void getRules_returns_empty_if_category_is_unknown() {
    ComplianceCategoryRules rules = metadataRules.getRules(Map.of("testV1", "unknown"));
    assertThat(rules.repoRuleKeys()).isEmpty();
    assertThat(rules.ruleKeys()).isEmpty();
  }

  @Test
  void getRules_returns_null_if_map_is_null() {
    assertThat(metadataRules.getRules(null)).isNull();
  }

}