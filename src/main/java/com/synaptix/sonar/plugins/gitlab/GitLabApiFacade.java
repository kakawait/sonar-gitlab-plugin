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

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.CommitComment;
import org.gitlab.api.models.GitlabCommitDiff;
import org.gitlab.api.models.GitlabProject;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * Facade for all WS interaction with GitLab.
 */
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ScannerSide
public class GitLabApiFacade {

    private static final Logger logger = Loggers.get(GitLabApiFacade.class);

    private static final Pattern PATCH_CHUNK_PATTERN =
            Pattern.compile("^@@\\s-[0-9]+(?:,[0-9]+)?\\s\\+([0-9]+)(?:,[0-9]+)?\\s@@.*$", Pattern.MULTILINE);

    private static final String COMMIT_CONTEXT = "sonarqube";

    private final GitLabPluginConfiguration configuration;

    private GitlabAPI gitLabApi;

    private GitlabProject gitLabProject;

    private Map<String, List<CommitComment>> commitCommentPerRevision;

    private Map<String, Map<String, Set<Line>>> patchPositionByFile;

    private File gitBaseDir;

    public GitLabApiFacade(GitLabPluginConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Init GitLab connection and any necessary information that will be used during analysis.
     *
     * @param projectBaseDir project base directory.
     *
     * @throws IllegalStateException if unable to find git base dir or any errors when fetching GitLab API.
     */
    void init(File projectBaseDir) {
        gitBaseDir = findGitBaseDir(projectBaseDir);
        if (gitBaseDir == null) {
            throw new IllegalStateException(
                    String.format("Unable to find Git root directory. Is (%s) part of a Git repository?",
                            projectBaseDir));
        }

        gitLabApi = GitlabAPI.connect(configuration.url(), configuration.userToken())
                             .ignoreCertificateErrors(configuration.ignoreSSL());
        try {
            gitLabProject = getGitLabProject();
            commitCommentPerRevision = getCommitCommentsPerRevision(configuration.commitHashes());
            patchPositionByFile = getPatchPositionsToLineMapping(configuration.commitHashes());
            logger.debug("patch position by file and hashes {}", patchPositionByFile);
        } catch (IOException e) {
        	logger.error("Unable to perform GitLab WS operation", e);
            throw new IllegalStateException("Unable to perform GitLab WS operation", e);
        }
    }

    void createCommitStatus(String revision, String status, String statusDescription) {
        logger.info("Call to commit status update with: status={}, desc={}", status, statusDescription);
        try {
            if (GitLabPlugin.BUILD_INIT_STATES.contains(status)) {
                logger.info("Skipping commit status update since there are builds for this commit ({}) " +
                        "that will fail for consecutive update to this state ({}).", configuration.commitHashes(), status);
            } else {
                gitLabApi.createCommitStatus(gitLabProject, revision, status, configuration.referenceName(),
                        COMMIT_CONTEXT, null, statusDescription);
            }
        } catch (IOException e) {
            String msg = String.format("Unable to update commit status. [status=%s, project_id=%s, sha=%s, ref=%s, " +
                    "context=%s, ignore_ssl=%s, build_init_state=%s, description=%s]", status, gitLabProject.getId(),
                    revision, configuration.referenceName(), COMMIT_CONTEXT,
                    gitLabApi.isIgnoreCertificateErrors(), configuration.getBuildInitState(), statusDescription);
            throw new IllegalStateException(msg, e);
        }
    }


    Set<CommitComment> getCommitCommentsForFile(String revision, InputFile inputFile) {
        assertNotNull(revision, "revision must not be null");
        assertNotNull(inputFile, "inputFile must not be null");

        String path = getPath(inputFile);

        List<CommitComment> value = commitCommentPerRevision.get(revision);
        return Optional.ofNullable(value)
                       .orElse(Collections.emptyList())
                       .stream()
                       .filter(Objects::nonNull)
                       .filter(c -> path.equals(c.getPath()))
                       .collect(toSet());
    }

    boolean hasFile(InputFile inputFile) {
        return patchPositionByFile.values().stream().anyMatch(e -> e.containsKey(getPath(inputFile)));
    }

    Optional<String> getRevisionForLine(InputFile inputFile, int lineNumber) {
        String value = null;
        try {
            value = Files.readAllLines(inputFile.path()).get(lineNumber - 1);
        } catch (IOException e) {
            // do nothing
        }
        Line line = new Line(lineNumber, value);
        String path = getPath(inputFile);
        logger.debug("try to find revision for given file {} = {} on {}", inputFile, path, line);
        return patchPositionByFile
                .entrySet()
                .stream()
                .filter(e -> e.getValue().entrySet().stream()
                              .peek(v -> logger.debug("{} -> {} equals? {}", v.getKey(), v.getValue(), line))
                              .anyMatch(v -> {
                                  logger.debug("v.getKey().equals(\"" + path + "\") => " + v.getKey().equals(path));
                                  logger.debug("v.getValue().contains(\"" + line + "\") => " + v.getValue().contains(line));
                                  return v.getKey().equals(path) && v.getValue().contains(line);
                              }))
                .peek(e -> logger.debug("matches {}", e))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    String getGitLabUrl(String revision, InputFile inputFile, Integer line) {
        String url = null;
        if (inputFile != null) {
            String template = "%s/blob/%s/%s%s";
            url = String.format(template, gitLabProject.getWebUrl(), revision, getPath(inputFile),
                    line != null ? "#L" + line : "");
            logger.debug("GitLab url: {}", url);
        }
        return url;
    }

    void createGlobalComment(String comment) {
        try {
            String revision = configuration.commitHashes().get(0);
            gitLabApi.createCommitComment(gitLabProject.getId(), revision, comment, null, null, null);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to comment the commit (%s)", comment) , e);
        }
    }

    void createInlineComment(String revision, InputFile inputFile, Integer line, String body) {
        String path = getPath(inputFile);
        try {
            logger.debug("gitlab-api create commit comment with parameters: " +
                    "id={}, sha={}, note={}, path={}, line={}", gitLabProject.getId(), revision, body,
                    path, line.toString());
            gitLabApi.createCommitComment(gitLabProject.getId(), revision, body, path, line.toString(),
                    "new");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create or update review comment in file " + path
                    + " at line " + line, e);
        }
    }

    private String getPath(InputPath inputPath) {
        return new PathResolver().relativePath(gitBaseDir, inputPath.file());
    }

    private File findGitBaseDir(@Nullable File baseDir) {
        if (baseDir == null) {
            return null;
        }
        if (new File(baseDir, ".git").exists()) {
            return baseDir;
        }
        return findGitBaseDir(baseDir.getParentFile());
    }

    private GitlabProject getGitLabProject() throws IOException {
        if (configuration.projectId() == null) {
            throw new IllegalStateException("Missing required attribute: " + GitLabPlugin.GITLAB_PROJECT_ID);
        }

        GitlabProject project = gitLabApi.getProject(configuration.projectId());
        if (project != null) {
            return project;
        }

        Set<GitlabProject> projects = Optional
                .ofNullable(gitLabApi.getProjects())
                .orElseThrow(() -> new IllegalStateException("Unable to find projects within: " + configuration.url()))
                .stream()
                .filter(this::isMatchingProject)
                .collect(toSet());
        if (projects.isEmpty()) {
            throw new IllegalStateException("Unable to found project for " + configuration.projectId());
        }
        if (projects.size() > 1) {
            throw new IllegalStateException("Multiple projects found for " + configuration.projectId());
        }
        return projects.iterator().next();
    }

    private boolean isMatchingProject(GitlabProject project) {
        return configuration.projectId().equals(project.getId().toString())
                || configuration.projectId().equals(project.getPathWithNamespace())
                || configuration.projectId().equals(project.getHttpUrl())
                || configuration.projectId().equals(project.getSshUrl())
                || configuration.projectId().equals(project.getWebUrl())
                || configuration.projectId().equals(project.getNameWithNamespace());
    }

    /**
     * GitLab expect review comments to be added on "patch lines" (aka position) but not on file lines.
     * So we have to iterate over each patch and compute corresponding file line in order to later map issues
     * to the correct position.
     *
     * @return Map corresponding of Revision -> File path -> List of Position.
     *
     * @throws IOException If any issue when fetching GitLab API.
     */
    private Map<String, Map<String, Set<Line>>> getPatchPositionsToLineMapping(List<String> revisions) throws IOException {
        Map<String, Map<String, Set<Line>>> result = new HashMap<>();

        for (String revision : revisions) {
            result.put(revision,
                    gitLabApi.getCommitDiffs(gitLabProject.getId(), revision)
                             .stream()
                             .collect(toMap(GitlabCommitDiff::getNewPath, d -> getPositionsFromPatch(d.getDiff()))));
        }

        return result;
    }

    private Set<Line> getPositionsFromPatch(String patch) {
        Set<Line> positions = new HashSet<>();

        int currentLine = -1;
        for (String line : patch.split("\n")) {
            if (line.startsWith("@")) {
                Matcher matcher = PATCH_CHUNK_PATTERN.matcher(line);
                if (!matcher.matches()) {
                    throw new IllegalStateException("Unable to parse line:\n\t" + line + "\nFull patch: \n\t" + patch);
                }
                currentLine = Integer.parseInt(matcher.group(1));
            } else if (line.startsWith("+")) {
                positions.add(new Line(currentLine, line.replaceFirst("\\+", "")));
                currentLine++;
            } else if (line.startsWith(" ")) {
                // Can't comment line if not addition or deletion due to following bug
                // https://gitlab.com/gitlab-org/gitlab-ce/issues/26606
                currentLine++;
            }
        }

        return positions;
    }

    private Map<String, List<CommitComment>> getCommitCommentsPerRevision(List<String> revisions) throws IOException {
        assertNotNull(revisions, "revision must not be null");

        Map<String, List<CommitComment>> result = new HashMap<>();
        for (String revision : revisions) {
            result.put(revision, gitLabApi.getCommitComments(gitLabProject.getId(), revision));
        }
        return result;
    }

    private void assertNotNull(Object value, String errorMessage) {
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static class Line {

        private static final Logger logger = Loggers.get(Line.class);

        private Integer number;

        private String content;

        Line(Integer number, String content) {
            this.number = number;
            this.content = content;
        }

        @Override
        public String toString() {
            return "Line{" + "number=" + number +
                    ", content='" + content + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            logger.debug("equals? {} = {}", o, this);
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Line line = (Line) o;
            return Objects.equals(number, line.number) &&
                    Objects.equals(content, line.content);
        }

        @Override
        public int hashCode() {
            return Objects.hash(number, content);
        }
    }

}
