package play.test

import org.openqa.selenium.htmlunit.HtmlUnitDriver
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.BrowserVersion

class PlayGroovyDriver extends HtmlUnitDriver {
	
	def PlayGroovyDriver() {
		super()
	}

	/*@Override void get(URL url) {
		

	}*/

	@Override WebClient newWebClient(BrowserVersion version) {
		return new PlayGroovyWebClient(version)
	}
}