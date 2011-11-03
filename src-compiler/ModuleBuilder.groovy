package play.groovysupport.build

import play.groovysupport.compiler.*
import play.libs.Files

class ModuleBuilder {
	
	static void main(String[] args) {
		
		println '~ You want me to compile? OK'

		def classpath = System.getProperty('java.class.path')
			.split(System.getProperty('path.separator')) as List

		def compiler = new GroovyCompiler(new File('.'), new File('./libs'), 
			classpath, new File('tmp'))

		def jar = {
			Files.delete(new File('lib/play-groovy.jar'))
			Files.zip(new File('tmp/classes'), new File('lib/play-groovy.jar'))
		}

		def compile = {

			def start = System.currentTimeMillis()
			def message = ''

			try {
				// TODO: better error display here?
				compiler.update(GroovyCompiler.getSourceFiles(new File('src')))
				message = "Compilation completed successful in ${(System.currentTimeMillis() - start)/1000}s"
			} catch (CompilationErrorException e) {
				// TODO: make this prettier?
				message = "Compilation failed because: ${e.compilationError}"
			} catch (e) {
				e.printStackTrace()
				message = "Compilation failed: ${e.getMessage()}"
			}

			println '~'
			println '~ ' + message
			println '~'
		}

		compile()
	}
}
