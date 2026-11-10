// sonarScan: send the code analysis to SonarQube Cloud and wait for the quality gate.
// Parameters:
//   org            required   the organization key (not a secret)
//   project        required   the project key (not a secret)
//   credentialsId  optional   default: sonar-cloud-token (Secret text, the only secret here)
//   hostUrl        optional   default: the SonarQube Cloud address. Change it for a
//                             self-hosted server.
// Contract:
//   - the default container has Maven, and mavenSettings already ran in this pod
//   - mavenTest ran before, so the JaCoCo coverage report exists
//   - Automatic Analysis is turned off for the project in SonarQube Cloud
// Choices:
//   - the scanner reads SONAR_TOKEN by itself, so the token is not on the command line
//   - qualitygate.wait=true makes the scan wait for the gate and fail the build if the
//     gate fails. This avoids a webhook from the server back to Jenkins.
//   - the plugin version is pinned. Check Maven Central for a newer one now and then.
// On failure: the build stops before any jar or image is made. A wrong organization key
//   or an expired token gives "Not authorized or organization not found".
def call(Map args = [:]) {
  String org           = args.org     ?: error('sonarScan: org is required')
  String project       = args.project ?: error('sonarScan: project is required')
  String credentialsId = args.credentialsId ?: 'sonar-cloud-token'
  String hostUrl       = args.hostUrl ?: 'https://sonarcloud.io'

  withCredentials([string(credentialsId: credentialsId, variable: 'SONAR_TOKEN')]) {
    withEnv(["SCAN_ORG=${org}", "SCAN_PROJECT=${project}", "SCAN_HOST=${hostUrl}"]) {
      sh '''
        mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar \
          -Dsonar.host.url="$SCAN_HOST" \
          -Dsonar.organization="$SCAN_ORG" \
          -Dsonar.projectKey="$SCAN_PROJECT" \
          -Dsonar.qualitygate.wait=true
      '''
    }
  }
}