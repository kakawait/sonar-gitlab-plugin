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

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.utils.MessageException;

/**
 * Trigger load of pull request metadata at the very beginning of SQ analysis. Also
 * set "in progress" status on the pull request.
 */
public class CommitProjectBuilder extends ProjectBuilder {

    private final GitLabPluginConfiguration configuration;
    private final GitLabApiFacade gitLabApiFacade;
    private final AnalysisMode mode;

    public CommitProjectBuilder(GitLabPluginConfiguration configuration, GitLabApiFacade gitLabApiFacade,
            AnalysisMode mode) {
        this.configuration = configuration;
        this.gitLabApiFacade = gitLabApiFacade;
        this.mode = mode;
    }

    @Override
    public void build(Context context) {
        if (!configuration.isEnabled()) {
            return;
        }
        if (!mode.isIssues()) {
            throw MessageException.of("The GitHub plugin is only intended to be used in preview or issues mode. " +
                    "Please set '" + CoreProperties.ANALYSIS_MODE + "'.");
        }

        gitLabApiFacade.init(context.projectReactor().getRoot().getBaseDir());
        if (configuration.statusNotificationMode().equals("commit-status")) {
            gitLabApiFacade.createCommitStatus(configuration.commitHashes().get(0), configuration.getBuildInitState(),
                    "SonarQube analysis in progress");
        }
    }
}
