/*
 * Copyright (C) 2022-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.reports.metadata.ReportMetadataSchema;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuleBuckets {
  private final String key;
  private final Set<RuleBucket> buckets;

  public record RuleBucket(String key, Set<String> ruleKeys) {}

  public RuleBuckets(String reportKey, ReportMetadataSchema.Report.Version version) {
    key = reportKey + version.key();

    if (version.categories() != null) {
      buckets = version.categories().stream()
        .flatMap(category -> Stream.concat(Stream.of(category), category.subcategories() != null ? category.subcategories().stream() : Stream.of()))
        .flatMap(category -> Stream.concat(Stream.of(category), category.subsubcategories() != null ? category.subsubcategories().stream() : Stream.of()))
        .map(category -> new RuleBucket(category.name(), category.rules() != null ? category.rules() : Set.of()))
        .collect(Collectors.toSet());
    } else {
      buckets = Set.of();
    }
  }

  public Set<RuleBucket> getBuckets() {
    return buckets;
  }

  public String getKey() {
    return key;
  }
}
