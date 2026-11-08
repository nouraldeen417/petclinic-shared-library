// Contract: the agent pod has a container named containerName (default: kubectl)
// and a ServiceAccount that may patch deployments in the namespace.
// It never fails the build. The caller rethrows the original error.
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