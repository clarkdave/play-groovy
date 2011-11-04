package play.test

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.WebResponse
import com.gargoylesoftware.htmlunit.WebRequestSettings

class PlayGroovyWebClient extends WebClient {
	
	def PlayGroovyWebClient(version) {
		super(version)
	}

	@Override private WebResponse loadWebResponseFromWebConnection(
			final WebRequestSettings webRequestSettings,
			final int nbAllowedRedirections) throws IOException {
			
		println 'Loading web response'

		super.loadWebResponseFromWebConnection(webRequestSettings, 
			nbAllowedRedirections)
	}
}