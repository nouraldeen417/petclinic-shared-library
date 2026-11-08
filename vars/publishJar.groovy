// Contract with settings.xml: it reads NEXUS_USER and NEXUS_PASS from the environment,
// and the server id must match serverId.
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