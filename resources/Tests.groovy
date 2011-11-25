import play.test.*
import models.*

class BasicTest extends SpockTest {

	'run this pointless test'() {
		when:
		def hello = 1

		then:
		hello == 1
	}