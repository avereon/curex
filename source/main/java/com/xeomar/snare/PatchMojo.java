package com.xeomar.snare;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Generally used after the maven-dependency-plugin to patch jars that are not
 * yet modules.
 */

@Mojo( name = "patch", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class PatchMojo extends AbstractMojo {

	@Parameter( property = "modulePath", defaultValue = "${project.build.directory}/dependency" )
	private String modulePath;

	@Parameter( property = "tempFolder" )
	private String tempFolder = System.getProperty( "java.io.tmpdir" ) + "/modpatch";

	@Parameter( property = "modules" )
	private Jar[] jars;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info( "Module path: " + getModulePath() );

		try {
			for( Jar jar : jars ) {
				patch( jar );
			}
		} catch( Throwable throwable ) {
			getLog().error( "Error occurred generating module patch", throwable );
		}

	}

	public String getModulePath() {
		return modulePath;
	}

	public void setModulePath( String modulePath ) {
		this.modulePath = modulePath;
	}

	public String getTempFolder() {
		return tempFolder;
	}

	public void setTempFolder( String tempFolder ) {
		this.tempFolder = tempFolder;
	}

	public Jar[] getJars() {
		return jars;
	}

	public void setJars( Jar[] jars ) {
		this.jars = jars;
	}

	public int patch( Jar jar ) throws Exception {
		File file = new File( getModulePath(), jar.getName() );
		if( !file.exists() ) throw new FileNotFoundException( file.toString() );

		Set<ModuleReference> moduleReferences = ModuleFinder.of( file.toPath() ).findAll();

		if( moduleReferences.size() > 1 ) {
			List<String> names = moduleReferences.stream().map( ( reference ) -> reference.descriptor().name() ).collect( Collectors.toList() );
			throw new IllegalArgumentException( "Jar contains more than one module: " + names.toString() );
		}

		String moduleName = null;

		if( moduleReferences.size() == 1 ) {
			ModuleReference reference = moduleReferences.iterator().next();
			if( reference.descriptor().isAutomatic() ) {
				moduleName = reference.descriptor().name();
			} else {
				getLog().info( "Existing module: " + jar.getName() );
				return 1;
			}
		}

		if( moduleName == null ) moduleName = file.getName().replace( "-", "." );

		getLog().info( "Patching module: " + jar.getName() );
		patch( file, moduleName, jar.getModules() );

		//if( isAutomaticModule( file ) ) throw new RuntimeException( "Module is still an automatic module: " + moduleName );

		return 0;
	}

	private boolean isAutomaticModule( File file ) {
		Set<ModuleReference> moduleReferences = ModuleFinder.of( file.toPath() ).findAll();

		if( moduleReferences.size() > 1 ) {
			List<String> names = moduleReferences.stream().map( ( reference ) -> reference.descriptor().name() ).collect( Collectors.toList() );
			throw new IllegalArgumentException( "Jar contains more than one module: " + names.toString() );
		}

		return moduleReferences.iterator().next().descriptor().isAutomatic();
	}

	private void patch( File file, String moduleName, List<String> modules ) throws IOException, InterruptedException {
		File workFolder = new File( getTempFolder() );
		File tempModule = new File( workFolder, file.getName() );

		// Move the module to fix out of the module path
		try {
			workFolder.mkdirs();
			file.renameTo( tempModule );
			patchModule( moduleName, workFolder, tempModule, modules );
		} finally {
			tempModule.renameTo( file );
			deleteAll( workFolder );
		}
	}

	private void patchModule( String moduleName, File workFolder, File tempModule, List<String> modules ) throws IOException, InterruptedException {
		File javaBin = new File( System.getProperty( "java.home" ), "bin" );
		File jdeps = new File( javaBin, "jdeps" );
		File javac = new File( javaBin, "javac" );
		File jar = new File( javaBin, "jar" );

		File moduleInfoFolder = new File( workFolder, moduleName );
		File moduleInfo = new File( moduleInfoFolder, "module-info.java" );

		StringBuilder builder = new StringBuilder();
		for( String module : modules ) {
			builder.append(",");
			builder.append(module );
		}
		String addModules = modules.size() == 0 ? "" : builder.toString().substring( 1 );

		String jdepsResult;
		if( modules.size() == 0 ) {
			jdepsResult = exec( false, jdeps.toString(), "--upgrade-module-path", modulePath, "--generate-module-info", workFolder.toString(), tempModule.toString() );
		} else {
			jdepsResult = exec( false, jdeps.toString(), "--upgrade-module-path", modulePath, "--add-modules=" + addModules, "--generate-module-info", workFolder.toString(), tempModule.toString() );
		}
		if( !"".equals( jdepsResult ) ) throw new RuntimeException( jdepsResult );

		String javacResult = exec( false, javac.toString(), "-p", modulePath, "--patch-module", moduleName + "=" + tempModule, moduleInfo.toString() );
		if( !"".equals( javacResult ) ) {
			System.err.println( javacResult );
			return;
		}
		String jarResult = exec( false, jar.toString(), "uf", tempModule.toString(), "-C", moduleInfoFolder.toString(), "module-info.class" );
		if( !"".equals( jarResult ) ) {
			System.err.println( jarResult );
			return;
		}
	}

	String exec( boolean log, String... commands ) throws IOException, InterruptedException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		ProcessBuilder builder = new ProcessBuilder( commands );
		if( log ) {
			System.out.println( builder.command() );
			builder.inheritIO();
		} else {
			builder.redirectErrorStream( true );
		}
		Process process = builder.start();
		process.getInputStream().transferTo( output );
		process.waitFor( 10, TimeUnit.SECONDS );

		int result = process.exitValue();

		return result == 0 ? "" : output.toString( StandardCharsets.UTF_8.toString() );
	}

	private static void deleteAll( File file ) {
		if( file.isDirectory() ) {
			for( File child : Objects.requireNonNull( file.listFiles() ) ) {
				deleteAll( child );
			}
		}
		file.delete();
	}

}
