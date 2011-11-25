import org.junit.*
import play.test.*
import play.mvc.*
import play.mvc.Http.*
import models.*

class ApplicationTest extends FunctionalTest {

    @Test
    public void testThatIndexPageWorks() {
        Response response = GET('/')
        assertIsOk(response)
        assertContentType('text/html', response)
        assertCharset(play.Play.defaultWebEncoding, response)
    }
    
}