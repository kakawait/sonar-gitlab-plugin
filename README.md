Sonar GitLab Plugin
===================

Forked from https://github.com/akrevenya/sonar-gitlab-plugin

# Goal

Add to each **commit** GitLab in a global commentary on the new anomalies added by this **commit** and add comment lines of modified files.

Comment commits:
![Comment commits](doc/comment_commits.jpg)

Comment line:
![Comment line](doc/comment_line.jpg)

Add build line:
![Add buids](doc/builds.jpg)

# Usage

For SonarQube >=5.4:

- Download last version https://github.com/kakawait/sonar-gitlab-plugin/releases/download/1.9.0/sonar-gitlab-plugin-1.9.0.jar
- Copy jar file in extensions directory `SONARQUBE_HOME/extensions/plugins`
- Restart SonarQube

# Command line

Example :

``` shell
mvn --batch-mode verify sonar:sonar -Dsonar.analysis.mode=preview -Dsonar.issuesReport.console.enable=true  -Dsonar.host.url=${SONAR_HOST_URL} -Dsonar.gitlab.url=${CI_PROJECT_URL%$CI_PROJECT_PATH} -Dsonar.analysis.mode=preview -Dsonar.issuesReport.console.enable=true -Dsonar.gitlab.commit_hashes=$(git log --pretty=format:%H origin/master..$CI_BUILD_REF | tr '\n' ',') -Dsonar.gitlab.ref_name=${CI_BUILD_REF_NAME} -Dsonar.gitlab.project_id=${CI_PROJECT_ID} -Dsonar.gitlab.user_token=${GITLAB_SONAR_USER_TOKEN} -Dsonar.gitlab.failure_notification_mode=status-code 
```

| Variable | Comment | Type |
| -------- | ----------- | ---- |
| sonar.gitlab.url | GitLab url | Administration, Variable |
| sonar.gitlab.max_global_issues | Maximum number of anomalies to be displayed in the global comment |  Administration, Variable |
| sonar.gitlab.user_token | Token of the user who can make reports on the project, either global or per project |  Administration, Project, Variable |
| sonar.gitlab.project_id | Project ID in GitLab or internal id or namespace + name or namespace + path or url http or ssh url or url or web | Project, Variable |
| sonar.gitlab.commit_hases | Hashes of the commits to be comment | Variable |
| sonar.gitlab.ref_name | Branch name or reference of the commit | Variable |
| sonar.gitlab.failure_notification_mode | Failure mode. Can be "commit-status" or "status-code" | 
| sonar.gitlab.ignore_ssl | Ignore SSL error when contacting GitLab API | 

- Administration : **Settings** globals in SonarQube
- Project : **Settings** of project in SonarQube
- Variable : In an environment variable or in the `pom.xml` either from the command line with` -D`

# Configuration

- In SonarQube: Administration -> General Settings -> GitLab -> **Reporting**. Set GitLab Url and Token

![Sonar settings](doc/sonar_settings.jpg)

- In SonarQube: Project Administration -> General Settings -> GitLab -> **Reporting**. Set project identifier in GitLab

![Sonar settings](doc/sonar_project_settings.jpg)
