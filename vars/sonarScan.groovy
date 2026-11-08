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