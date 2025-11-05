/*
 * Copyright (C) 2022-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

package io.sonarcloud.compliancereports.dao;

import java.util.Set;
import java.util.UUID;

public interface ActiveRuleDao {

  Set<String> getActiveRuleKeysForProject(UUID projectId);
}
