// Each pod starts empty, so each group must check out the code again.
// expectedTag makes later pods prove they got the SAME commit as the first one.
def call(Map args = [:]) {
  def scmVars = checkout scm
  String commit = scmVars.GIT_COMMIT
  if (args.expectedTag && !commit.startsWith(args.expectedTag)) {
    error("Commit changed during the build: expected ${args.expectedTag}, got ${commit}")
  }
  return commit
}