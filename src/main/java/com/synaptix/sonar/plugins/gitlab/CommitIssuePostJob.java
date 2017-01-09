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
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

/**
 * Compute comments to be added on the commit.
 */
public class CommitIssuePostJob implements PostJob {

    private static final Logger logger = Loggers.get(CommitIssuePostJob.class);

    private final GitLabPluginConfiguration configuration;
    private final GitLabApiFacade gitLabApiFacade;
    private final MarkDownUtils markDownUtils;

    public CommitIssuePostJob(GitLabPluginConfiguration configuration, GitLabApiFacade gitLabApiFacade,
            MarkDownUtils markDownUtils) {
        this.configuration = configuration;
        this.gitLabApiFacade = gitLabApiFacade;
        this.markDownUtils = markDownUtils;
    }

    @Override
    public void describe(@Nonnull PostJobDescriptor descriptor) {
        descriptor.name("GitLab plugin issue publisher").requireProperty(GitLabPlugin.GITLAB_COMMIT_SHA);
    }

    @Override
    public void execute(@Nonnull PostJobContext context) {
        GlobalReport report = new GlobalReport(configuration, markDownUtils);

        StreamSupport
                .stream(context.issues().spliterator(), false)
                .filter(PostJobIssue::isNew)
                .filter(i -> {
                    InputComponent inputComponent = i.inputComponent();
                    return inputComponent != null
                            && inputComponent.isFile()
                            && gitLabApiFacade.hasFile((InputFile) inputComponent);
                })
                .forEach(i -> {
                    InputFile inputFile = (InputFile) i.inputComponent();
                    boolean hasFileLine = gitLabApiFacade.hasFileLine(inputFile, i.line());
                    if (hasFileLine) {
                        createInlineComment(inputFile, i);
                    }
                    report.update(i, gitLabApiFacade.getGitLabUrl(configuration.commitSHA(), inputFile, i.line()), hasFileLine);
                });

        if (report.hasNewIssues() || configuration.commentNoIssue()) {
            gitLabApiFacade.createGlobalComment(report.toMarkdown());
        }

        gitLabApiFacade.createCommitStatus(report.getStatus(), report.getStatusDescription());
    }

    private void createInlineComment(InputFile inputFile, PostJobIssue issue) {
        logger.debug("Create inline comment for rule key {} on file {} with revision {}", issue.ruleKey(), inputFile,
                configuration.commitSHA());
        String body = markDownUtils.inlineIssue(issue.severity(), issue.message(), issue.ruleKey().toString());

        boolean exists = gitLabApiFacade.getCommitCommentsForFile(configuration.commitSHA(), inputFile)
                .stream()
                .anyMatch(c -> c.getLine().equals(Integer.toString(issue.line())) && c.getNote().equals(body));
        if (!exists) {
            gitLabApiFacade.createOrUpdateReviewComment(inputFile, issue.line(), body);
        }
    }

}
