package controllers

import play.*
import play.mvc.*
import play.data.validation.*
import play.cache.*;
import play.libs.*;

import java.util.*

import models.*

class Application extends Controller {

	static void index() {
		Post frontPost = Post.find('order by postedAt desc').first()
		List<Post> olderPosts = Post.find('order by postedAt desc').from(1).fetch(1)
		render(frontPost, olderPosts)
	}

	static void show(Long id) {
		Post post = Post.findById(id)
		String randomId = Codec.UUID()
		render(post, randomId)
	}

	static void postComment(Long postId,
			@Required(message='Author required') String author,
			@Required(message='A message is required') String content,
			@Required(message='Please type the code') String code,
			String randomId) {
		
		Post post = Post.findById(postId)
		if (Play.id != 'test') {
			validation.equals(code, Cache.get(randomId)).message('Invalid code. Please type it again')
		}
		if (validation.hasErrors()) {
			render('Application/show.html', post, randomId)
		}
		post.addComment(author, content)
		flash.success('Thanks for posting %s', author)
		Cache.delete(randomId)
		show(postId)
	}

}