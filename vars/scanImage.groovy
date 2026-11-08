// Contract: the agent pod has a container named containerName (default: trivy).
// Reads $WORKSPACE/image.tar. Fails the build when findings of the chosen severity exist.
def call(Map args = [:]) {
  String containerName = args.containerName ?: 'trivy'
  String severity      = args.severity ?: 'HIGH,CRITICAL'
  String timeout       = args.timeout  ?: '15m'
  String cacheDir      = args.cacheDir ?: '/tmp/trivy'

  container(containerName) {
    withEnv(["SCAN_SEVERITY=${severity}", "SCAN_TIMEOUT=${timeout}", "SCAN_CACHE=${cacheDir}"]) {
      sh '''
        trivy image --input "$WORKSPACE/image.tar" \
          --scanners vuln \
          --severity "$SCAN_SEVERITY" \
          --ignore-unfixed \
          --exit-code 1 \
          --timeout "$SCAN_TIMEOUT" \
          --cache-dir "$SCAN_CACHE"
      '''
    }
  }
}