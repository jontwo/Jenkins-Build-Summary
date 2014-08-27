//currentBuild = "All"
currentBuild = "Latest"
//currentBuild = 2077

//currentJob = "All"
currentJob = "RegressionAssignmentsUK(Chrome,Linux)"
//currentJob = "highlevel"
//currentJob = "Cleanup"

currentConfig = "All"
//currentConfig = "Browser_OS=linux,Browser_Type=Chrome,Build_Type=malaysia,Environment_Type=clean"
//currentConfig = "Browser_OS=linux,Build_Type=uk-primary,Environment_Type=clean"

currentKnownDefects = [
  "regression.assignments.filter_open_assignments_on_issue_date",
  "regression.assignments.filter_open_assignments_on_due_date",
  "regression.assignments.on_click_selected_breadcrumb_link_returns_users_to_relevant_page",
  "regression.assignments.enter_and_delete_user_wall_messages",
  "regression.assignments.search_and_select_image_through_text_activity_widget",
  "regression.assignments.complete_assignment_with_user_wall_message",
  "regression.assignments.open_help",
]

removeKnownDefects = true
summaryOnly = false

// known errors
errors = [
  "Action '_get_spinner' did not fail": 0,
  "asynchronous script timeout: result was not received in 60 seconds": 0,
  "{{{AssertionError :}}}": 0,
  "AssertionError : None == None": 0,
  "AssertionError : Timed out waiting for": 0,
  "AttributeError : 'module' object has no attribute": 0,
  "css selector 'table.closed tbody tr.assign td input.check' found 5 elements": 0,
  "css selector 'table.table tbody tr:nth-child(1) td a.student-view-popover' found 2 elements": 0,
  "css selector 'form > div.textarea.cke_editable.cke_editable_inline' found 3 elements": 0,
  "Duplicate user row in export": 0,
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
            if (!summaryOnly) println("  Build: $build.number Time: $build.time")
            fullMessage = ''
            testName = ''
      totalErrors = 0
            for (line in build.getLog(Integer.MAX_VALUE)) {
              // get test name
              if (line.startsWith('ERROR:') || line.startsWith('FAIL:')) {
                testName = line.replace('ERROR: ', '').replace('FAIL: ', '')
                if (removeKnownDefects && currentKnownDefects.contains(testName)) {
                  testName = ''
                } else {
                  if (!summaryOnly) println('\t' + testName)
                }
                errorMessage = ""
                inErrorMessage = false
                totalErrors++
              }
              if (testName != '') {
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
                      if (!summaryOnly) println('\t\t***New error***')
                      newErrors.put(errorMessage, 1)
                      errorCount = 1
                    }
                    fullMessage = errorMessage
                  }

                  if (!summaryOnly) println('\t\t' + errorMessage)
                  errorTable.add([build.number, errorMessage, errorCount, testName, fullMessage])
                  errorMessage = ""
                }
              }
            }
            if (summaryOnly) println("  Build: $build.number Time: $build.time Errors: " + totalErrors)
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
