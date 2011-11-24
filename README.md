Groovy Plugin for Play Framework
================================

This is a Groovy plugin for the Play Framework -- it'll allow you to write your Play code in Groovy rather than Java (or Scala) and test it with Spock and Geb.

Done
----
* Groovy (1.8.3) compilation (with recompile on reload)
 * can cross-compile java and groovy sources
 * can put multiple classes in files (classes not tied to filenames)
* Spock tests
 * Treated as normal unit tests but with nicer syntax and better error output
* Geb tests
 * Can be used to do functional or selenium type testing (htmlunit)
 * can also visit external pages as part of a test

Usage
-----
This isn't as radical as the Play Scala plugin. You can still use JPA and structure your models in the same way as you would in Java... but you can cut down on line noise using Groovy's syntax, and exploit its dynamic nature when it's useful. Because it supports cross-compilation, you can also keep most of your app Java and add a few Groovy files (such as Spock tests) when you want.

Any work you need to do with collections in particular will benefit from Groovy's expressive syntax and methods. The [Differences from Java](http://groovy.codehaus.org/Differences+from+Java) page on the Groovy website has a great list of differences.

Take a look at the samples/blog app, which is a Groovyified version of the Play 'yabe' app.

### Accessors/mutators ###
Play has you set your model fields as public and then autogenerates get and set methods for them. In Groovy, you should not set a visibility modifier for your fields. The Groovy compiler will default these fields to private but create a get and set method for them, as Play does.

So while in standard Play you'd do this:

	public class Post extends Model {
		public String title;
	}
	...
	Post p = new Post();
	p.title = 'Hello';
	System.out.println(p.title);

In Groovy, do this:

	class Post extends Model {
		String title
	}
	...
	Post p = new Post()
	p.title = 'Hello'
	println(p.title)

In both cases, you can override the get/set methods with your own.

### Constructors ###
If you like, you can leave your models without a constructor and then instantiate them using the Groovy bean constructor, like so:

	class Post extends Model {
		String title
		String content
	}
	...
	Post p = new Post(title: 'Hello World', content: 'Lorem ipsum...')

But be aware this will reduce the amount of compile-time checking that can happen.

Tests
-----
Perhaps the most useful thing about this is the built-in [Spock](http://spockframework.org) and [Geb](http://www.gebish.org) support. Spock lets you write your unit and functional tests in a really expressive way, and formats errors in a monospaced, readable fashion.

You can also use Geb to do functional/selenium type testing. I'm still not sure exactly how useful it is, but it's there for you to play with.

Here's an example of a Spock test in play:

	def 'try to connect as a user'() {
		given:
		new User(email: 'bob@gmail.com', password: 'secret', fullname: 'Bob').save()

		expect:
		User.connect('bob@gmail.com', 'secret') != null
		User.connect('bob@gmail.com', 'wrong-password') == null
		User.connect('jane@gmail.com', 'secret') == null
	}

Todo
----
* More robust sample/testing apps for better test coverage
* Geb support is not finished yet
 * only supports GET requests
 * doesn't support cookies
 * doesn't return js/css yet, or get content types
* IDE support (might work now, I haven't tried yet)

Future
------
* JPA is great but doesn't look so Groovy. I wonder if I could get Grails' GORM to work...
