package models

import javax.persistence.*

import play.data.binding.*
import play.data.validation.*
import play.db.jpa.Model


@Entity
class User extends Model {

	@Email @Required
	String email

	@Required
	String password

	String fullname
	boolean isAdmin

	static User connect(String email, String password) {
		find('byEmailAndPassword', email, password).first()
	}

	String toString() {
		email
	}
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

	@OneToMany(mappedBy='post', cascade=CascadeType.ALL)
	List<Comment> comments

	@ManyToMany(cascade=CascadeType.PERSIST)
	Set<Tag> tags

	def Post(User author, String title, String content) { 
		this.comments = new ArrayList<Comment>()
		this.tags = new TreeSet(); 
		this.author = author
		this.title = title
		this.content = content
		this.postedAt = new Date()
	}

	Post addComment(String author, String content) {
		Comment newComment = new Comment(this, author, content)
		comments.add(newComment)
		save()
	}

	Post previous() {
		Post.find('postedAt < ? order by postedAt desc', postedAt).first()
	}

	Post next() {
		Post.find('postedAt > ? order by postedAt asc', postedAt).first()
	}
}

@Entity
class Comment extends Model {
	
	@Required
	String author

	@Required
	Date postedAt

	@Lob @Required @MaxSize(1000)
	String content

	@ManyToOne @Required
	Post post

	def Comment(Post post, String author, String content) {
		this.post = post
		this.author = author
		this.content = content
		this.postedAt = new Date()
	}

	String toString() {
		content.length() > 50 ? content.substring(0, 50) + '...' : content
	}
}

@Entity
class Tag extends Model implements Comparable<Tag> {
	
	@Required
	String name

	private Tag(String name) {
		this.name = name
	}

	static Tag findOrCreateByName(String name) {
		Tag.find('byName', name).first() ?: new Tag(name)
	}

	String toString() {
		name
	}

	int compareTo(Tag tag) {
		name.compareTo(tag.name)
	}
}