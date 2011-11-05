import play.test.*

class ApplicationGroovyTest extends GebTest {
	
	def 'check index page is OK'() {
		when:
		go '/'

		then:
		statusCode == 200
		title != 'Application error'
	}
}