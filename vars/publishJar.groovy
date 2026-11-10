// publishJar: upload the jar to Nexus, once per version.
// Parameters:
//   version        required   from artifactInfo
//   url            required   from artifactInfo (the address the file will have)
//   nexusUrl       optional   default: env.NEXUS_URL
//   repo           optional   default: maven-releases
//   serverId       optional   default: nexus-releases (must match the id in settings.xml)
//   credentialsId  optional   default: nexus-ci
// Contract:
//   - the default container has Maven and curl, and mavenSettings already ran in this pod
//   - settings.xml reads NEXUS_USER and NEXUS_PASS, which are set from the credential below
//   - Nexus does not accept the same release version twice
// How it works: if the file already exists, it skips the upload. Otherwise it sets the
//   version in pom.xml (workspace only, nothing is committed) and runs mvn deploy.
// Known limit: mvn deploy builds the jar again, with the commit in its version. The tests
//   ran earlier on the same commit, but the uploaded file is a new build of that commit.
// On failure: the stage fails on wrong credentials (401), a missing right (403), and so on.
def call(Map args = [:]) {
  String version       = args.version ?: error('publishJar: version is required')
  String url           = args.url     ?: error('publishJar: url is required')
  String nexusUrl      = args.nexusUrl ?: env.NEXUS_URL ?: error('publishJar: nexusUrl is required, or set NEXUS_URL')
  String repo          = args.repo ?: 'maven-releases'
  String serverId      = args.serverId ?: 'nexus-releases'
  String credentialsId = args.credentialsId ?: 'nexus-ci'

  withCredentials([usernamePassword(credentialsId: credentialsId,
                   usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
    withEnv(["ART_VERSION=${version}", "ART_URL=${url}",
             "DEPLOY_REPO=${serverId}::${nexusUrl}/repository/${repo}/"]) {
      sh '''
        if curl -fsI "$ART_URL" > /dev/null; then
          echo "Version $ART_VERSION is already in Nexus, skipping upload"
        else
          mvn -B -q versions:set -DnewVersion="$ART_VERSION" -DgenerateBackupPoms=false
          mvn -B deploy -DskipTests -DaltDeploymentRepository="$DEPLOY_REPO"
        fi
      '''
    }
  }
}