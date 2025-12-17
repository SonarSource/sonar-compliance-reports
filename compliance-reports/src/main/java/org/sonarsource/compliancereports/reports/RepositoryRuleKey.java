/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

package org.sonarsource.compliancereports.reports;

public record RepositoryRuleKey(String repository, String rule) {
  @Override
  public String toString() {
    return repository + ":" + rule;
  }

  public static RepositoryRuleKey of(String ruleKey) {
    int pos = ruleKey.indexOf(':');
    String repo = ruleKey.substring(0, pos);
    String key = ruleKey.substring(pos + 1);
    return new RepositoryRuleKey(repo, key);
  }
}
