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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;

public class MarkDownUtilsTest {
    private MarkDownUtils markDownUtils;

    @Before
    public void setUp() {
        Settings settings = new MapSettings();
        settings.appendProperty("sonar.host.url", "http://test.test.xx");
        markDownUtils = new MarkDownUtils(settings);
    }

    @Test
    public void testInlineIssue_NoAuthor() {
        Severity severity = Severity.BLOCKER;
        String message = "Something bad happend";
        String ruleKey = "RuleKey";
        Optional<String> author = Optional.empty();

        assertThat(markDownUtils.inlineIssue(severity, message, ruleKey, author).contains("@"), equalTo(false));
    }

    @Test
    public void testInlineIssue_Author() {
        Severity severity = Severity.BLOCKER;
        String message = "Something bad happend";
        String ruleKey = "RuleKey";
        Optional<String> author = Optional.of("dve");

        assertThat(markDownUtils.inlineIssue(severity, message, ruleKey, author).contains("@dve "), equalTo(true));
    }
}
