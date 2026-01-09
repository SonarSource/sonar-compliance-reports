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

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonarsource.compliancereports.reports.metadata.ComplianceStandardMetadata;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTreeTest {

  @Test
  void givenMetadataWithNestedCategories_shouldParseTree() {
    ComplianceStandardMetadata.Report.Version given = new ComplianceStandardMetadata.Report.Version(
      "version",
      "",
      null,
      null,
      List.of(
        new ComplianceStandardMetadata.Report.Category("cat1", null, null, null, null, Set.of("a", "b", "c"), List.of(
          new ComplianceStandardMetadata.Report.Category("cat1.1", null, null, null, null, Set.of("d", "e", "f"), List.of(
            new ComplianceStandardMetadata.Report.Category("cat1.1.1", null, null, null, null, Set.of("g", "h", "i"), null, null),
            new ComplianceStandardMetadata.Report.Category("cat1.1.2", null, null, null, null, Set.of("j", "k", "l"), null, null)
          ), null)), null),
        new ComplianceStandardMetadata.Report.Category("cat2", null, null, null, null, Set.of("a", "b", "c"), List.of(
          new ComplianceStandardMetadata.Report.Category("cat2.1", null, null, null, null, Set.of("d", "e", "f"), List.of(
            new ComplianceStandardMetadata.Report.Category("cat2.1.1", null, null, null, null, Set.of("g", "h", "i"), null, null)), null),
          new ComplianceStandardMetadata.Report.Category("cat2.2", null, null, null, null, Set.of("d", "e", "f"), List.of(
            new ComplianceStandardMetadata.Report.Category("cat2.2.1", null, null, null, null, Set.of("g", "h", "i"), null, null)
          ), null)), null)
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
