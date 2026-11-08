// Contract: container named containerName (default: kubectl) with wget.
// Addresses have no scheme on purpose (wget uses plain http), which also keeps
// the NoHttp rule of the app repo quiet if you ever copy them there.
def call(Map args = [:]) {
  String containerName = args.containerName ?: 'kubectl'
  String namespace     = args.namespace  ?: error('smokeTest: namespace is required')
  String service       = args.service    ?: 'petclinic'
  String deployment    = args.deployment ?: 'petclinic'
  String healthPath    = args.healthPath ?: '/actuator/health'
  String healthExpect  = args.healthExpect ?: '"status":"UP"'
  String dataPath      = args.dataPath   ?: ''
  String dataExpect    = args.dataExpect ?: ''
  int retries          = (args.retries ?: 6) as int
  int delaySeconds     = (args.delaySeconds ?: 10) as int
  boolean rollback     = args.containsKey('rollback') ? args.rollback : true

  String base = "${service}.${namespace}.svc.cluster.local"

  container(containerName) {
    withEnv(["HEALTH_URL=${base}${healthPath}", "HEALTH_EXPECT=${healthExpect}",
             "DATA_URL=${dataPath ? base + dataPath : ''}", "DATA_EXPECT=${dataExpect}",
             "RETRIES=${retries}", "DELAY=${delaySeconds}"]) {
      try {
        sh '''
          i=0; ok=0
          while [ "$i" -lt "$RETRIES" ]; do
            if wget -qO- "$HEALTH_URL" | grep -q "$HEALTH_EXPECT"; then ok=1; break; fi
            i=$((i+1)); sleep "$DELAY"
          done
          [ "$ok" = 1 ] || { echo "Health check failed: $HEALTH_URL"; exit 1; }

          if [ -n "$DATA_URL" ]; then
            wget -qO- "$DATA_URL" | grep -q "$DATA_EXPECT" || { echo "Data check failed: $DATA_URL"; exit 1; }
          fi
          echo "Smoke test passed"
        '''
      } catch (err) {
        echo "Smoke test failed: ${err.message}"
        if (rollback) {
          rollbackK8s(namespace: namespace, deployment: deployment, containerName: containerName)
        }
        throw err
      }
    }
  }
}