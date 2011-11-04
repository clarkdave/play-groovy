import org.junit.*
import java.util.*
import play.test.*
import models.*

import spock.lang.Specification

class BasicGroovyTest extends Specification {

	def 'checking basic maths'() {
		when:
		def a = 1
		def b = 2

		then:
		a + b == 2
	}

}
