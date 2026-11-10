# petclinic-shared-library

A Jenkins shared library for building, scanning, and deploying a containerized Java application on Kubernetes. It is used by the [PetClinic DevSecOps platform](https://github.com/YOUR_GITHUB_USER/spring-petclinic) (replace the link with your fork), and it is written so that another application can use the same functions with its own parameters.

Each function does one job, takes named parameters, and keeps secrets in Jenkins credentials. The pipeline file of an application stays short, and says what runs and in which pod. The functions hold the how.

## Contents

- [How to use it](#how-to-use-it)
- [Function catalog](#function-catalog)
- [Pod templates](#pod-templates)
- [What the library expects from Jenkins](#what-the-library-expects-from-jenkins)
- [Conventions](#conventions)
- [Changing the library safely](#changing-the-library-safely)
- [Known limits and next steps](#known-limits-and-next-steps)

## How to use it

1. In Jenkins, open **Manage Jenkins, System, Global Trusted Pipeline Libraries** and add a library named `petclinic-shared-library`. Use default version `main`, retrieval method **Modern SCM**, **Git**, and the address of this repository. Leave **Load implicitly** off.
2. Start the Jenkinsfile of your application with this line. The underscore at the end is needed:

   ```groovy
   @Library('petclinic-shared-library@main') _
   ```

3. Copy [`examples/Jenkinsfile.example`](examples/Jenkinsfile.example), and change the values in its `environment` block.

For a stable pipeline, use a release tag instead of `main`, for example `@v1.0.0`. A tag cannot change, so a later edit of the library cannot break a working pipeline.

## Function catalog

Every function is in `vars/`. The file name is the function name, and the method inside is always `call`. Every function has a header comment with its parameters, its contract, and what happens on failure.

| Function | What it does | Required parameters | Runs in container | Credential |
| --- | --- | --- | --- | --- |
| `checkoutSource` | Checks out the code, returns the commit, and can prove it is the same commit as in an earlier pod | none | Jenkins agent | none |
| `mavenSettings` | Puts the Maven settings file in place, so downloads go through Nexus | none | default (Maven) | none (a managed file) |
| `artifactInfo` | Works out the jar version and its Nexus address | `tag` | default (Maven) | none |
| `mavenTest` | Runs the tests and records the test and coverage reports | none | default (Maven) | none |
| `sonarScan` | Sends the code analysis to SonarQube Cloud and waits for the quality gate | `org`, `project` | default (Maven) | `sonar-cloud-token` |
| `publishJar` | Uploads the jar to Nexus, once per version | `version`, `url` | default (Maven, curl) | `nexus-ci` |
| `fetchJar` | Downloads the jar from Nexus into the workspace | `url` | `tools` | none (anonymous read) |
| `buildImage` | Builds the image with Kaniko and writes `image.tar`, without pushing | `repoUrl`, `tag` | `kaniko` | none |
| `scanImage` | Scans `image.tar` with Trivy and fails on serious findings | none | `trivy` | none |
| `pushImage` | Pushes `image.tar` and adds a second tag | `repoUrl`, `tag`, `timeStamp` | `crane` | `dockerhub-creds` |
| `deployToK8s` | Applies the manifests, waits for the rollout, rolls back on failure | `namespace`, `manifestDir`, `repoUrl`, `tag` | `kubectl` | none (a service account) |
| `smokeTest` | Checks health and optionally data, rolls back on failure | `namespace` | `kubectl` | none |
| `rollbackK8s` | Goes back to the previous version of a Deployment. Never fails the build | `namespace` | `kubectl` | none (a service account) |
| `approve` | Waits for a person to confirm | none | none (no agent) | none |

How the functions work together in a normal pipeline:

```
checkoutSource -> mavenSettings -> artifactInfo -> mavenTest -> sonarScan -> publishJar
checkoutSource -> fetchJar -> buildImage -> scanImage -> pushImage
checkoutSource -> deployToK8s -> smokeTest        (deployToK8s and smokeTest call rollbackK8s on failure)
approve -> checkoutSource -> deployToK8s -> smokeTest
```

Three links between functions are worth knowing:

- `buildImage`, `scanImage`, and `pushImage` all use the file `$WORKSPACE/image.tar`. They must run in the same pod.
- `artifactInfo` must run before `publishJar`, because `publishJar` changes `pom.xml` in the workspace.
- `mavenTest` must run before `sonarScan`, because Sonar reads the coverage report that `mavenTest` creates.

## Pod templates

The templates are in `resources/pods/`. A function loads one with `libraryResource('pods/NAME.yaml')`, and the Kubernetes plugin turns it into an agent pod.

| Template | Containers | Service account | Used for |
| --- | --- | --- | --- |
| `maven.yaml` | `maven` | see the note below | Build, test, analyse, publish the jar |
| `image.yaml` | `tools`, `kaniko`, `trivy`, `crane` | none | Build, scan, and push the image |
| `deploy.yaml` | `kubectl` | `jenkins-deployer` | Deploy and smoke test |

The container names are a contract. The functions call `container('kaniko')` and so on, so a renamed container gives a "container not found" error.

Pods do not share a disk. A pod starts with an empty workspace, so each group of work checks out the code again, and the jar travels through Nexus.

Note on `maven.yaml`: it currently sets the deploy service account, which this pod does not need. Removing it is listed under known limits.

## What the library expects from Jenkins

| Item | Value | Used by |
| --- | --- | --- |
| Credential, username and password | `dockerhub-creds` | `pushImage` |
| Credential, secret text | `sonar-cloud-token` | `sonarScan` |
| Credential, username and password | `nexus-ci` | `publishJar` |
| Managed file (Config File Provider) | `maven-settings` | `mavenSettings` |
| Global environment variable | `NEXUS_URL` | `artifactInfo`, `publishJar`, and the settings file |
| Plugins | Kubernetes, Pipeline, Git, Credentials Binding, Coverage, Config File Provider, Timestamper, JUnit | Various |
| Cluster | A service account `jenkins-deployer` with a Role in each target namespace | `deploy.yaml` |

The `maven-settings` file reads `NEXUS_URL`, `NEXUS_USER`, and `NEXUS_PASS` from the environment, and its server id must match the `serverId` of `publishJar` (default `nexus-releases`).

Credential IDs are parameters with defaults, so a different Jenkins can use other IDs without changing the library.

## Conventions

These rules keep the functions predictable:

1. **Named parameters in a map.** Required ones use `?: error('function: name is required')`, so a missing value stops the build with a clear message.
2. **Defaults for the usual case**, such as container names and credential IDs.
3. **Values reach the shell through `withEnv`** and the shell text is in single quotes. Groovy variables are not written into the shell text, so quoting stays simple and secrets stay masked.
4. **Secrets never enter the library.** Only credential IDs do.
5. **The function owns its tool.** `container('kaniko')` is inside `buildImage`, so a caller cannot forget it. The header comment lists the container as a contract.
6. **Cleanup in `finally`**, so reports are recorded even when tests fail.
7. **Failure handling next to the cause.** `deployToK8s` and `smokeTest` roll back by themselves, so a failed scan can never trigger a rollback.

## Changing the library safely

- Test a change on a branch. In an application Jenkinsfile, use `@Library('petclinic-shared-library@my-branch') _`. The Jenkins **Replay** option on a build lets you try a change without committing.
- Release with a tag: `git tag v1.1.0` and `git push origin v1.1.0`. Applications that use the tag do not change until you change the tag in their Jenkinsfile.
- This is a trusted library, so its code runs without the script sandbox. Anyone who can push to this repository can run code in Jenkins. Limit who can push, and review changes.
- After a change in a function that others depend on, run a full pipeline, not only the stage you edited.

## Known limits and next steps

- `maven.yaml` sets the `jenkins-deployer` service account. Only `deploy.yaml` needs it, so removing the line from `maven.yaml` gives the build pod no rights in the cluster.
- `publishJar` runs `mvn deploy`, which builds the jar again with the commit in its version. A stricter design builds and tests once, then uploads that exact file.
- The tags of the `crane` container (and the base images in Jenkins and Nexus manifests) are not pinned to exact versions.
- `deployToK8s` and `rollbackK8s` use a local variable named `timeout`, which hides the pipeline step of the same name inside those functions.
- The functions are written for one application layout (a Maven project, an image built from `dist/app.jar`, manifests with the word `IMAGE_PLACEHOLDER`). A second, different application will show what needs to become a parameter.
- A pipeline template (the whole flow behind one call) and a second small application that uses the library unchanged are planned.