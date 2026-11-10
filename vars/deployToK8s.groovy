// deployToK8s: deploy the application to one namespace and wait until it is ready.
// If the rollout fails, it rolls back and then fails the build.
// Parameters:
//   namespace      required   target namespace, for example petclinic-dev
//   manifestDir    required   folder with service.yaml and deployment.yaml
//   repoUrl        required   image name without a tag
//   tag            required   image tag
//   deployment     optional   name of the Deployment. Default: petclinic
//   timeout        optional   how long to wait for the rollout. Default: 300s
//   containerName  optional   default: kubectl
// Contract:
//   - the pod has a container with that name, and its service account may patch
//     deployments in the namespace (resources/pods/deploy.yaml)
//   - manifestDir/deployment.yaml has the word IMAGE_PLACEHOLDER where the image goes
//   - the namespace written in the manifests equals the namespace parameter, or kubectl refuses
//   - only the two named files are applied. A database in the same folder is never touched.
// On failure: rollbackK8s runs, then the original error is thrown, so the build stays red.
// Note: the local variable named timeout hides the pipeline step timeout(...) inside this function.
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
        // apply only submits the new state. rollout status waits, and fails if the
        // new pods never become ready.
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