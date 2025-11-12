/*
 * Copyright (C) 2022-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.reports.metadata.ReportMetadataSchema;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBucketsTest {

  @Test
  void givenMetadataWithNestedCategories_shouldFlattenHierarchy() {
    ReportMetadataSchema.Report.Version given = new ReportMetadataSchema.Report.Version(
      "version",
      "",
      null,
      null,
      List.of(
        new ReportMetadataSchema.Report.Category("cat1", null, null, null, Set.of("a", "b", "c"), List.of(
          new ReportMetadataSchema.Report.Category("cat1.1", null, null, null, Set.of("d", "e", "f"), null, List.of(
            new ReportMetadataSchema.Report.Category("cat1.1.1", null, null, null, Set.of("g", "h", "i"), null, null),
            new ReportMetadataSchema.Report.Category("cat1.1.2", null, null, null, Set.of("j", "k", "l"), null, null)
          ))), null),
        new ReportMetadataSchema.Report.Category("cat2", null, null, null, Set.of("a", "b", "c"), List.of(
          new ReportMetadataSchema.Report.Category("cat2.1", null, null, null, Set.of("d", "e", "f"), null, List.of(
            new ReportMetadataSchema.Report.Category("cat2.1.1", null, null, null, Set.of("g", "h", "i"), null, null))),
          new ReportMetadataSchema.Report.Category("cat2.2", null, null, null, Set.of("d", "e", "f"), null, List.of(
            new ReportMetadataSchema.Report.Category("cat2.2.1", null, null, null, Set.of("g", "h", "i"), null, null)
          ))), null)
      )
    );

    RuleBuckets underTest = new RuleBuckets("key", given);

    // should have one bucket for each category, subcategory, and subsubcategory
    assertThat(underTest.getBuckets()).hasSize(9);
  }
}