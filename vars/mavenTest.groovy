// mavenTest: run the unit and Spring tests, and make the coverage report.
// Parameters: none.
// Contract:
//   - the default container has Maven
//   - the project has the JaCoCo plugin, so jacoco:report writes target/site/jacoco/jacoco.xml
//     (sonarScan reads that file, so this must run BEFORE sonarScan)
//   - the Jenkins plugins JUnit and Coverage are installed
// On failure: the stage fails when a test fails. The reports are recorded anyway (finally).
def call() {
  try {
    sh 'mvn -B test jacoco:report'
  } finally {
    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
    recordCoverage(tools: [[parser: 'JACOCO']])
  }
}