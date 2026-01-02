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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonarsource.compliancereports.reports.metadata.ReportMetadataSchema;

/**
 * Represents the parsed metadata for a single report (standard + version)
 */
public class CategoryTree {
  private final ReportKey key;
  private final Set<CategoryTreeNode> children;

  @Nullable
  private final ReportMetadataSchema.Report.Levels levels;

  public record CategoryTreeNode(String key, Set<String> ruleKeys, Set<CategoryTreeNode> children, @Nullable Integer levelIndex,
    boolean levelsInclusive, int numberOfLevels, @Nullable Integer ordinal) {}

  public CategoryTree(String standardKey, ReportMetadataSchema.Report.Version version, @Nullable ReportMetadataSchema.Report.Levels levels) {
    this.key = new ReportKey(standardKey, version.key());
    this.levels = levels;

    if (version.categories() != null) {
      children = version.categories().stream()
        .map(this::generateTreeNode)
        .collect(Collectors.toSet());
    } else {
      children = Set.of();
    }
  }
  private CategoryTreeNode generateTreeNode(ReportMetadataSchema.Report.Category category) {
    Set<CategoryTreeNode> subnodes = new HashSet<>();
    if (category.subcategories() != null && !category.subcategories().isEmpty()) {
      subnodes = category.subcategories().stream()
        .map(this::generateTreeNode)
        .collect(Collectors.toSet());
    }

    Set<String> rules = category.rules() == null ? new HashSet<>() : new HashSet<>(category.rules());
    Integer levelIndex = null;
    boolean levelsInclusive = false;
    if (levels != null && category.level() != null) {
      levelIndex = levels.values().stream()
        .map(ReportMetadataSchema.Report.Levels.LevelValue::name)
        .toList()
        .indexOf(category.level());
      levelsInclusive = levels.inclusive();
    }

    return new CategoryTreeNode(category.name(), rules, subnodes, levelIndex, levelsInclusive, levels == null ? 0 : levels.values().size(), category.ordinal());
  }

  /**
   *
   * @return the top-level categories of the tree
   */
  public Set<CategoryTreeNode> getChildren() {
    return children;
  }

  public ReportKey getKey() {
    return key;
  }

  public static int categoryCompareTo(CategoryTreeNode a, CategoryTreeNode b) {
    // If both categories have ordinals, sort by ordinal
    Integer aOrdinal = a.ordinal();
    Integer bOrdinal = b.ordinal();
    if (aOrdinal != null && bOrdinal != null) {
      return Integer.compare(aOrdinal, bOrdinal);
    }
    // Otherwise, sort by name
    return a.key().compareToIgnoreCase(b.key());
  }
}
