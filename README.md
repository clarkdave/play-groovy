Groovy Plugin for Play Framework
================================

This is a Groovy plugin for the Play Framework -- it'll allow you to write your Play code in Groovy rather than Java (or Scala)

Done
----
* Groovy compilation (with recompile on reload)
 * can cross-compile java and groovy sources
 * can put multiple classes in files (classes not tied to filenames)
* Spock tests
 * Treated as normal unit tests but with nicer syntax and better error output
* Geb tests
 * Can be used to do functional or selenium type testing (htmlunit)
 * can also visit external pages as part of a test

Todo
----
* More robust sample/testing apps for better test coverage
* Geb support is not finished yet
 * only supports GET requests
 * doesn't support cookies
 * doesn't return js/css yet, or get content types
