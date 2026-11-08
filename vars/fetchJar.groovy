// dest must match the path the Dockerfile copies from.
def call(Map args = [:]) {
  String url  = args.url ?: error('fetchJar: url is required')
  String dest = args.dest ?: 'dist/app.jar'

  withEnv(["ART_URL=${url}", "DEST=${dest}"]) {
    sh '''
      mkdir -p "$(dirname "$DEST")"
      curl -fsS -o "$DEST" "$ART_URL"
      ls -la "$DEST"
    '''
  }
  archiveArtifacts artifacts: dest, fingerprint: true
}