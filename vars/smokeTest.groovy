// smokeTest: check that a deployed application is alive, and optionally that it reads data.
// Parameters:
//   namespace      required   where the application runs
//   service        optional   default: petclinic
//   deployment     optional   default: petclinic (used for the rollback)
//   healthPath     optional   default: /actuator/health
//   healthExpect   optional   text that must appear. Default: "status":"UP"
//   dataPath       optional   a second address that returns data from the database
//   dataExpect     optional   text that must appear in that answer
//   retries        optional   default: 6
//   delaySeconds   optional   wait between tries. Default: 10
//   rollback       optional   default: true. Set false to test the function alone.
//   containerName  optional   default: kubectl
// Contract: the pod has that container with wget and grep. The address is built from
//   the service name and the namespace (service.namespace.svc.cluster.local).
// Addresses have no scheme on purpose (wget uses plain http), which also keeps
//   the NoHttp rule of the app repo quiet if you ever copy them there.
// How it works: the health check retries, because the Service may answer slowly right
//   after a rollout. The data check proves the application really uses its database.
// On failure: it rolls back (unless rollback is false), then fails the build.
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