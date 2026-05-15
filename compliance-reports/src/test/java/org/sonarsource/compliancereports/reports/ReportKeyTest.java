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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportKeyTest {
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
