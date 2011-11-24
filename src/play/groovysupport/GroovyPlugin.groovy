package play.groovysupport

import java.lang.reflect.Modifier

import play.*
import play.test.*
import play.test.TestEngine.TestResults
import play.exceptions.*
import play.vfs.VirtualFile
import play.classloading.ApplicationClasses.ApplicationClass
import play.groovysupport.compiler.*

import org.junit.Assert

import spock.lang.Specification

class GroovyPlugin extends PlayPlugin {
	
	def compiler
	def currentSources = [:]

	@Override
	void onLoad() {

		compiler = new GroovyCompiler(Play.applicationPath,
			new File(Play.modules['groovy'].getRealFile(), 'lib'),
			System.getProperty('java.class.path')
				.split(System.getProperty('path.separator')) as List,
			Play.tmpDir
		)

		onConfigurationRead()

		/**
		 * The Play TestEngine only grabs classes which are assignable from
		 * org.junit.Assert -- Spock tests don't extend from JUnit, so we need
		 * to modify the TestEngine.allUnitTests method to ensure it picks up
		 * Specification classes too
		 */
		TestEngine.metaClass.static.allUnitTests = {
			Play.classloader.getAssignableClasses(Assert.class)
				.plus(Play.classloader.getAssignableClasses(Specification.class))
				.findAll {
					!Modifier.isAbstract(it.getModifiers()) &&
					!FunctionalTest.class.isAssignableFrom(it) &&
					!GebTest.class.isAssignableFrom(it)
				}
		}

		TestEngine.metaClass.static.allGebTests << {
			Play.classloader.getAssignableClasses(GebTest.class)
				.findAll { !Modifier.isAbstract(it.getModifiers()) }
		}
		
		Logger.info('Groovy support is active')
	}

	@Override
	void onConfigurationRead() {
		// TODO: investigate how necessary this is...
		Play.configuration.put("play.bytecodeCache", "false")
	}

	@Override
	TestResults runTest(Class<BaseTest> testClass) {

		null
	}

	@Override
	boolean detectClassesChange() {

		try {
			def sources = sources()
			def javaResult = updateJava(sources.java)
			def groovyResult = updateGroovy(sources.groovy)

			if (groovyResult) {
				updateInternalApplicationClasses(groovyResult)
			}

			if (javaResult || groovyResult) {
				// force reload for time being whenever we compile stuff
				reload()
			}
		} catch (RuntimeException e) {
			throw e
		} catch (CompilationErrorException e) {
			throw compilationException(e.compilationError)
		}

		return true
	}

	@Override
	boolean compileSources() {

		try {
			def sources = sources()

			updateJava(sources.java)
			def result = updateGroovy(sources.groovy)

			if (result) {
				updateInternalApplicationClasses(result)
			}
		} catch (CompilationErrorException e) {
			throw compilationException(e.compilationError)
		}

		return true
	}

	/**
	 * Update Play's internal ApplicationClasses
	 */
	def updateInternalApplicationClasses(CompilationResult result) {
		
		// remove deleted classes
		result.removedClasses.each {
			Play.classes.remove(it.name)
		}

		// add/update other classes
		result.updatedClasses.each {
			def appClass = new ApplicationClass()
			appClass.name = it.name
			appClass.javaFile = new VirtualFile(it.source)
			// TODO: if the javaFile can't be located for some reason
			// it will cause serious problems later on, so it needs to 
			// be handled here
			appClass.refresh()
			appClass.compiled(it.code)
			Play.classes.add(appClass)
		}
	}

	def reload() {

		// throwing a RuntimeException will force Play to reload
		// and reload all our classes.
		// We could try to use the HotswapAgent here but it's probably not
		// a big problem to just force a reload of the code every time (Scala
		// plugin seems to do it this way too)

		// TODO: I *think* that every reload triggers the compileSources() hook,
		// which means stuff is getting compiled twice at the moment since we
		// aren't doing a check of lastmodified time before recompiles
		// not a huge prob for now but needs fixing
		throw new RuntimeException('Need reload')
	}

	def compilationException(compilationError) {
		if (compilationError.source) {
			new CompilationException(
				VirtualFile.open(compilationError.source), compilationError.message,
				compilationError.line, compilationError.start, compilationError.end
			)
		} else {
			new CompilationException(compilationError.message)
		}
	}

	/**
	 * return a list of all the paths of currently loaded modules
	 * except the groovy module
	 */
	def loadedModuleNames = {
		Play.modules.findAll { name, f -> name != 'groovy'}.collect { name, file -> 
			file.getRealFile().toString().toLowerCase() }
	}

	/**
	 * get a map of our Groovy and Java sources... because the Groovy Compiler
	 * isn't always so great at compiling Java files, for modules we want to use
	 * a Java compiler instead, since Groovy's had some problems (for example, the
	 * inner annotation definitions in the CRUD module caused issues)
	 *
	 * So, any .java files which are part of a module will be considered Java
	 * sources. Any Java files within the Play app will be compiled by Groovy as
	 * usual to ensure cross-compilation support works fine.
	 */
	def sources() {
		def sources = [:]
		Play.javaPath.each {
			GroovyCompiler.getSourceFiles(it.getRealFile()).each { f -> sources[f] = f.lastModified()}
		}
		
		def javaSources = sources.findAll { src, lm ->
			src = src.toString().toLowerCase()
			for (modName in loadedModuleNames()) {
				if (src.startsWith(modName) && src.endsWith('.java')) return true
			}
			return false
		}
		def groovySources = sources.findAll { f, lm -> !(f in javaSources.keySet()) }

		return [
			groovy: groovySources,
			java: javaSources.findAll { file, lastModified ->
				// we'd like to override the testrunner controller with our own, so
				// let's make sure it never gets compiled... bit of a hack but I couldn't
				// see any other way to override a controller in an included module
				!(file.toString() =~ /(?i)testrunner.+TestRunner\.java/)
			}
		]
	}

	def updateJava(sources) {

		if (currentSources?.java != sources) {

			def result = []

			sources.each { file, time ->
				def src = file.toString()
				// remove .java at the end
				src = src.substring(0, src.length()-5)

				// we need to turn this source into a class name, but we can't just
				// assume it's in /modules/ (it could be anywhere...) so run through
				// the loaded modules and if it matches, remove the matching path
				for (modName in loadedModuleNames()) {
					if (src.startsWith(modName)) {
						src = src.substring(modName.length())
						break
					}
				}
				// is it OK to assume the file is in /app? it might not be, but
				// for now it seems to work
				if (src.startsWith('/app')) {
					src = src.substring(5)
				}
				else {
					println 'Not sure what to do with this source. This is probably a bug'
				}
				
				def className = src.replace(File.separator, '.')
				def appClass = Play.classes.getApplicationClass(className)

				// TODO: refresh based on lastmodified timestamp, etc..
				appClass.refresh()

				if (appClass.compile() == null) {
					Play.classes.classes.remove(appClass.name)
				} else {
					result << appClass
				}
			}

			currentSources.java = sources

			return result
		}

		return null
	}

	def updateGroovy(sources) {
		
		if (currentSources?.groovy != sources) {
			// sources have changed, so compile them
			Logger.debug('Compiling Groovy sources')

			def result = compiler.update(sources.keySet().toList())
			currentSources.groovy = sources

			return result
		}

		return null
	}

}