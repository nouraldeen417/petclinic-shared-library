// approve: pause the pipeline until a person confirms, or until the time runs out.
// Call it from a stage that has NO agent block, so waiting holds no pod and no memory.
// Parameters:
//   message         optional   text shown to the person. Default: 'Promote to the next environment?'
//   ok              optional   label of the confirm button. Default: 'Deploy'
//   timeoutMinutes  optional   how long to wait. Default: 30
// Contract: none.
// On failure: when nobody answers in time, the build stops. Jenkins may show it as
//   aborted, not failed.
def call(Map args = [:]) {
  String message = args.message ?: 'Promote to the next environment?'
  String ok      = args.ok ?: 'Deploy'
  int minutes    = (args.timeoutMinutes ?: 30) as int

  timeout(time: minutes, unit: 'MINUTES') {
    input message: message, ok: ok
  }
}