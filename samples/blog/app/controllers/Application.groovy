package controllers

import play.*
import play.mvc.*

import java.util.*

import models.*

class Application extends Controller {

	static void index() {
		Logger.info('Loading index')
		render()
	}

}