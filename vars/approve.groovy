def call(Map args = [:]) {
  String message = args.message ?: 'Promote to the next environment?'
  String ok      = args.ok ?: 'Deploy'
  int minutes    = (args.timeoutMinutes ?: 30) as int

  timeout(time: minutes, unit: 'MINUTES') {
    input message: message, ok: ok
  }
}