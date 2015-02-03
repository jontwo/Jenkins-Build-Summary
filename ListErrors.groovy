//currentBuild = "All"
currentBuild = "Latest"
//currentBuild = 193

//currentJob = "All"
currentJob = "Superbreak_mobile"

currentConfig = "All"
//currentConfig = "Browser_Type=mobile,label=linux"
//currentConfig = "Browser_Type=desktop,label=linux"
//currentConfig = "Browser_Type=mobile,label=osx"
//currentConfig = "Browser_Type=desktop,label=osx"

currentKnownDefects = [
  "scenarios.hotel_add_rail",
  "scenarios.hotel_plus_rail",
  "scenarios.hotel_only",
]

removeKnownDefects = false
summaryOnly = false

// known errors
errors = [
  "asynchronous script timeout: result was not received in 60 seconds": 0,
  "{{{AssertionError :}}}": 0,
  "AssertionError : 20 not greater than or equal to": 0,
  "AssertionError : Element text should be": 0,
  "AssertionError : None == None": 0,
  "AssertionError : This test is not implemented": 0,
  "AssertionError : Timed out waiting for": 0,
  "AttributeError : 'module' object has no attribute": 0,
  "Duplicate user row in export": 0,
  "Element is not clickable at point": 0,
  "Element is not displayed": 0,
  "ElementNotVisibleException": 0,
  /Expected attribute: '(?:^|\\s)headerSortDown(?:\\s|$)'. Actual attribute: u'header'/: 0,
  /Expected attribute: '(?:^|\\s)headerSortDown(?:\\s|$)'. Actual attribute: u'header headerSortUp'/: 0,
  /Expected attribute: '(?:^|\\s)headerSortUp(?:\\s|$)'. Actual attribute: u'header'/: 0,
  /Expected attribute: '(?:^|\\s)headerSortUp(?:\\s|$)'. Actual attribute: u'header headerSortDown'/: 0,
  "found no elements within 30.00 seconds.  We expected 1.": 0,
  "HTTPError : HTTP Error 500: Internal Server Error": 0,
  "ImportError : cannot import name": 0,
  "IndexError : list index out of range": 0,
  "MismatchError": 0,
  "'NoneType' object has no attribute": 0,
  "Precisely one of the elements returned by": 0,
  "StaleElementReferenceException": 0,
  "URLError : <urlopen error [Errno 111] Connection refused>": 0,
  "ValueError : I/O operation on closed file": 0,
  "WebDriverException : Message: An unknown server-side error occurred while processing the command.": 0,
]
searchError = ''
//searchError = 'Other element would receive the click: <div class="ui-os-app-iframe-overlay"'

newErrors = [:]
errorTable = []
errorCount = 0
inErrorMessage = false

if (searchError != '') {
  println ('Looking for error: ' + searchError)
}
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
                  if (!summaryOnly && searchError == '') println('\t' + testName)
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

                  if (searchError == '') {
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

                    if (!summaryOnly) println("\t\t$errorMessage")
                    errorTable.add([build.number, errorMessage, errorCount, testName, config.name, fullMessage])
                  } else {
                    if (errorMessage.contains(searchError)) {
                      fullMessage = errorMessage
                      errorMessage = searchError
                      errorCount += 1
                      println ('\t' + testName)
                      errorTable.add([build.number, errorMessage, errorCount, testName, config.name, fullMessage])
                    }
                  }
                  errorMessage = ""
                }
              }
            }
            if (summaryOnly) {
              use (groovy.time.TimeCategory) {
                d = new Date(build.duration) - new Date(0)
                println(String.format("  Build: $build.number Time: $build.time Duration: %1d:%02d:%02d Errors: %d",
                                      d.hours, d.minutes, d.seconds, totalErrors))
              }
            }
            buildCount++
          }
        }
      }
    }
    // output results
    println('Summary:\n========')
    if (searchError == '') {
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
    } else {
      sum = errorCount
    }
    println('----------------\nTotal errors: ' + sum)
    println()
    // paste this into google docs sheet
    errorTable.each{println(it[0] + '\t' + it[1] + '\t' + it[2] + '\t' + it[3] + '\t' + it[4] + '\t' + it[5])}
    //errorTable.each{println(it[0] + '\t' + it[3])}
    println()
  }
}
