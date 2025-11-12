/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports.metadata;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

public record ReportMetadataSchema(Report report) {

  public record Report(
    String name,
    // Assuming "key" will be present in the schema
    String key,
    @Nullable String description,
    @Nullable ReportClassification classification,
    @Nullable List<ReportUrl> urls,
    Taxonomy taxonomy,
    @Nullable Levels levels,
    List<Version> versions) {

    public enum ReportClassification {
      ACCESSIBILITY,
      SECURITY
    }

    public record ReportUrl(
      @Nullable String name,
      String url
    ) {}

    public record Taxonomy(
      String category,
      @Nullable String subcategory,
      @Nullable String subsubcategory
    ) {}

    public record Levels(
      String name,
      String description,
      boolean inclusive,
      List<LevelValue> values
    ) {

      public record LevelValue(
        String name,
        String description
      ) {}
    }

    public record Version(
      String name,
      // Assuming "key" will be present in the schema
      String key,
      @Nullable String description,
      @Nullable List<ReportUrl> urls,
      @Nullable List<Category> categories
    ) {}

    public record Category(
      String name,
      @Nullable String description,
      @Nullable String level,
      @Nullable List<ReportUrl> urls,
      @Nullable Set<String> rules,
      @Nullable List<Category> subcategories,
      @Nullable List<Category> subsubcategories
    ) {}
  }
}
