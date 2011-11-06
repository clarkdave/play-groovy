import play.test.*
import models.*

class BasicTest extends SpockTest {
	
	def setup() {
		Fixtures.deleteDatabase()
	}

	def 'create and retrieve a user'() {
		when:
		new User(email: 'bob@gmail.com', password: 'secret', fullname: 'Bob').save()
		def bob = User.find('byEmail', 'bob@gmail.com').first()

		then:
		bob != null
		bob.fullname == 'Bob'
	}


	def 'try to connect as a user'() {
		given:
		new User(email: 'bob@gmail.com', password: 'secret', fullname: 'Bob').save()

		expect:
		User.connect('bob@gmail.com', 'secret') != null
		User.connect('bob@gmail.com', 'wrong-password') == null
		User.connect('jane@gmail.com', 'secret') == null
	}


	def 'create a post'() {
		when:
		User bob = new User(email: 'bob@gmail.com', password: 'secret', fullname: 'Bob').save()
		new Post(bob, 'My first post', 'Hello world').save()

		assert Post.count() == 1
		def bobPosts = Post.find('byAuthor', bob).fetch()

		assert bobPosts.size() == 1
		def firstPost = bobPosts.get(0)

		then:
		firstPost != null
		firstPost.author == bob
		firstPost.title == 'My first post'
		firstPost.content == 'Hello world'
		firstPost.postedAt != null
	}
}