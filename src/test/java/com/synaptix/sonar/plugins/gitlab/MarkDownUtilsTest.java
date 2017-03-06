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
