def call(String fileId = 'maven-settings') {
  configFileProvider([configFile(fileId: fileId, variable: 'MAVEN_SETTINGS')]) {
    sh 'mkdir -p ~/.m2 && cp "$MAVEN_SETTINGS" ~/.m2/settings.xml'
  }
}