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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public class GitLabPlugin implements Plugin {

    static final String GITLAB_URL = "sonar.gitlab.url";
    static final String GITLAB_MAX_GLOBAL_ISSUES = "sonar.gitlab.max_global_issues";
    static final String GITLAB_USER_TOKEN = "sonar.gitlab.user_token";
    static final String GITLAB_PROJECT_ID = "sonar.gitlab.project_id";
    static final String GITLAB_COMMIT_HASHES = "sonar.gitlab.commit_hashes";
    static final String GITLAB_REF_NAME = "sonar.gitlab.ref_name";
    static final String GITLAB_GLOBAL_COMMENT_NO_ISSUE = "sonar.gitlab.comment_no_issue";
    static final String GITLAB_IGNORE_SSL = "sonar.gitlab.ignore_ssl";
    static final String GITLAB_BUILD_INIT_STATE = "sonar.gitlab.build_init_state";
    static final String GITLAB_DISABLE_GLOBAL_COMMENT = "sonar.gitlab.disable_global_comment";
    static final String GITLAB_STATUS_NOTIFICATION_MODE = "sonar.gitlab.failure_notification_mode";
    static final String GITLAB_PING_USER = "sonar.gitlab.ping_user";

    static final List<String> BUILD_INIT_STATES = Collections.unmodifiableList(Arrays.asList("pending", "running"));
    private static final List<String> STATUS_NOTIFICATIONS_MODE = Collections.unmodifiableList(
            Arrays.asList("commit-status", "exit-code")
    );

    private static final String CATEGORY = "gitlab";
    private static final String INSTANCE_SUBCATEGORY = "instance";
    private static final String REPORTING_SUBCATEGORY = "reporting";

    private static List<PropertyDefinition> definitions() {
        return Arrays.asList(
                PropertyDefinition
                        .builder(GITLAB_URL)
                        .name("GitLab url")
                        .description("URL to access GitLab. Default value is fine for public GitLab. " +
                                "Can be modified for GitLab local installation.")
                        .category(CATEGORY)
                        .subCategory(INSTANCE_SUBCATEGORY)
                        .defaultValue("https://gitlab.com")
                        .index(1)
                        .build(),
                PropertyDefinition
                        .builder(GITLAB_USER_TOKEN)
                        .name("GitLab user token")
                        .description("Token for the user that SonarQube will comment as.")
                        .category(CATEGORY)
                        .subCategory(INSTANCE_SUBCATEGORY)
                        .index(2)
                        .build(),
                PropertyDefinition
                        .builder(GITLAB_MAX_GLOBAL_ISSUES)
                        .name("GitLab max global issues")
                        .description("Max issues to show in global comment.")
                        .category(CATEGORY)
                        .subCategory(REPORTING_SUBCATEGORY)
                        .type(PropertyType.INTEGER)
                        .defaultValue(String.valueOf(10))
                        .index(3)
                        .build(),
                PropertyDefinition
                        .builder(GITLAB_PROJECT_ID)
                        .name("GitLab Project id")
                        .description("The unique id, path with namespace, name with namespace, web url, ssh url " +
                                "or http url of the current project that GitLab.")
                        .category(CATEGORY)
                        .subCategory(REPORTING_SUBCATEGORY)
                        .onlyOnQualifiers(Qualifiers.PROJECT)
                        .index(4)
                        .build(),
                PropertyDefinition
                        .builder(GITLAB_COMMIT_HASHES)
                        .name("Commit hashes")
                        .description("The commit revisions that will be used by plugin to find issue on files.")
                        .category(CATEGORY)
                        .subCategory(REPORTING_SUBCATEGORY)
                        .hidden()
                        .index(5)
                        .build(),
                PropertyDefinition
                        .builder(GITLAB_REF_NAME)
                        .name("Commit reference")
                        .description("The commit reference for which project is built.")
                        .category(CATEGORY)
                        .subCategory(REPORTING_SUBCATEGORY)
                        .hidden()
                        .index(6)
                        .build(),
                PropertyDefinition
                        .builder(GITLAB_GLOBAL_COMMENT_NO_ISSUE)
                        .name("Global comment even when there is no new issues")
                        .description("Add a global comment even when there is no new issue.")
                        .category(CATEGORY)
                        .subCategory(REPORTING_SUBCATEGORY)
                        .type(PropertyType.BOOLEAN)
                        .defaultValue(String.valueOf(false))
                        .index(7)
                        .build(),
                PropertyDefinition
                        .builder(GITLAB_IGNORE_SSL)
                        .name("Ignore SSL")
                        .description("Ignore SSL certificate for https GitLab connections.")
                        .category(CATEGORY)
                        .subCategory(INSTANCE_SUBCATEGORY)
                        .type(PropertyType.BOOLEAN)
                        .defaultValue(String.valueOf(false))
                        .index(8)
                        .build(),
                PropertyDefinition
                        .builder(GITLAB_BUILD_INIT_STATE)
                        .name("Build Initial State")
                        .description("State that should be the first when build commit status update is called.")
                        .category(CATEGORY)
                        .subCategory(REPORTING_SUBCATEGORY)
                        .index(9)
                        .type(PropertyType.SINGLE_SELECT_LIST)
                        .options(BUILD_INIT_STATES)
                        .defaultValue("running")
                        .build(),
                PropertyDefinition
                        .builder(GITLAB_DISABLE_GLOBAL_COMMENT)
                        .name("Disable global comment")
                        .description("Disable global comment, report only inline.")
                        .category(CATEGORY)
                        .subCategory(REPORTING_SUBCATEGORY)
                        .index(10)
                        .defaultValue(String.valueOf(false))
                        .build(),
                PropertyDefinition
                        .builder(GITLAB_STATUS_NOTIFICATION_MODE)
                        .name("Status notification mode")
                        .description("Status notification mode: commit-status or exit-code")
                        .category(CATEGORY)
                        .subCategory(REPORTING_SUBCATEGORY)
                        .type(PropertyType.SINGLE_SELECT_LIST)
                        .options(STATUS_NOTIFICATIONS_MODE)
                        .index(11)
                        .defaultValue("commit-status")
			.build(),
		PropertyDefinition.builder(GITLAB_PING_USER).name("Ping the user")
			.description("Ping the user who made an issue by @ mentioning.")//
			.category(CATEGORY)
			.subCategory(REPORTING_SUBCATEGORY)//
			.type(PropertyType.BOOLEAN)//
			.defaultValue(String.valueOf(false))//
			.index(12)
                        .build()
        );
    }

    @Override
    public void define(@Nonnull Context context) {
        context.addExtensions(CommitIssuePostJob.class, GitLabPluginConfiguration.class, CommitProjectBuilder.class,
                GitLabApiFacade.class, MarkDownUtils.class);
        context.addExtensions(definitions());
    }
}
