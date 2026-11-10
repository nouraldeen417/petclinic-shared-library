// buildImage: build the container image with Kaniko and save it as a tar file.
// It does NOT push. scanImage and pushImage read the same file.
// Parameters:
//   repoUrl        required   image name without a tag, for example docker.io/user/petclinic
//   tag            required   usually the git short SHA
//   containerName  optional   default: kaniko
// Contract:
//   - the pod has a container with that name (resources/pods/image.yaml)
//   - the workspace has a Dockerfile, and the files it copies (dist/app.jar)
//   - the output is $WORKSPACE/image.tar. scanImage and pushImage depend on this path.
// On failure: the stage fails and nothing is pushed.
def call(Map args = [:]) {
  String containerName = args.containerName ?: 'kaniko'
  String tag           = args.tag     ?: error('buildImage: tag is required')
  String repoUrl       = args.repoUrl ?: error('buildImage: repoUrl is required')

  container(containerName) {
    withEnv(["IMAGE=${repoUrl}:${tag}"]) {
      // --no-push with --tarPath writes the image to a file. --destination only
      // gives the image its name inside the tar.
      // --ignore-path: on kind, Kaniko fails when it cleans up a file that the node
      // mounts into every container.
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