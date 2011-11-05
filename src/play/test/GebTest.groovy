package play.test

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import geb.Browser
import geb.spock.GebSpec

import play.Play
import play.mvc.Http
import play.mvc.Http.Request
import play.mvc.Http.Response
import play.Invoker
import play.Invoker.InvocationContext
import play.mvc.ActionInvoker

import org.openqa.selenium.htmlunit.HtmlUnitDriver
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.util.WebConnectionWrapper
import com.gargoylesoftware.htmlunit.WebResponse
import com.gargoylesoftware.htmlunit.WebResponseData
import com.gargoylesoftware.htmlunit.WebRequest

class GebTest extends GebSpec {
	
	//static ExecutorService testExecutor = Executors.newSingleThreadExecutor()
	def lastStatusCode = null

	Browser createBrowser() {
		def browser = super.createBrowser()
		// TODO: make sure the http.port config is set when it's default
		browser.baseUrl = 'http://localhost:' + Play.configuration.getProperty('http.port')
		browser.driver = getDriver()
		return browser
	}

	int getStatusCode() {
		return browser.driver.getStatusCode()
	}

	def getDriver() {
		
		return new HtmlUnitDriver() {
			@Override WebClient modifyWebClient(WebClient client) {
				GebTest.wrapWebConnection(client)
				return client
			}

			int getStatusCode() {
				def page = lastPage()
				if (page == null) return null
				return page.getWebResponse().getStatusCode()
			}
		}
	}

	static def wrapWebConnection(client) {
		new WebConnectionWrapper(client) {

			@Override public WebResponse getResponse(WebRequest request)
				throws IOException {

				def url = request.getUrl()
				def response = null
				
				// TODO: make sure this works with default port
				if (url.getPort().toString() == Play.configuration.getProperty('http.port')) {

					// this is probably a request to a Play controller, so instead
					// of doing an actual http request we'll invoke the specified
					// controller/action and return the result
					def playRequest = GebTest.newRequest()

					playRequest.method = 'GET'
					playRequest.url = url.getPath()
					playRequest.path = url.getPath()
					playRequest.querystring = url.getQuery()?:''
					playRequest.body = new ByteArrayInputStream(new byte[0])
					// TODO: handle cookies

					def playResponse = GebTest.newResponse()

					GebTest.makeRequest(playRequest, playResponse)

					def data = new WebResponseData(
						playResponse.out.toByteArray(),
						playResponse.status, '',
						[]
					)

					response = new WebResponse(data, request, 0)

				} else {
					response = super.getResponse(request)
				}

				return response
			}
		}
	}

	static void makeRequest(request, response) {
		/*final Future invocationResult = testExecutor.submit(new Invoker.Invocation() {
			@Override void execute() throws Exception {
				ActionInvoker.invoke(request, response)
			}

			@Override InvocationContext getInvocationContext() {
				ActionInvoker.resolve(request, response)
				return new InvocationContext(Http.invocationType,
					request.invokedMethod.getAnnotations(),
					request.invokedMethod.getDeclaringClass().getAnnotations())
			}
		})*/

		// TODO: removed the asynchronous execution for now as it was
		// breaking static file requests. At the moment any requests for static
		// stuff (/public) will just return as 200 OKs with empty strings
		// stuff still works but it means anything testing css/js functionality
		// won't work

		try {
			//invocationResult.get(30, TimeUnit.SECONDS)
			ActionInvoker.invoke(request, response)
			// TODO: cookies?
			response.out.flush()
		}
		catch (Exception e) {
			throw new RuntimeException(e)
		}
	}

	static Request newRequest() {
		// delegate to Play's FunctionalTest which should
		// give us a default play Request object
		FunctionalTest.newRequest()
	}

	static Response newResponse() {
		FunctionalTest.newResponse()
	}
}