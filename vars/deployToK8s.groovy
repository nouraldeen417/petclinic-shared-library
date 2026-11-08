// Contract: container named containerName (default: kubectl).
// manifestDir must contain service.yaml and deployment.yaml.
// deployment.yaml must contain the word IMAGE_PLACEHOLDER where the image goes.
// The namespace in the manifests must equal the namespace parameter, or kubectl refuses.
def call(Map args = [:]) {
  String containerName = args.containerName ?: 'kubectl'
  String namespace     = args.namespace   ?: error('deployToK8s: namespace is required')
  String manifestDir   = args.manifestDir ?: error('deployToK8s: manifestDir is required')
  String repoUrl       = args.repoUrl     ?: error('deployToK8s: repoUrl is required')
  String tag           = args.tag         ?: error('deployToK8s: tag is required')
  String deployment    = args.deployment  ?: 'petclinic'
  String timeout       = args.timeout     ?: '300s'

  container(containerName) {
    withEnv(["NS=${namespace}", "DIR=${manifestDir}", "IMAGE=${repoUrl}:${tag}",
             "DEPLOYMENT=${deployment}", "TIMEOUT=${timeout}"]) {
      try {
        sh '''
          kubectl apply -n "$NS" -f "$DIR/service.yaml"
          sed "s|IMAGE_PLACEHOLDER|$IMAGE|g" "$DIR/deployment.yaml" | kubectl apply -n "$NS" -f -
          kubectl rollout status deployment/"$DEPLOYMENT" -n "$NS" --timeout="$TIMEOUT"
        '''
      } catch (err) {
        echo "Deploy to ${namespace} failed: ${err.message}"
        rollbackK8s(namespace: namespace, deployment: deployment, containerName: containerName)
        throw err
      }
    }
  }
}