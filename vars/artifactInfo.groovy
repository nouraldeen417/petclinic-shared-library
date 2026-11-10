// artifactInfo: work out the version and the Nexus address of this build's jar.
// It only computes values and changes nothing.
// Parameters:
//   tag       required   usually the git short SHA, the same tag as the image
//   nexusUrl  optional   default: env.NEXUS_URL (a Jenkins global variable)
//   repo      optional   default: maven-releases
// Returns: a map [version: ..., url: ...]
//   version = the pom version without -SNAPSHOT, a dash, and the tag (for example 4.0.0-ac98d7d)
//   url     = the Maven repository path: the group with dots turned into slashes, then
//             artifact, version, and file name
// Contract: runs in the default container, which must have Maven, and it reads the
//   pom.xml in the workspace. It must run BEFORE publishJar, because publishJar changes pom.xml.
// On failure: error if the tag or the Nexus address is missing.
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