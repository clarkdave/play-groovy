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
			def result = update()
			if (result) {
				updateInternalApplicationClasses(result)
				if (result.updatedClasses.size() + result.removedClasses.size() > 0) {
					reload()
				}
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
		
		Logger.info('Compiling sources')

		try {
			def result = update()
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

	def loadedModuleNames = {
		Play.modules.collect { name, file -> 
			file.getRealFile().toString().toLowerCase() }
	}.memoize()

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
		def m = [:]
		Play.javaPath.each {
			GroovyCompiler.getSourceFiles(it.getRealFile()).each { f -> m[f] = f.lastModified()}
		}
		def sources = [
			'groovy': [:],
			'java': [:]
		]
		println loadedModuleNames
		return m.findAll { file, lastModified ->
			// we'd like to override the testrunner controller with our own, so
			// let's make sure it never gets compiled... bit of a hack but I couldn't
			// see any other way to override a controller in an included module
			!(file.toString() =~ /(?i)testrunner.+TestRunner\.java/)
		}
	}

	def update() {
		
		// get the latest sources
		def newSources = sources()
		
		if (currentSources != newSources) {
			// sources have changed, so compile them
			def result = compiler.update(newSources.keySet().toList())
			currentSources = newSources
			return result
		} else {
			// sources haven't changed
			return null
		}
	}
}