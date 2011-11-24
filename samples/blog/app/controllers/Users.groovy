package controllers

import play.*
import play.mvc.*

@Check('admin')
@With(Secure.class)
class Users extends CRUD {
	
}