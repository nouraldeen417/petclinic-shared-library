def call() {
  try {
    sh 'mvn -B test jacoco:report'
  } finally {
    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
    recordCoverage(tools: [[parser: 'JACOCO']])
  }
}