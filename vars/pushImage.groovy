// pushImage: push the scanned image to the registry and add a second tag.
// Parameters:
//   repoUrl        required   image name without a tag
//   tag            required   the git short SHA (the tag Kubernetes uses)
//   timeStamp      required   the second tag, the UTC build time for people to read
//   credentialsId  optional   default: dockerhub-creds (username and access token)
//   containerName  optional   default: crane
// Contract:
//   - the pod has a container with that name (resources/pods/image.yaml)
//   - it pushes $WORKSPACE/image.tar, written by buildImage
//   - call it only after scanImage, so a vulnerable image never reaches the registry
// How the second tag works: crane tag adds a name to the same image on the registry.
//   Nothing is uploaded twice.
// On failure: the stage fails on a bad token or a missing tar file.
def call(Map args = [:]) {
  String containerName = args.containerName ?: 'crane'
  String credentialsId = args.credentialsId ?: 'dockerhub-creds'
  String repoUrl       = args.repoUrl   ?: error('pushImage: repoUrl is required')
  String tag           = args.tag       ?: error('pushImage: tag is required')
  String timeStamp     = args.timeStamp ?: error('pushImage: timeStamp is required')

  container(containerName) {
    withCredentials([usernamePassword(credentialsId: credentialsId,
                     usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
      withEnv(["IMAGE=${repoUrl}:${tag}", "EXTRA_TAG=${timeStamp}"]) {
        // Single quotes: the shell reads the variables, so the token never appears in
        // the Groovy text. --password-stdin keeps it out of the process list.
        sh '''
          echo "$DH_PASS" | crane auth login index.docker.io -u "$DH_USER" --password-stdin
          crane push "$WORKSPACE/image.tar" "$IMAGE"
          crane tag "$IMAGE" "$EXTRA_TAG"
        '''
      }
    }
  }
}