// scanImage: scan the image file with Trivy and fail on serious findings.
// Parameters:
//   severity       optional   default: HIGH,CRITICAL
//   timeout        optional   default: 15m (the default of Trivy, 5 minutes, was too short)
//   cacheDir       optional   default: /tmp/trivy
//   containerName  optional   default: trivy
// Contract:
//   - the pod has that container (resources/pods/image.yaml)
//   - it reads $WORKSPACE/image.tar, written by buildImage
// Choices in the command:
//   --scanners vuln    only vulnerabilities. Secret scanning of a big jar is slow.
//   --ignore-unfixed   only findings that have a fix. Nothing with a fix is hidden.
//   --exit-code 1      makes the stage fail when something is found.
// On failure: the build stops here, before pushImage.
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