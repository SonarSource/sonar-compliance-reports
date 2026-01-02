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

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonarsource.compliancereports.reports.RepositoryRuleKey.of;

class MetadataRulesTest {
  private final MetadataLoader metaDataLoader = new MetadataLoader(Set.of(
    () -> "TestMetadata.yml", () -> "TestMetadata2.yml", () -> "MetadataWithInclusiveLevels.yml", () -> "TestMetadataWithWildcards.yml"));
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
    assertThat(rules.get(reportKey).allRepoRuleKeys()).containsOnly(RepositoryRuleKey.of("java:S001"));
    assertThat(rules.get(reportKey).allRuleKeys()).containsOnly("2", "3");
  }

  @Test
  void getRulesByStandard_returns_rules_and_wildcards_for_multiple_categories() {
    ReportKey reportKey = new ReportKey("test", "V1");
    Map<ReportKey, ComplianceCategoryRules> rules = metadataRules.getRulesByStandard(Map.of(reportKey, Set.of("category1", "category2")));
    assertThat(rules.get(reportKey).allRepoRuleKeys()).containsOnly(RepositoryRuleKey.of("java:S001"));
    assertThat(rules.get(reportKey).allRuleKeys()).containsOnly("1", "2", "3");
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

    assertThat(rules.get(reportKey1).allRepoRuleKeys()).containsOnly(RepositoryRuleKey.of("java:S001"));
    assertThat(rules.get(reportKey1).allRuleKeys()).containsOnly("1", "2", "3");

    assertThat(rules.get(reportKey2).allRepoRuleKeys()).containsOnly(RepositoryRuleKey.of("java:S001"));
    assertThat(rules.get(reportKey2).allRuleKeys()).isEmpty();
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
  void getRuleCountByStandardCategory_return_count_for_wildcard_repo() {
    ReportKey reportKey = new ReportKey("test-wildcard-repos", "WithWildcardReposVersion1");
    Map<String, Long> ruleCountByStandardCategory = metadataRules.getRuleCountByStandardCategory(reportKey, Map.of("secrets:Y1", 5L, "java:S001", 2L));
    assertThat(ruleCountByStandardCategory).containsOnly(entry("category1withsecretrules", 5L));
  }

  @Test
  void getRuleCountByStandardCategory_return_zero_with_no_wildcards_at_all() {
    ReportKey reportKey = new ReportKey("test-wildcard-repos", "WithNoWildcardsVersion2");
    Map<String, Long> ruleCountByStandardCategory = metadataRules.getRuleCountByStandardCategory(reportKey, Map.of());
    assertThat(ruleCountByStandardCategory).containsOnly(entry("category2withnowildcards", 0L));
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

  @Test
  void nested_categories_should_aggregate_all_rules_from_children() {
    var complianceCategoryRulesMap = metadataRules.getRulesByCategory(new ReportKey("levels-test", "A"));
    var cat1Rules = complianceCategoryRulesMap.get("cat1");

    assertThat(cat1Rules.allRepoRuleKeys()).containsOnly(
      of("java:1"), of("java:2"), of("java:3"), of("java:6"), of("java:9"), of("java:21"),
      of("java:22"), of("java:23"), of("java:49"), of("java:99"), of("java:100"), of("java:101"),
      of("java:102"), of("java:111"), of("java:222"), of("java:333"));
    assertThat(cat1Rules.allRuleKeys()).containsOnly("14", "41", "64", "62", "1", "11", "12", "13");
    assertThat(cat1Rules.allRepos()).containsExactly("secrets");
  }

  @Test
  void nested_categories_should_aggregate_rules_by_level() {
    var complianceCategoryRulesMap = metadataRules.getRulesByCategory(new ReportKey("levels-test", "A"));
    var cat1Rules = complianceCategoryRulesMap.get("cat1");

    assertThat(cat1Rules.getRepoRuleKeysByLevel())
      .hasEntrySatisfying(0, set -> assertThat(set).containsExactlyInAnyOrder(
        of("java:1"), of("java:2"), of("java:3"), of("java:21"), of("java:22"), of("java:23"),
        of("java:49"), of("java:99"), of("java:100"), of("java:101"), of("java:102")))
      .hasEntrySatisfying(1, set -> assertThat(set).containsExactlyInAnyOrder(
        of("java:1"), of("java:2"), of("java:3"), of("java:21"), of("java:22"), of("java:23"),
        of("java:49"), of("java:99"), of("java:100"), of("java:101"), of("java:102"), of("java:6"), of("java:9")))
      .hasEntrySatisfying(2, set -> assertThat(set).containsExactlyInAnyOrder(
        of("java:1"), of("java:2"), of("java:3"), of("java:21"), of("java:22"), of("java:23"),
        of("java:49"), of("java:99"), of("java:100"), of("java:101"), of("java:102"), of("java:6"),
        of("java:9"), of("java:111"), of("java:222"), of("java:333")));

    assertThat(cat1Rules.getRuleKeysByLevel())
      .hasEntrySatisfying(0, set -> assertThat(set).containsExactlyInAnyOrder("62"))
      .hasEntrySatisfying(1, set -> assertThat(set).containsExactlyInAnyOrder("62", "1"))
      .hasEntrySatisfying(2, set -> assertThat(set).containsExactlyInAnyOrder("62", "1", "14", "41", "64"));

    assertThat(cat1Rules.getReposByLevel())
      .hasEntrySatisfying(1, set -> assertThat(set).containsExactly("secrets"))
      .hasEntrySatisfying(2, set -> assertThat(set).containsExactly("secrets"));
  }

  @Test
  void nested_subcategory_should_contain_only_its_rules() {
    var complianceCategoryRulesMap = metadataRules.getRulesByCategory(new ReportKey("levels-test", "A"));
    var cat12Rules = complianceCategoryRulesMap.get("cat1").getChildren().get("cat1.2");

    assertThat(cat12Rules.allRepoRuleKeys()).containsOnly(
      of("java:21"), of("java:22"), of("java:23"), of("java:49"), of("java:99"));
    assertThat(cat12Rules.allRuleKeys()).containsOnly("62", "1");

    assertThat(cat12Rules.getRepoRuleKeysByLevel())
      .hasEntrySatisfying(0, set -> assertThat(set).containsExactlyInAnyOrder(
        of("java:21"), of("java:22"), of("java:23"), of("java:49"), of("java:99")))
      .hasEntrySatisfying(1, set -> assertThat(set).containsExactlyInAnyOrder(
        of("java:21"), of("java:22"), of("java:23"), of("java:49"), of("java:99")))
      .hasEntrySatisfying(2, set -> assertThat(set).containsExactlyInAnyOrder(
        of("java:21"), of("java:22"), of("java:23"), of("java:49"), of("java:99")));

    assertThat(cat12Rules.getRuleKeysByLevel())
      .hasEntrySatisfying(0, set -> assertThat(set).containsExactlyInAnyOrder("62"))
      .hasEntrySatisfying(1, set -> assertThat(set).containsExactlyInAnyOrder("62", "1"))
      .hasEntrySatisfying(2, set -> assertThat(set).containsExactlyInAnyOrder("62", "1"));

    assertThat(cat12Rules.getReposByLevel()).isEmpty();
  }

  @Test
  void deeply_nested_category_should_handle_empty_repo_rules() {
    var complianceCategoryRulesMap = metadataRules.getRulesByCategory(new ReportKey("levels-test", "A"));
    var cat133Rules = complianceCategoryRulesMap.get("cat1").getChildren().get("cat1.3").getChildren().get("cat1.3.3");

    assertThat(cat133Rules.allRepoRuleKeys()).isEmpty();
    assertThat(cat133Rules.allRuleKeys()).containsOnly("11", "12", "13");
    assertThat(cat133Rules.getRepoRuleKeysByLevel()).isEmpty();
    assertThat(cat133Rules.getRuleKeysByLevel()).isEmpty();
    assertThat(cat133Rules.getReposByLevel()).isEmpty();
  }

  @Test
  void checking_for_an_invalid_level_should_return_false() {
    ReportKey reportKey = new ReportKey("levels-test", "A");
    var complianceCategoryRulesMap = metadataRules.getRulesByCategory(reportKey);
    var cat1Rules = complianceCategoryRulesMap.get("cat1");

    assertThat(cat1Rules.containsRuleAtLevel("java:1", -1)).isFalse();
    assertThat(cat1Rules.containsRuleAtLevel("java:1", 5)).isFalse();
  }
}
