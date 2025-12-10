/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import io.sonarcloud.compliancereports.reports.metadata.ReportMetadataSchema;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Represents the parsed metadata for a single report (standard + version)
 */
public class CategoryTree {
  private final ReportKey key;
  private final Set<CategoryTreeNode> children;

  @Nullable
  private final ReportMetadataSchema.Report.Levels levels;

  public record CategoryTreeNode(String key, Set<String> ruleKeys, Set<CategoryTreeNode> children, @Nullable Integer levelIndex,
    boolean levelsInclusive, int numberOfLevels) {}

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

    return new CategoryTreeNode(category.name(), rules, subnodes, levelIndex, levelsInclusive, levels == null ? 0 : levels.values().size());
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
}
