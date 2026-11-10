// fetchJar: download the jar of this build from Nexus into the workspace.
// The image is built from this stored file, not from a file left behind by a build.
// Parameters:
//   url            required   the address from artifactInfo
//   dest           optional   where to save it. Default: dist/app.jar
//   containerName  optional   default: tools
// Contract:
//   - the pod has a container with that name and wget (busybox in image.yaml)
//   - anonymous read is enabled in Nexus, so no login is needed
//   - dest must match the path the Dockerfile copies from
// On failure: wget exits with an error on a 404, so a missing jar fails the stage.
def call(Map args = [:]) {
  String containerName = args.containerName ?: 'tools'
  String url  = args.url ?: error('fetchJar: url is required')
  String dest = args.dest ?: 'dist/app.jar'

  container(containerName) {
    withEnv(["ART_URL=${url}", "DEST=${dest}"]) {
      sh '''
        mkdir -p "$(dirname "$DEST")"
        wget -q -O "$DEST" "$ART_URL"
        ls -la "$DEST"
      '''
    }
  }
  // Keep the jar with the build record. The fingerprint links it to the build.
  archiveArtifacts artifacts: dest, fingerprint: true
}