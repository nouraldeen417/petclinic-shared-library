// mavenSettings: put the Maven settings file in place, so Maven downloads through Nexus.
// The file is stored in Jenkins as a managed file (Config File Provider plugin).
// Parameters:
//   fileId  optional   id of the managed file. Default: maven-settings
// Contract:
//   - the default container of the pod has Maven (it runs in resources/pods/maven.yaml)
//   - the file uses the variables NEXUS_URL, NEXUS_USER and NEXUS_PASS from the environment
// It must run once at the start of every build, because each build gets a new pod and
// ~/.m2/settings.xml is Maven's default place, so later mvn commands use it with no flag.
// On failure: the stage fails if the managed file does not exist.
def call(String fileId = 'maven-settings') {
  configFileProvider([configFile(fileId: fileId, variable: 'MAVEN_SETTINGS')]) {
    sh 'mkdir -p ~/.m2 && cp "$MAVEN_SETTINGS" ~/.m2/settings.xml'
  }
}