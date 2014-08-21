currentBuild = "All"
//currentBuild = "Latest"
//currentBuild = 2077

//currentJob = "All"
currentJob = "RegressionAssignmentsUK(Chrome,Linux)"
//currentJob = "highlevel"
//currentJob = "Cleanup"

currentConfig = "All"
//currentConfig = "Browser_OS=linux,Browser_Type=Chrome,Build_Type=malaysia,Environment_Type=clean"
//currentConfig = "Browser_OS=linux,Build_Type=uk-primary,Environment_Type=clean"

currentTest = "duplicate_site_with_multiple_tasks"

// known errors
errors = [
  "Action '_get_spinner' did not fail": 0,
  "asynchronous script timeout: result was not received in 60 seconds": 0,
  "AssertionError : None == None": 0,
  "AssertionError : Timed out waiting for": 0,
  "AttributeError : 'module' object has no attribute": 0,
  "css selector 'table.closed tbody tr.assign td input.check' found 5 elements": 0,
  "css selector 'table.table tbody tr:nth-child(1) td a.student-view-popover' found 2 elements": 0,
  "css selector 'form > div.textarea.cke_editable.cke_editable_inline' found 3 elements": 0,
  "Element is not clickable at point": 0,
  "Element is not displayed": 0,
  "ElementNotVisibleException": 0,
  "found no elements within 30.00 seconds.  We expected 1.": 0,
  "HTTPError : HTTP Error 500: Internal Server Error": 0,
  "ImportError : cannot import name": 0,
  "IndexError : list index out of range": 0,
  "MismatchError": 0,
  "'NoneType' object has no attribute": 0,
  "Precisely one of the elements returned by": 0,
  "StaleElementReferenceException": 0,
  "ValueError : I/O operation on closed file": 0,
]
newErrors = [:]
errorTable = []
inErrorMessage = false
inTraceback = false

println('Looking for errors in ' + currentTest)

for (job in Hudson.instance.items) {
  println("Examining job $job.name ")
  if (job.lastBuild != null && (job.name == currentJob || currentJob == "All")) {
    println("  Result: $job.lastBuild.result")
    for (config in job.getAllJobs()) {
      if (currentConfig == "All" || currentConfig == config.name) {
        println("  Config: $config.name")
        buildCount = 0
        for (build in config.getBuilds()) {
          if (currentBuild == "All" || (currentBuild == "Latest" && buildCount == 0) || currentBuild == build.number) {
            // show results for build
            println("  Build: $build.number Time: $build.time")
            fullMessage = ''
            testName = ''
            for (line in build.getLog(Integer.MAX_VALUE)) {
              // get test name
              if (line.startsWith('ERROR:') || line.startsWith('FAIL:')) {
                testName = line.replace('ERROR: ', '').replace('FAIL: ', '')
                errorMessage = ""
                inErrorMessage = false
              }
              if (testName.contains(currentTest)) {
                // start of exception message
                if (line.startsWith('Original exception:')) {
                  inErrorMessage = true
                }
                // middle of exception message
                if (inErrorMessage) {
                  errorMessage += line
                }
                // end of exception message
                if (line.endsWith('}}}') && inErrorMessage) {
                  inErrorMessage = false

                  // check for known error
                  found = false
                  for (err in errors.keySet()) {
                    if (errorMessage.contains(err)) {
                      errors[err] += 1
                      errorCount = errors[err]
                      found = true
                      // only keep summary message
                      fullMessage = errorMessage
                      errorMessage = err
                    }
                  }
                  if (!found) {
                    if (newErrors.keySet().contains(errorMessage)) {
                      newErrors[errorMessage] +=1
                      errorCount = newErrors[errorMessage]
                    } else {
                      println('\t\t***New error***')
                      newErrors.put(errorMessage, 1)
                      errorCount = 1
                    }
                    fullMessage = errorMessage
                  }

                  println('\t\t' + errorMessage)
                  errorTable.add([build.number, errorMessage, errorCount, testName, fullMessage])
                  errorMessage = ""
                }
                // parse traceback for file and line number
                if (line.startsWith('  ')) {
                  if (line.startsWith('  File')) {
                    if (line.contains(currentTest)) {
                      inTraceback = true
                    } else {
                      inTraceback = false
                    }
                  }
                  if (inTraceback) {
                    println('\t\t' + line)
                  }
                }
              }
            }
            buildCount++
          }
        }
      }
    }
    // output results
    println('Summary:\n========')
    sum = 0
    errors.each{
      if (it.value > 0) {
        println(it.value + ': ' + it.key)
        sum += it.value
      }
    }
    println(newErrors.size() + ' new errors found')
    newErrors.each{
      println(it.value + ': ' + it.key)
      sum += it.value
    }
    println('----------------\nTotal errors: ' + sum)
    println()
    // paste this into google docs sheet
    errorTable.each{println(it[0] + '\t' + it[1] + '\t' + it[2] + '\t' + it[3] + '\t' + it[4])}
    println()
  }
}
