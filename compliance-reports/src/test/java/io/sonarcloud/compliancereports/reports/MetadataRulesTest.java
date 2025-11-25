/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.reports.MetadataRules.ComplianceCategoryRules;
import io.sonarcloud.compliancereports.reports.MetadataRules.RepositoryRuleKey;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

class MetadataRulesTest {
  private final MetadataLoader metaDataLoader = new MetadataLoader(Set.of(
    () -> "TestMetadata.yml", () -> "TestMetadata2.yml"));
  private final MetadataRules metadataRules = new MetadataRules(metaDataLoader);


  @Test
  void getRules_returns_rules_and_wildcards() {
    ComplianceCategoryRules rules = metadataRules.getRules(Map.of(new ReportKey("test", "V1"), Set.of("category1")));
    assertThat(rules.repoRuleKeys()).containsOnly(RepositoryRuleKey.of("java:S001"));
    assertThat(rules.ruleKeys()).containsOnly("2", "3");
  }

  @Test
  void getRules_returns_rules_and_wildcards_for_multiple_categories() {
    ComplianceCategoryRules rules = metadataRules.getRules(Map.of(new ReportKey("test", "V1"), Set.of("category1", "category2")));
    assertThat(rules.repoRuleKeys()).containsOnly(RepositoryRuleKey.of("java:S001"));
    assertThat(rules.ruleKeys()).containsOnly("1", "2", "3");
  }

  @Test
  void RepositoryRuleKey_parses_repo_and_key() {
    RepositoryRuleKey repositoryRuleKey = RepositoryRuleKey.of("repo:rule");
    assertThat(repositoryRuleKey.repository()).isEqualTo("repo");
    assertThat(repositoryRuleKey.rule()).isEqualTo("rule");
  }

  @Test
  void getRules_returns_empty_if_category_is_unknown() {
    ComplianceCategoryRules rules = metadataRules.getRules(Map.of(new ReportKey("test", "V1"), Set.of("unknown")));
    assertThat(rules.repoRuleKeys()).isEmpty();
    assertThat(rules.ruleKeys()).isEmpty();
  }

  @Test
  void getRules_for_single_standard() {
    ComplianceCategoryRules rules = metadataRules.getRules(new ReportKey("test", "V1"), "category1");
    assertThat(rules.repoRuleKeys()).containsOnly(RepositoryRuleKey.of("java:S001"));
    assertThat(rules.ruleKeys()).containsOnly("2", "3");
  }

  @Test
  void getRuleCountByStandardCategory_return_count() {
    Map<String, Long> countByRuleKey = Map.of("java:S001", 2L, "php:S001", 3L, "js:1", 4L, "php:1", 2L);
    Map<String, Long> countByCategory = metadataRules.getRuleCountByStandardCategory(new ReportKey("test", "V1"), countByRuleKey);
    assertThat(countByCategory).containsOnly(
      entry("category1", 2L), entry("category2", 6L), entry("category3", 6L)
    );
  }

  @Test
  void applyComplianceFiltersToFacet_applies_other_filters() {
    ReportKey reportKey1 = new ReportKey("test", "V1");
    ReportKey reportKey2 = new ReportKey("test", "V2");

    Set<String> ruleKeys = Set.of("java:S001", "java:2");

    // filter on reportKey1 should have no effect
    Map<ReportKey, String> filters = Map.of(
      reportKey2, "cat1",
      reportKey1, "category2"
    );
    Set<String> filteredRuleKeys = metadataRules.applyComplianceFiltersToFacet(ruleKeys, reportKey1, filters);
    assertThat(filteredRuleKeys).containsOnly("java:S001");
  }
}