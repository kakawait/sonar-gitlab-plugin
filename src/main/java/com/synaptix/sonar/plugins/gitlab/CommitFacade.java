/*
 * SonarQube :: GitLab Plugin
 * Copyright (C) 2016-2016 Talanlabs
 * gabriel.allaigre@talanlabs.com
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

import org.apache.commons.io.IOUtils;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabCommitDiff;
import org.gitlab.api.models.GitlabProject;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Facade for all WS interaction with GitLab.
 */
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@BatchSide
public class CommitFacade {

    private static final Logger logger = Loggers.get(CommitFacade.class);

    static final String COMMIT_CONTEXT = "sonarqube";

    private final GitLabPluginConfiguration config;
    private File gitBaseDir;
    private GitlabAPI gitLabAPI;
    private GitlabProject gitLabProject;
    private Map<String, Set<Integer>> patchPositionMappingByFile;

    public CommitFacade(GitLabPluginConfiguration config) {
        this.config = config;
    }

    public void init(File projectBaseDir) {
        if (findGitBaseDir(projectBaseDir) == null) {
            throw new IllegalStateException(String.format("Unable to find Git root directory. Is (%s) part of a Git repository?", projectBaseDir));
        }
        gitLabAPI = GitlabAPI.connect(config.url(), config.userToken()).ignoreCertificateErrors(config.ignoreSSL());
        try {
            gitLabProject = getGitLabProject();

            patchPositionMappingByFile = mapPatchPositionsToLines(gitLabAPI.getCommitDiffs(gitLabProject.getId(), config.commitSHA()));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to perform GitLab WS operation", e);
        }
    }

    private File findGitBaseDir(@Nullable File baseDir) {
        if (baseDir == null) {
            return null;
        }
        if (new File(baseDir, ".git").exists()) {
            this.gitBaseDir = baseDir;
            return baseDir;
        }
        return findGitBaseDir(baseDir.getParentFile());
    }

    private GitlabProject getGitLabProject() throws IOException {
        if (config.projectId() == null) {
            throw new IllegalStateException("Missing required attribute: " + GitLabPlugin.GITLAB_PROJECT_ID);
        }
        List<GitlabProject> projects = gitLabAPI.getProjects();
        if (projects == null) {
            throw new IllegalStateException("Unable to find projects under this location: " + config.url());
        }
        List<GitlabProject> res = new ArrayList<>();
        for(GitlabProject project : projects) {
            if (config.projectId().equals(project.getId().toString()) || config.projectId().equals(project.getPathWithNamespace()) || config.projectId().equals(project.getHttpUrl())
                    || config.projectId().equals(project.getSshUrl()) || config.projectId().equals(project.getWebUrl()) || config.projectId().equals(project.getNameWithNamespace())) {
                logger.debug("found matching project with project id: {}", project.getId());
                res.add(project);
            }
        }
        if (res.isEmpty()) {
            throw new IllegalStateException("Unable found project for " + config.projectId());
        }
        if (res.size() > 1) {
            throw new IllegalStateException("Multiple found projects for " + config.projectId());
        }
        return res.get(0);
    }

    private static Map<String, Set<Integer>> mapPatchPositionsToLines(List<GitlabCommitDiff> diffs) throws IOException {
        Map<String, Set<Integer>> patchPositionMappingByFile = new HashMap<>();
        for (GitlabCommitDiff file : diffs) {
            Set<Integer> patchLocationMapping = new HashSet<>();
            patchPositionMappingByFile.put(file.getNewPath(), patchLocationMapping);
            String patch = file.getDiff();
            if (patch == null) {
                continue;
            }
            processPatch(patchLocationMapping, patch);
        }
        return patchPositionMappingByFile;
    }

    private static void processPatch(Set<Integer> patchLocationMapping, String patch) throws IOException {
        int currentLine = -1;
        for (String line : IOUtils.readLines(new StringReader(patch))) {
            if (line.startsWith("@")) {
                // http://en.wikipedia.org/wiki/Diff_utility#Unified_format
                Matcher matcher = Pattern.compile("@@\\p{Space}-[0-9]+(?:,[0-9]+)?\\p{Space}\\+([0-9]+)(?:,[0-9]+)?\\p{Space}@@.*").matcher(line);
                if (!matcher.matches()) {
                    throw new IllegalStateException("Unable to parse patch line " + line + "\nFull patch: \n" + patch);
                }
                currentLine = Integer.parseInt(matcher.group(1));
            } else if (line.startsWith("-")) {
                // Skip removed lines
            } else if (line.startsWith("+") || line.startsWith(" ")) {
                // Count added and unmodified lines
                patchLocationMapping.add(currentLine);
                currentLine++;
            } else if (line.startsWith("\\")) {
                // I'm only aware of \ No newline at end of file
                // Ignore
            }
        }
    }

    public void createOrUpdateSonarQubeStatus(String status, String statusDescription) {
        try {
            gitLabAPI.createCommitStatus(gitLabProject, config.commitSHA(), status, config.refName(), COMMIT_CONTEXT, null, statusDescription);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to update commit status (%s)", status), e);
        }
    }

    public boolean hasFile(InputFile inputFile) {
        return patchPositionMappingByFile.containsKey(getPath(inputFile));
    }

    public boolean hasFileLine(InputFile inputFile, int line) {
        return hasFile(inputFile) && patchPositionMappingByFile.get(getPath(inputFile)).contains(line);
    }

    public String getGitLabUrl(InputFile inputFile, Integer issueLine) {
        if (inputFile != null) {
            String path = getPath(inputFile);
            String rtn = gitLabProject.getWebUrl() + "/blob/" + config.commitSHA() + "/" + path + (issueLine != null ? ("#L" + issueLine) : "");
            logger.info("Gitlab URL: {}", rtn);
            return rtn;
        }
        return null;
    }

    public void createOrUpdateReviewComment(InputFile inputFile, Integer line, String body) {
        String fullpath = getPath(inputFile);
        //System.out.println("Review : "+fullpath+" line : "+line);
        try {
            gitLabAPI.createCommitComment(gitLabProject.getId(),config.commitSHA(), body, fullpath, line.toString(), "new");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create or update review comment in file " + fullpath + " at line " + line, e);
        }
    }

    private String getPath(InputPath inputPath) {
        return new PathResolver().relativePath(gitBaseDir, inputPath.file());
    }

    public void addGlobalComment(String comment) {
        try {
            gitLabAPI.createCommitComment(gitLabProject.getId(),config.commitSHA(), comment, null, null, null);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to comment the commit (%s)", comment) , e);
        }
    }
}
