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

import java.util.Optional;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

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
        descriptor.name("GitLab plugin issue publisher").requireProperty(GitLabPlugin.GITLAB_COMMIT_HASHES);
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
                    Optional<String> revision = gitLabApiFacade.getRevisionForLine(inputFile, i.line());
                    revision.ifPresent(r -> createInlineComment(r, inputFile, i));
                    if (!revision.isPresent()) {
                        logger.debug("Unable to find line {} on file {} in revisions {}",
                                i.line(), inputFile, configuration.commitHashes());
                    }
                    report.update(i, gitLabApiFacade.getGitLabUrl(configuration.commitHashes().get(0), inputFile,
                            i.line()), revision.isPresent());
                });

        if (!configuration.disableGlobalComment() && report.hasNewIssues() || configuration.commentNoIssue()) {
            gitLabApiFacade.createGlobalComment(report.toMarkdown());
        }

        String status = report.getStatus();
        String statusDescription = report.getStatusDescription();
        if (configuration.statusNotificationMode().equals("status-code")) {
            String message = String.format("Call to commit status update with: status=%s, desc=%s",
                    status, statusDescription);
            if (status.equals("failed")) {
                throw MessageException.of(message);
            }
            logger.info(message);
        } else if (configuration.statusNotificationMode().equals("commit-status")) {
            gitLabApiFacade.createCommitStatus(configuration.commitHashes().get(0), status,
                    report.getStatusDescription());
        }
    }

    private void createInlineComment(String revision, InputFile inputFile, PostJobIssue issue) {
        logger.debug("Create inline comment for rule key {} on file {} and line {} with revision {}", issue.ruleKey(),
                inputFile, issue.line(), revision);
        Optional<String> username = configuration.pingUser() ? gitLabApiFacade.getUsernameForRevision(revision)
                : Optional.empty();
        String body = markDownUtils.inlineIssue(issue.severity(), issue.message(), issue.ruleKey().toString(),
                username);

        boolean exists = gitLabApiFacade.getCommitCommentsForFile(revision, inputFile)
                .stream()
                .anyMatch(c -> c.getLine().equals(Integer.toString(issue.line())) && c.getNote().equals(body));
        logger.debug("Inline comment already present on revision {} for file {} on line {}",
                revision, inputFile, issue.line());
        if (!exists) {
            gitLabApiFacade.createInlineComment(revision, inputFile, issue.line(), body);
        }
    }

}
