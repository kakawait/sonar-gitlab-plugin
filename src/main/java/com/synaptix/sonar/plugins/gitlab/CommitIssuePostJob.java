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

import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Compute comments to be added on the commit.
 */
public class CommitIssuePostJob implements PostJob {

    private final GitLabPluginConfiguration gitLabPluginConfiguration;
    private final CommitFacade commitFacade;
    private final MarkDownUtils markDownUtils;

    public CommitIssuePostJob(GitLabPluginConfiguration gitLabPluginConfiguration, CommitFacade commitFacade,
            MarkDownUtils markDownUtils) {
        this.gitLabPluginConfiguration = gitLabPluginConfiguration;
        this.commitFacade = commitFacade;
        this.markDownUtils = markDownUtils;
    }

    @Override
    public void describe(@Nonnull PostJobDescriptor descriptor) {
        descriptor.name("GitLab plugin issue publisher").requireProperty(GitLabPlugin.GITLAB_COMMIT_SHA);
    }

    @Override
    public void execute(@Nonnull PostJobContext context) {
        GlobalReport report = new GlobalReport(gitLabPluginConfiguration, markDownUtils);

        Map<InputFile, Map<Integer, StringBuilder>> commentsToBeAddedByLine = processIssues(context, report);

        updateReviewComments(commentsToBeAddedByLine);

        if (report.hasNewIssue() || gitLabPluginConfiguration.commentNoIssue()) {
            commitFacade.addGlobalComment(report.formatForMarkdown());
        }

        commitFacade.createOrUpdateSonarQubeStatus(report.getStatus(), report.getStatusDescription());
    }

    @Override
    public String toString() {
        return "GitLab Commit Issue Publisher";
    }

    private Map<InputFile, Map<Integer, StringBuilder>> processIssues(PostJobContext context, GlobalReport report) {
        Map<InputFile, Map<Integer, StringBuilder>> commentToBeAddedByFileAndByLine = new HashMap<>();
        for (PostJobIssue issue : context.issues()) {
            Severity severity = issue.severity();
            boolean isNew = issue.isNew();
            if (!isNew) {
                continue;
            }
            Integer issueLine = issue.line();
            InputComponent component = issue.inputComponent();
            if (component == null || !component.isFile() || !(component instanceof InputFile)) {
                continue;
            }
            InputFile inputFile = (InputFile) component;
            if (gitLabPluginConfiguration.ignoreFileNotModified() && !commitFacade.hasFile(inputFile)) {
                continue;
            }
            boolean reportedInline = false;
            if (issueLine != null) {
                int line = issueLine;
                if (commitFacade.hasFileLine(inputFile, line)) {
                    String message = issue.message();
                    String ruleKey = issue.ruleKey().toString();
                    if (!commentToBeAddedByFileAndByLine.containsKey(inputFile)) {
                        commentToBeAddedByFileAndByLine.put(inputFile, new HashMap<>());
                    }
                    Map<Integer, StringBuilder> commentsByLine = commentToBeAddedByFileAndByLine.get(inputFile);
                    if (!commentsByLine.containsKey(line)) {
                        commentsByLine.put(line, new StringBuilder());
                    }
                    commentsByLine.get(line).append(markDownUtils.inlineIssue(severity.name(), message, ruleKey))
                                  .append("\n");
                    reportedInline = true;
                }
            }
            report.process(issue, commitFacade.getGitLabUrl(inputFile, issueLine), reportedInline);

        }
        return commentToBeAddedByFileAndByLine;
    }

    private void updateReviewComments(Map<InputFile, Map<Integer, StringBuilder>> commentsToBeAddedByLine) {
        for (Map.Entry<InputFile, Map<Integer, StringBuilder>> entry : commentsToBeAddedByLine.entrySet()) {
            for (Map.Entry<Integer, StringBuilder> entryPerLine : entry.getValue().entrySet()) {
                String body = entryPerLine.getValue().toString();
                commitFacade.createOrUpdateReviewComment(entry.getKey(), entryPerLine.getKey(), body);
            }
        }
    }
}
