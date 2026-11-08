// Contract: the agent pod has a container named containerName (default: kaniko).
// Writes $WORKSPACE/image.tar and does NOT push. scanImage and pushImage read the same file.
def call(Map args = [:]) {
  String containerName = args.containerName ?: 'kaniko'
  String tag           = args.tag     ?: error('buildImage: tag is required')
  String repoUrl       = args.repoUrl ?: error('buildImage: repoUrl is required')

  container(containerName) {
    withEnv(["IMAGE=${repoUrl}:${tag}"]) {
      sh '''
        /kaniko/executor \
          --context="dir://$WORKSPACE" \
          --dockerfile="$WORKSPACE/Dockerfile" \
          --destination="$IMAGE" \
          --no-push \
          --tarPath="$WORKSPACE/image.tar" \
          --ignore-path=/product_uuid
      '''
    }
  }
}