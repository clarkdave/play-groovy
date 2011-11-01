package models

import javax.persistence.*

import play.data.binding.*
import play.data.validation.*
import play.db.jpa.Model

@Entity
class User extends Model {
	
	@Required
	String username

}