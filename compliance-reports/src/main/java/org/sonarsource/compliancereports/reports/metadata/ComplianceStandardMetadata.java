/*
 * Compliance Reports
 * Copyright (C) 2022-2026 SonarSource Sàrl
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
package org.sonarsource.compliancereports.reports.metadata;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

public record ComplianceStandardMetadata(Report report) {

  public record Report(
    String name,
    String key,
    @Nullable String description,
    @Nullable ReportClassification classification,
    @Nullable List<ReportUrl> urls,
    Taxonomy taxonomy,
    @Nullable Levels levels,
    List<Version> versions) {

    public Report withoutRules() {
      return new Report(
        this.name,
        this.key,
        this.description,
        this.classification,
        this.urls,
        this.taxonomy,
        this.levels,
        this.versions.stream().map(Version::withoutRules).toList()
      );
    }

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
      String key,
      @Nullable String description,
      @Nullable List<ReportUrl> urls,
      @Nullable List<Category> categories
    ) {

      public Version withoutRules() {
        return new Version(
          this.name,
          this.key,
          this.description,
          this.urls,
          this.categories == null ? null : this.categories.stream().map(Category::withoutRules).toList()
        );
      }
    }

    public record Category(
      String name,
      @Nullable String description,
      @Nullable String key,
      @Nullable String level,
      @Nullable List<ReportUrl> urls,
      @Nullable Set<String> rules,
      @Nullable List<Category> subcategories,
      @Nullable Integer ordinal,
      @Nullable Set<String> cwes
    ) {

      public Category withoutRules() {
        return new Category(
          this.name,
          this.description,
          this.key,
          this.level,
          this.urls,
          null,
          this.subcategories == null ? null : this.subcategories.stream().map(Category::withoutRules).toList(),
          this.ordinal,
          this.cwes
        );
      }
    }
  }
}
