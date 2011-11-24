package controllers

import play.*
import play.mvc.*
import play.data.validation.*

import models.*

@With(Secure.class)
class Admin extends Controller {
	
	@Before
	static void setConnectedUser() {
		if (Security.isConnected()) {
			User user = User.find('byEmail', Security.connected()).first()
			renderArgs.put('user', user.fullname)
		}
	}

	static void index() {
		List<Post> posts = Post.find('author.email', Security.connected()).fetch()
		render(posts)
	}

	static void form(Long id) {
		if (id != null) {
			Post post = Post.findById(id)
			render(post)
		}
		render()
	}

	static void save(Long id, String title, String content, String tags) {
		Post post

		if (id == null) {
			User author = User.find('byEmail', Security.connected()).first()
			post = new Post(author, title, content)
		}
		else {
			post = Post.findById(id)
			post.title = title
			post.content = content
			post.tags.clear()
		}

		// set tags list
		tags.split('\\s+').each { tag ->
			if (tag.trim().length() > 0) {
				post.tags.add(Tag.findOrCreateByName(tag))
			}
		}

		validation.valid(post)

		if (validation.hasErrors()) {
			render('@form', post)
		}

		// save and go to index
		post.save()
		index()
	}
}