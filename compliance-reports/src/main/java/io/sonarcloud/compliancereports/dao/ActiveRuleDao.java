/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.dao;

import java.util.Set;
import java.util.UUID;

public interface ActiveRuleDao {

  Set<String> getActiveRuleKeys(UUID aggregationId, AggregationType aggregationType);
}
