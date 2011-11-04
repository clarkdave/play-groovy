import play.test.*
import geb.spock.GebSpec

class ApplicationGroovyTest extends GebSpec {
	
	def 'check index page is OK'() {
		when:

		browser.baseUrl = 'http://google.com'
		browser.driver = new PlayGroovyDriver()

		browser.go()
		println title

		then:
		title != 'Application error'
	}
}