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

@Entity
class Post extends Model {
	
	@Required
	String title

	@Required @As('yyyy-MM-dd')
	Date postedAt

	@Lob @Required @MaxSize(10000)
	String content

	@Required @ManyToOne
	User author

}