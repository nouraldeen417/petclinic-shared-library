// Contract: the agent pod has a container named containerName (default: crane).
// Pushes $WORKSPACE/image.tar as repoUrl:tag, then adds a second tag (timeStamp).
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
        sh '''
          echo "$DH_PASS" | crane auth login index.docker.io -u "$DH_USER" --password-stdin
          crane push "$WORKSPACE/image.tar" "$IMAGE"
          crane tag "$IMAGE" "$EXTRA_TAG"
        '''
      }
    }
  }
}