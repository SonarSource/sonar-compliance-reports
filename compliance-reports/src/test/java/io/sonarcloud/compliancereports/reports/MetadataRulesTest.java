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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetadataRulesTest {
  private final MetadataLoader metaDataLoader = new MetadataLoader(Set.of(
    () -> "TestMetadata.yml", () -> "TestMetadata2.yml"));
  private final MetadataRules metadataRules = new MetadataRules(metaDataLoader);

  @Test
  void getRulesByStandard_throws_IAE_if_standard_unknown() {
    Map<ReportKey, Set<String>> map = Map.of(new ReportKey("unknown", "V1"), Set.of("cat1"));
    assertThatThrownBy(() -> metadataRules.getRulesByStandard(map)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getRulesByStandard_returns_rules_and_wildcards() {
    ReportKey reportKey = new ReportKey("test", "V1");
    Map<ReportKey, ComplianceCategoryRules> rules = metadataRules.getRulesByStandard(Map.of(reportKey, Set.of("category1")));
    assertThat(rules).containsOnlyKeys(reportKey);
    assertThat(rules.get(reportKey).repoRuleKeys()).containsOnly(RepositoryRuleKey.of("java:S001"));
    assertThat(rules.get(reportKey).ruleKeys()).containsOnly("2", "3");
  }

  @Test
  void getRulesByStandard_returns_rules_and_wildcards_for_multiple_categories() {
    ReportKey reportKey = new ReportKey("test", "V1");
    Map<ReportKey, ComplianceCategoryRules> rules = metadataRules.getRulesByStandard(Map.of(reportKey, Set.of("category1", "category2")));
    assertThat(rules.get(reportKey).repoRuleKeys()).containsOnly(RepositoryRuleKey.of("java:S001"));
    assertThat(rules.get(reportKey).ruleKeys()).containsOnly("1", "2", "3");
  }

  @Test
  void getRulesByStandard_returns_rules_and_wildcards_for_multiple_standards() {
    ReportKey reportKey1 = new ReportKey("test", "V1");
    ReportKey reportKey2 = new ReportKey("test", "V2");

    Map<ReportKey, ComplianceCategoryRules> rules = metadataRules.getRulesByStandard(Map.of(
      reportKey1, Set.of("category1", "category2"),
      reportKey2, Set.of("cat1", "non existent")
    ));

    assertThat(rules).containsOnlyKeys(reportKey1, reportKey2);

    assertThat(rules.get(reportKey1).repoRuleKeys()).containsOnly(RepositoryRuleKey.of("java:S001"));
    assertThat(rules.get(reportKey1).ruleKeys()).containsOnly("1", "2", "3");

    assertThat(rules.get(reportKey2).repoRuleKeys()).containsOnly(RepositoryRuleKey.of("java:S001"));
    assertThat(rules.get(reportKey2).ruleKeys()).isEmpty();
  }

  @Test
  void RepositoryRuleKey_parses_repo_and_key() {
    RepositoryRuleKey repositoryRuleKey = RepositoryRuleKey.of("repo:rule");
    assertThat(repositoryRuleKey.repository()).isEqualTo("repo");
    assertThat(repositoryRuleKey.rule()).isEqualTo("rule");
  }

  @Test
  void getRulesByStandard_returns_empty_if_category_is_unknown() {
    ReportKey reportKey = new ReportKey("test", "V1");
    Map<ReportKey, ComplianceCategoryRules> rules = metadataRules.getRulesByStandard(Map.of(reportKey, Set.of("unknown")));
    assertThat(rules.get(reportKey).isEmpty()).isTrue();
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

    Set<String> ruleKeys = Set.of("java:S001", "java:2", "java:3");

    Map<ReportKey, Set<String>> filters = Map.of(
      // filter based on all categories
      reportKey2, Set.of("cat1", "cat2"),
      // filter on reportKey1 should have no effect
      reportKey1, Set.of("category2")
    );
    Set<String> filteredRuleKeys = metadataRules.applyComplianceFiltersToFacet(ruleKeys, reportKey1, filters);
    assertThat(filteredRuleKeys).containsOnly("java:S001", "java:3");
  }
}