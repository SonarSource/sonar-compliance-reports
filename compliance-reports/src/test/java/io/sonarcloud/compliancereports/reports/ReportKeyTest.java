/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReportKeyTest {
  @Test
  void toString_encodes_key() {
    assertThat(new ReportKey("standard", "version")).hasToString("standard:version");
  }

  @Test
  void parse_succeeds() {
    assertThat(ReportKey.parse("standard:version1:2"))
      .isEqualTo(new ReportKey("standard", "version1:2"));
  }

  @Test
  void parse_fails() {
    assertThatThrownBy(() -> ReportKey.parse("standard")).isInstanceOf(IllegalStateException.class);
  }

}
