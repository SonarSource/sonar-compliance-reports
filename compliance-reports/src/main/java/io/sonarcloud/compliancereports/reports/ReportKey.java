/*
 * Copyright (C) 2022-2025 SonarSource Sàrl
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package io.sonarcloud.compliancereports.reports;

public record ReportKey(String standard, String version) {
  @Override
  public String toString() {
    return standard + ":" + version;
  }

  public static ReportKey parse(String reportKey) {
    int i = reportKey.indexOf(':');
    if (i < 0) {
      throw new IllegalStateException("Invalid format: " + reportKey);
    }
    return new ReportKey(reportKey.substring(0, i), reportKey.substring(i + 1));
  }
}
