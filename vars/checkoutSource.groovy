// checkoutSource: check out the source code in the current pod, and return the commit.
// Each pod starts with an empty workspace, so every group of work must check out again.
// Parameters:
//   expectedTag  optional   the commit tag from the first pod. When given, the function
//                           proves that this pod got the SAME commit.
// Returns: the full commit hash.
// Contract: the Jenkinsfile has options { skipDefaultCheckout(true) }, so this is the only checkout.
// On failure: error if someone pushed a new commit while the build was running and
//   this pod checked out a different one.
def call(Map args = [:]) {
  def scmVars = checkout scm
  String commit = scmVars.GIT_COMMIT
  if (args.expectedTag && !commit.startsWith(args.expectedTag)) {
    error("Commit changed during the build: expected ${args.expectedTag}, got ${commit}")
  }
  return commit
}