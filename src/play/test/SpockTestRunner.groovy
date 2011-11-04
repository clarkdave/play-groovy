package play.test

import org.spockframework.runtime.Sputnik

import org.junit.runner.notification.RunNotifier

class SpockTestRunner {
	
	static def runJunitClass(Class<BaseTest> clazz) {
		
		def notifier = new RunNotifier()
		def sputnik = new Sputnik(clazz)
		sputnik.run(notifier)
	}
}