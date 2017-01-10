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

import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.config.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ScannerSide
public class GitLabPluginConfiguration {

    private Settings settings;

    public GitLabPluginConfiguration(Settings settings) {
        this.settings = settings;
    }

    boolean isEnabled() {
        return settings.hasKey(GitLabPlugin.GITLAB_COMMIT_HASHES);
    }

    @CheckForNull
    String projectId() {
        return settings.getString(GitLabPlugin.GITLAB_PROJECT_ID);
    }

    @CheckForNull
    List<String> commitHashes() {
        return Arrays.asList(settings.getStringArray(GitLabPlugin.GITLAB_COMMIT_HASHES));
    }

    @CheckForNull
    String referenceName() {
        return settings.getString(GitLabPlugin.GITLAB_REF_NAME);
    }

    @CheckForNull
    String userToken() {
        return settings.getString(GitLabPlugin.GITLAB_USER_TOKEN);
    }

    @CheckForNull
    String url() {
        return settings.getString(GitLabPlugin.GITLAB_URL);
    }

    @CheckForNull
    int maxGlobalIssues() {
        return settings.getInt(GitLabPlugin.GITLAB_MAX_GLOBAL_ISSUES);
    }

    @CheckForNull
    boolean commentNoIssue() {
        return settings.getBoolean(GitLabPlugin.GITLAB_GLOBAL_COMMENT_NO_ISSUE);
    }

    @CheckForNull
    boolean ignoreSSL() {
        return settings.getBoolean(GitLabPlugin.GITLAB_IGNORE_SSL);
    }

    @CheckForNull
    String getBuildInitState() {
        return settings.getString(GitLabPlugin.GITLAB_BUILD_INIT_STATE);
    }

    @CheckForNull
    boolean disableGlobalComment() {
        return settings.getBoolean(GitLabPlugin.GITLAB_DISABLE_GLOBAL_COMMENT);
    }

    @CheckForNull
    String statusNotificationMode() {
        return settings.getString(GitLabPlugin.GITLAB_STATUS_NOTIFICATION_MODE);
    }
}
