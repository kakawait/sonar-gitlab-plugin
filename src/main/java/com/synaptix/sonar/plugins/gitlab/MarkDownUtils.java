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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.Settings;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ScannerSide
public class MarkDownUtils {

    private static final String SONAR_HOST_URL_PROPERTY_KEY = "sonar.host.url";

    private static final String INLINE_ISSUE_COMMENT_TEMPLATE = "%s %s %s";

    private static final String INLINE_ISSUE_WITH_AUTHOR_COMMENT_TEMPLATE = "%s %s %s %s";

    private static final String GLOBAL_ISSUE_COMMENT_TEMPLATE = "%s %s (%s) %s";

    private static final String GLOBAL_ISSUE_WITH_URL_COMMENT_TEMPLATE = "%s [%s](%s) %s";

    private static final Map<Severity, String> SEVERITY_EMOJI_MAPPINGS = Collections.unmodifiableMap(
            new HashMap<Severity, String>() {{
                put(Severity.BLOCKER, ":no_entry:");
                put(Severity.CRITICAL, ":no_entry_sign:");
                put(Severity.MAJOR, ":warning:");
                put(Severity.MINOR, ":arrow_down_small:");
                put(Severity.INFO, ":information_source:");
            }});

    private final String baseUrl;

    /**
     * Sets up these utilities.
     * <p>
     * If {@value org.sonar.api.CoreProperties#SERVER_BASE_URL} is configured, it will be used over the sonar host URL.
     *
     * @param settings SonarQube settings in order to get SonarQube url.
     *
     * @throws IllegalArgumentException if missing SonarQube base url.
     */
    public MarkDownUtils(Settings settings) {
        String baseUrl = settings.hasKey(CoreProperties.SERVER_BASE_URL)
                ? settings.getString(CoreProperties.SERVER_BASE_URL)
                : settings.getString(SONAR_HOST_URL_PROPERTY_KEY);
        if (baseUrl == null) {
            throw new IllegalArgumentException(String.format("A base URL must be provided with the setting %s or %s",
                    CoreProperties.SERVER_BASE_URL,
                    SONAR_HOST_URL_PROPERTY_KEY
            ));
        }

        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        this.baseUrl = baseUrl;
    }

    /**
     * Returns the markdown emoji text for a violation severity.
     *
     * @param severity issue severity to be mapped.
     *
     * @return An {@link String} representing a markdown <i>Emoji</i>.
     */
    static String getEmojiForSeverity(Severity severity) {
        return SEVERITY_EMOJI_MAPPINGS.getOrDefault(severity, ":grey_question:");
    }

    /**
     * Format a rule violation for display inline with other information.
     *
     * @param severity issue severity to be used to get icon.
     * @param message message to be display.
     * @param ruleKey reference to rule, will be transformed to link to SonarQube instance.
     *
     * @return inline comment that will be posted to GitLab commit.
     *
     * @throws IllegalArgumentException if one of the method parameter is null.
     */
    String inlineIssue(Severity severity, String message, String ruleKey, String author) {
        assertNotNull(severity, "severity must not be null");
        assertNotNull(message, "message must not be null");
        assertNotNull(ruleKey, "ruleKey must not be null");

        String ruleLink = getRuleLink(ruleKey);

	if (author == null) {
	    return String.format(INLINE_ISSUE_COMMENT_TEMPLATE, getEmojiForSeverity(severity), message, ruleLink);
	} else {
	    return String.format(INLINE_ISSUE_WITH_AUTHOR_COMMENT_TEMPLATE, getEmojiForSeverity(severity), "@" + author,
		    message, ruleLink);
	}
    }

    /**
     * Build an entry for the global issues / rule violations comment.
     *
     * @param severity issue severity to be used to get icon.
     * @param message message to be display.
     * @param ruleKey reference to rule, will be transformed to link to SonarQube instance.
     * @param url
     * @param componentKey
     *
     * @return global comment that will be posted to GitLab commit.
     *
     * @throws IllegalArgumentException if one of the method parameter is null.
     */
    String globalIssue(Severity severity, String message, String ruleKey, @Nullable String url, String componentKey) {
        assertNotNull(severity, "severity must not be null");
        assertNotNull(message, "message must not be null");
        assertNotNull(ruleKey, "ruleKey must not be null");
        assertNotNull(componentKey, "componentKey must not be null");

        String ruleLink = getRuleLink(ruleKey);
        String emoji = getEmojiForSeverity(severity);

        if (url == null) {
            return String.format(GLOBAL_ISSUE_COMMENT_TEMPLATE, emoji, message, componentKey, ruleLink);
        } else {
            return String.format(GLOBAL_ISSUE_WITH_URL_COMMENT_TEMPLATE, emoji, message, url, ruleLink);
        }
    }

    private static String encodeForUrl(String url) {
        try {
            return URLEncoder.encode(url, StandardCharsets.UTF_8.displayName());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Encoding not supported", e);
        }
    }

    private String getRuleLink(String ruleKey) {
        assertNotNull(ruleKey, "ruleKey must not be null");

        return "[:blue_book:](" + baseUrl + "coding_rules#rule_key=" + encodeForUrl(ruleKey) + ")";
    }

    private void assertNotNull(Object value, String errorMessage) {
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }



}
