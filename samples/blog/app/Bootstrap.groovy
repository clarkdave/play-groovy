import play.*
import play.jobs.*
import play.test.*

import models.*

@OnApplicationStart
class Bootstrap extends Job {
	
	public void doJob() {
		// load initial data if db is empty
		if (User.count() == 0) {
			//println('Count: ' + User.count()) 
			Fixtures.load('initial-data.yml')
		}
	}
}