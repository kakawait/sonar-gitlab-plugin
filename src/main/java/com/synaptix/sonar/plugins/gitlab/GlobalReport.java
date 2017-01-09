/*
 * SonarQube :: GitLab Plugin
 * Copyright (C) 2009-2016 Thibaud Leprêtre
 * thibaud.lepretre@gmail.com
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
package com.synaptix.sonar.plugins.gitlab;

import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import static com.synaptix.sonar.plugins.gitlab.MarkDownUtils.*;

class GlobalReport {

    private static final String SUCCESS_STATUS = "success";

    private static final String FAILED_STATUS = "failed";

    private final GitLabPluginConfiguration configuration;

    private final MarkDownUtils markDownUtils;

    private Map<Severity, Integer> numberOfIssuesBySeverity = Arrays
            .stream(Severity.values())
            .collect(Collectors.toMap(Function.identity(), e -> 0));

    private Map<Severity, List<String>> unreachableIssuesBySeverity = new HashMap<>();

    GlobalReport(GitLabPluginConfiguration configuration, MarkDownUtils markDownUtils) {
        this.configuration = configuration;
        this.markDownUtils = markDownUtils;
    }

    boolean hasNewIssues() {
        return numberOfIssuesBySeverity.values().stream().mapToInt(Integer::intValue).sum() > 0;
    }

    void update(PostJobIssue issue, @Nullable String gitLabUrl, boolean skip) {
        numberOfIssuesBySeverity.computeIfPresent(issue.severity(), (k, v) -> v + 1);
        if (!skip) {
            List<String> reports = unreachableIssuesBySeverity.computeIfAbsent(issue.severity(), k -> new ArrayList<>());

            String comment = markDownUtils.globalIssue(issue.severity(), issue.message(), issue.ruleKey().toString(),
                    gitLabUrl, issue.componentKey());
            reports.add("* " + comment);
        }
    }

    String toMarkdown() {
        StringBuilder report = new StringBuilder();
        report.append(reportNewIssues());
        if (hasNewIssues()) {
            report.append("\nWatch the comments in this conversation to review them.");
        }
        int numberOfUnreachableIssues = unreachableIssuesBySeverity.values().stream().mapToInt(List::size).sum();
        if (numberOfUnreachableIssues > 0) {
            report.append("\nNote: the following issues could not be reported as comments " +
                    "because they are located on lines that are not displayed in this commit:\n");

            report.append(unreachableIssuesBySeverity
                    .values()
                    .stream()
                    .flatMap(List::stream)
                    .limit(configuration.maxGlobalIssues())
                    .collect(Collectors.joining("\n")));
            if (numberOfUnreachableIssues > configuration.maxGlobalIssues()) {
                report.append("\n* ... ")
                      .append(numberOfUnreachableIssues - configuration.maxGlobalIssues())
                      .append(" more\n");
            }
        }
        return report.toString();
    }

    String getStatusDescription() {
        String report = "no issues";

        int numberOfIssues = numberOfIssuesBySeverity.values().stream().mapToInt(Integer::intValue).sum();
        if (numberOfIssues > 0) {
            String reportForSeverities = numberOfIssuesBySeverity
                    .entrySet()
                    .stream()
                    .filter(e -> e.getKey().equals(Severity.CRITICAL) || e.getKey().equals(Severity.BLOCKER))
                    .filter(e -> e.getValue() > 0)
                    .map(e -> String.format("%d %s", e.getValue(), e.getKey()))
                    .collect(Collectors.joining(" and "));

            String template = "{0} {0,choice,1#issue,|1<issues}, with %s";
            report = MessageFormat.format(template, numberOfIssues,
                    reportForSeverities.length() == 0 ? "no critical nor blocker issues" : reportForSeverities);
        }

        return "SonarQube reported: " + report;
    }

    String getStatus() {
        return (numberOfIssuesBySeverity.getOrDefault(Severity.BLOCKER, 0) > 0
                    || numberOfIssuesBySeverity.getOrDefault(Severity.CRITICAL, 0) > 0)
                ? FAILED_STATUS
                : SUCCESS_STATUS;
    }

    private String reportNewIssues() {
        int numberOfIssues = numberOfIssuesBySeverity.values().stream().mapToInt(Integer::intValue).sum();
        String report = "no issues";

        if (numberOfIssues > 0) {
            String template = "{0} {0,choice,1#issue|1<issues}:\n";
            report = MessageFormat.format(template, numberOfIssues);
            report += numberOfIssuesBySeverity
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue() > 0)
                    .map(e -> String.format("* %s %d %s\n",
                            getEmojiForSeverity(e.getKey()), e.getValue(), e.getKey().name().toLowerCase()))
                    .collect(Collectors.joining());
        }

        return "SonarQube analysis reported " + report;
    }

}
