// Contract: the agent pod has a container named containerName (default: tools) with wget.
// dest must match the path the Dockerfile copies from.
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
  archiveArtifacts artifacts: dest, fingerprint: true
}