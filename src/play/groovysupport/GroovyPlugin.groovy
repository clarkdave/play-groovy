package play.groovysupport

import play.*
import play.exceptions.*
import play.vfs.VirtualFile
import play.classloading.ApplicationClasses.ApplicationClass
import play.groovysupport.compiler.*
import play.mvc.Http.*

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
		
		Logger.info('Groovy support is active')
	}

	@Override
	void onConfigurationRead() {
		// TODO: investigate how necessary this is...
		Play.configuration.put("play.bytecodeCache", "false")
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
			appClass.javaFile = VirtualFile.open(it.source)
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

	def sources() {
		def map = [:]
		Play.javaPath.each {
			GroovyCompiler.getSourceFiles(it.getRealFile()).each { f -> map[f] = f.lastModified()}
		}
		return map
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