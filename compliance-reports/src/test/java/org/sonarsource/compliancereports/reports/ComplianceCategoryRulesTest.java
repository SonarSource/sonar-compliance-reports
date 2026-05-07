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
package org.sonarsource.compliancereports.reports;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static java.util.Map.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ComplianceCategoryRulesTest {

  @Test
  void getChildren_shouldReturnInOrderedByOrdinalAsc() {
    assertChildrenOrder(1, 2, true, "Category A.1", "Category A.2");
  }

  @Test
  void getChildren_shouldReturnInOrderedByOrdinalDesc() {
    assertChildrenOrder(2, 1, true, "Category A.2", "Category A.1");
  }

  @Test
  void getChildren_shouldReturnInOrderedByKeyWhenNoOrdinalAsc() {
    assertChildrenOrder(null, null, true, "Category A.1", "Category A.2");
  }

  @Test
  void getChildren_shouldReturnInOrderedByKeyWhenNoOrdinalDesc() {
    assertChildrenOrder(null, null, false, "Category A.1", "Category A.2");
  }

  private void assertChildrenOrder(Integer ordinal1, Integer ordinal2,
                                    boolean insertInReverseOrder,
                                    String expectedFirstKey, String expectedSecondKey) {
    CategoryTree.CategoryTreeNode root = createNode("Category A", 1);
    CategoryTree.CategoryTreeNode child1 = createNode("Category A.1", ordinal1);
    CategoryTree.CategoryTreeNode child2 = createNode("Category A.2", ordinal2);

    ComplianceCategoryRules categoryRules = new ComplianceCategoryRules(root);
    categoryRules.getChildrenByNode().putAll(
      insertInReverseOrder
        ? of(child2, new ComplianceCategoryRules(child2), child1, new ComplianceCategoryRules(child1))
        : of(child1, new ComplianceCategoryRules(child1), child2, new ComplianceCategoryRules(child2))
    );

    List<Map.Entry<String, ComplianceCategoryRules>> children = categoryRules.getChildren().entrySet().stream().toList();
    assertEquals(2, children.size());
    assertEquals(expectedFirstKey, children.get(0).getKey());
    assertEquals(expectedSecondKey, children.get(1).getKey());
  }

  private CategoryTree.CategoryTreeNode createNode(String key, Integer ordinal) {
    return new CategoryTree.CategoryTreeNode(key, Set.of(), Set.of(), null, false, 0, ordinal, null);
  }
}
