// rollbackK8s: go back to the previous version of a Deployment.
// It is called by deployToK8s and smokeTest when they fail. Callers rethrow their own
// error, so the build stays red.
// Parameters:
//   namespace      required   the namespace to roll back
//   deployment     optional   default: petclinic
//   timeout        optional   how long to wait for the old version. Default: 180s
//   containerName  optional   default: kubectl
// Contract: the pod has that container, and its service account may patch deployments
//   in the namespace.
// On failure: it never fails the build. It prints the reason. The first deploy of an
//   application has no earlier version, so there is nothing to go back to.
def call(Map args = [:]) {
  String containerName = args.containerName ?: 'kubectl'
  String namespace     = args.namespace ?: error('rollbackK8s: namespace is required')
  String deployment    = args.deployment ?: 'petclinic'
  String timeout       = args.timeout ?: '180s'

  container(containerName) {
    withEnv(["NS=${namespace}", "DEPLOYMENT=${deployment}", "TIMEOUT=${timeout}"]) {
      try {
        sh '''
          kubectl rollout undo deployment/"$DEPLOYMENT" -n "$NS"
          kubectl rollout status deployment/"$DEPLOYMENT" -n "$NS" --timeout="$TIMEOUT"
        '''
      } catch (err) {
        echo "Rollback failed, or there is no earlier version to go back to: ${err.message}"
      }
    }
  }
}