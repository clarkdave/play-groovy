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


	def 'post comments'() {
		setup: 'create a user and a post'
		User bob = new User(email: 'bob@gmail.com', password: 'secret', fullname: 'Bob').save()
		Post bobPost = new Post(bob, 'My first post', 'Hello world').save()

		and: 'create some comments'
		new Comment(bobPost, 'Jeff', 'Nice post').save()
		new Comment(bobPost, 'Tom', 'I knew that!').save()

		and: 'retreive comments for the post'
		def bobPostComments = Comment.find('byPost', bobPost).fetch()
		assert bobPostComments.size() == 2

		when:
		def firstComment = bobPostComments.get(0)

		then:
		firstComment != null
		firstComment.author == 'Jeff'
		firstComment.content == 'Nice post'
		firstComment.postedAt != null

		when:
		def secondComment = bobPostComments.get(1)

		then:
		secondComment != null
		secondComment.author == 'Tom'
		secondComment.content == 'I knew that!'
		secondComment.postedAt != null
	}


	def 'comment to post relationship'() {
		setup: 'create a user and post'
		User bob = new User(email: 'bob@gmail.com', password: 'secret', fullname: 'Bob').save()
		Post bobPost = new Post(bob, 'My first post', 'Hello world').save()

		when: 'post some comments'
		bobPost.addComment('Jeff', 'Nice post')
		bobPost.addComment('Tom', 'I knew that !')

		then: 'confirm counts'
		User.count() == 1
		Post.count() == 1
		Comment.count() == 2

		when: 'retrieve bob post'
		bobPost = Post.find('byAuthor', bob).first()

		then:
		bobPost != null
		bobPost.comments.size() == 2
		bobPost.comments.get(0).author == 'Jeff'

		when: 'delete the post'
		bobPost.delete()

		then: 'confirm all comments are deleted'
		User.count() == 1
		Post.count() == 0
		Comment.count() == 0
	}


	def 'full test'() {
		given: 'loading test fixtures'
		Fixtures.load('data.yml')

		expect:
		User.count() == 2
		Post.count() == 3
		Comment.count() == 3

		// user authentication tests
		User.connect('bob@gmail.com', 'secret') != null
		User.connect('jeff@gmail.com', 'secret') != null
		User.connect('jeff@gmail.com', 'badpassword') == null
		User.connect('tom@gmail.com', 'baduser') == null

		when: 'get bob posts'
		def bobPosts = Post.find('author.email', 'bob@gmail.com').fetch()

		then:
		bobPosts.size() == 2

		when: 'get comments on bob posts'
		def bobComments = Comment.find('post.author.email', 'bob@gmail.com').fetch()

		then:
		bobComments.size() == 3

		when: 'get most recent post'
		Post frontPost = Post.find('order by postedAt desc').first()

		then:
		frontPost != null
		frontPost.title == 'About the model layer'
		frontPost.comments.size() == 2

		when: 'adding a new comment'
		frontPost.addComment('Jim', 'Hello guys')

		then:
		frontPost.comments.size() == 3
		Comment.count() == 4
	}


	def 'test tags'() {
		given:
		User bob = new User(email: 'bob@gmail.com', password: 'secret', fullname: 'Bob').save()
		Post bobPost = new Post(bob, "My first post", "Hello world").save()
		Post anotherBobPost = new Post(bob, "My second post post", "Hello world").save()

		expect: 'no posts tagged with [Red]'
		Post.findTaggedWith('Red').size() == 0

		when: 'tagging the posts'
		bobPost.tagItWith('Red').tagItWith('Blue').save()
		anotherBobPost.tagItWith('Red').tagItWith('Green').save()

		then:
		Post.findTaggedWith('Red').size() == 2
		Post.findTaggedWith('Blue').size() == 1
		Post.findTaggedWith('Green').size() == 1
		Post.findTaggedWith('Red', 'Blue').size() == 1
		Post.findTaggedWith('Red', 'Green').size() == 1
		Post.findTaggedWith('Red', 'Green', 'Blue').size() == 0
		Post.findTaggedWith('Green', 'Blue').size() == 0

		when: 'get tag cloud and sort it'
		def cloud = Tag.getCloud()
		cloud.sort { a, b ->
			a.tag.toString().compareTo(b.tag.toString())
		}

		then:
		cloud == [
			[tag: 'Blue', pound: 1],
			[tag: 'Green', pound: 1],
			[tag: 'Red', pound: 2]
		]
	}
}