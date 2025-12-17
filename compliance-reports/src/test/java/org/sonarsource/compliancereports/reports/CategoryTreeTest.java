/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonarsource.compliancereports.reports;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonarsource.compliancereports.reports.metadata.ReportMetadataSchema;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTreeTest {

  @Test
  void givenMetadataWithNestedCategories_shouldParseTree() {
    ReportMetadataSchema.Report.Version given = new ReportMetadataSchema.Report.Version(
      "version",
      "",
      null,
      null,
      List.of(
        new ReportMetadataSchema.Report.Category("cat1", null, null, null, null, Set.of("a", "b", "c"), List.of(
          new ReportMetadataSchema.Report.Category("cat1.1", null, null, null, null, Set.of("d", "e", "f"), List.of(
            new ReportMetadataSchema.Report.Category("cat1.1.1", null, null, null, null, Set.of("g", "h", "i"), null),
            new ReportMetadataSchema.Report.Category("cat1.1.2", null, null, null, null, Set.of("j", "k", "l"), null)
          )))),
        new ReportMetadataSchema.Report.Category("cat2", null, null, null, null, Set.of("a", "b", "c"), List.of(
          new ReportMetadataSchema.Report.Category("cat2.1", null, null, null, null, Set.of("d", "e", "f"), List.of(
            new ReportMetadataSchema.Report.Category("cat2.1.1", null, null, null, null, Set.of("g", "h", "i"), null))),
          new ReportMetadataSchema.Report.Category("cat2.2", null, null, null, null, Set.of("d", "e", "f"), List.of(
            new ReportMetadataSchema.Report.Category("cat2.2.1", null, null, null, null, Set.of("g", "h", "i"), null)
          ))))
      )
    );

    CategoryTree underTest = new CategoryTree("key", given, null);

    assertThat(underTest.getChildren()).hasSize(2);
    assertThat(countAllNodes(underTest.getChildren())).isEqualTo(9);
  }

  private static int countAllNodes(Set<CategoryTree.CategoryTreeNode> nodes) {
    return nodes.stream().mapToInt(CategoryTreeTest::countNodes).sum();
  }

  private static int countNodes(CategoryTree.CategoryTreeNode node) {
    return 1 + countAllNodes(node.children());
  }
}
