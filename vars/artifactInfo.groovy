// Returns [version: ..., url: ...] for the jar of this build.
// Must run BEFORE publishJar, because publishJar changes pom.xml.
def call(Map args = [:]) {
  String tag      = args.tag ?: error('artifactInfo: tag is required')
  String nexusUrl = args.nexusUrl ?: env.NEXUS_URL ?: error('artifactInfo: nexusUrl is required, or set NEXUS_URL')
  String repo     = args.repo ?: 'maven-releases'

  String base     = mvnValue('project.version').replace('-SNAPSHOT', '')
  String group    = mvnValue('project.groupId')
  String artifact = mvnValue('project.artifactId')
  String version  = "${base}-${tag}"
  String path     = "${group.replace('.', '/')}/${artifact}/${version}/${artifact}-${version}.jar"

  return [version: version, url: "${nexusUrl}/repository/${repo}/${path}"]
}

// The expression is always a fixed text from this file, never user input.
private String mvnValue(String expression) {
  return sh(script: "mvn -B -q help:evaluate -Dexpression=${expression} -DforceStdout",
            returnStdout: true).trim()
}