currentJob = "All"
//currentJob = "RegressionAssignmentsUK(Chrome,Linux)"
//currentJob = "highlevel"
//currentJob = "Cleanup"

for (job in Hudson.instance.items) {
  println("Examining job $job.name ")
  if (job.lastBuild != null && (job.name == currentJob || currentJob == "All")) {
    if (job.isBuildable()) {
      println("  Enabled")
    } else {
      println("  Disabled")
    }
    if (job.logRotator != null) {
      if (job.logRotator.getDaysToKeepStr() != "") {
        println("  Days to keep: " + job.logRotator.getDaysToKeepStr())
      }
      if (job.logRotator.getNumToKeepStr() != "") {
        println("  Builds to keep: " + job.logRotator.getNumToKeepStr())
      }
    }
  }
  println()
}
