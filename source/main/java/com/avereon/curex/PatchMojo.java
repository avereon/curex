package com.avereon.curex;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * Generally used after the maven-dependency-plugin to patch jars that are not
 * yet modules.
 */

@Mojo( name = "patch", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class PatchMojo extends AbstractMojo {

	private final Random random = new Random();

	@Parameter( readonly = true, defaultValue = "${project}" )
	private MavenProject project;

	@Parameter( property = "modulePath", defaultValue = "${project.build.directory}/dependency" )
	private String modulePath;

	@Parameter( property = "tempFolder" )
	private String tempFolder = System.getProperty( "java.io.tmpdir" ) + "/curex-" + Integer.toHexString( random.nextInt() );

	@Parameter( property = "modules" )
	private ModuleJar[] jars;

	@Override
	public void execute() {
		getLog().info( "Module path: " + getModulePath() );
		getLog().info( "Temp folder: " + getTempFolder() );

		try {
			for( ModuleJar jar : jars ) {
				patch( jar );
			}
		} catch( Throwable throwable ) {
			getLog().error( "Error occurred generating module patch", throwable );
		}

	}

	public MavenProject getProject() {
		return project;
	}

	public void setProject( MavenProject project ) {
		this.project = project;
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

	@SuppressWarnings( "unused" )
	public void setTempFolder( String tempFolder ) {
		this.tempFolder = tempFolder;
	}

	@SuppressWarnings( "unused" )
	public ModuleJar[] getJars() {
		return jars;
	}

	public void setJars( ModuleJar[] jars ) {
		this.jars = jars;
	}

	public void patch( ModuleJar jar ) throws Exception {
		FileSet fileSet = new FileSet();
		fileSet.setDirectory( getModulePath() );
		fileSet.addInclude( jar.getName() );
		for( String file : new FileSetManager().getIncludedFiles( fileSet ) ) {
			patch( jar, new File( getModulePath(), file ) );
		}
	}

	private void patch( ModuleJar jar, File file ) throws IOException, InterruptedException {
		Set<ModuleReference> moduleReferences = getModuleReferences( file );

		String moduleName = jar.getModule();

		if( moduleName == null && moduleReferences.size() == 1 ) {
			ModuleReference reference = moduleReferences.iterator().next();
			if( reference.descriptor().isAutomatic() ) {
				moduleName = reference.descriptor().name();
			} else {
				getLog().info( "Existing module: " + file.getName() );
				return;
			}
		}

		if( moduleName == null ) moduleName = file.getName().replace( "-", "." );
		jar.setModule( moduleName );

		try {
			getLog().info( "Patching module: " + file.getName() + " as " + jar.getModule() );
			patchModule( jar, file );
		} finally {
			deleteAll( new File( getTempFolder() ) );
		}

		//if( isAutomaticModule( file ) ) throw new RuntimeException( "Module is still an automatic module: " + moduleName );
	}

	private Set<ModuleReference> getModuleReferences( File file ) {
		Set<ModuleReference> moduleReferences = ModuleFinder.of( file.toPath() ).findAll();

		if( moduleReferences.size() > 1 ) {
			List<String> names = moduleReferences.stream().map( ( reference ) -> reference.descriptor().name() ).collect( Collectors.toList() );
			throw new IllegalArgumentException( "ModuleJar contains more than one module: " + names );
		}

		return moduleReferences;
	}

	//	@SuppressWarnings( "unused" )
	//	private boolean isAutomaticModule( File file ) {
	//		return getModuleReferences( file ).iterator().next().descriptor().isAutomatic();
	//	}

	private void patchModule( ModuleJar jar, File file ) throws IOException, InterruptedException {
		File workFolder = new File( getTempFolder() );
		File tempModule = new File( workFolder, file.getName() );

		// Move the module to fix out of the module path
		try {
			if( !workFolder.mkdirs() ) throw new IOException( "Unable to make temp folder " + workFolder );
			if( !file.renameTo( tempModule ) ) throw new IOException( "Unable to move " + file + " to " + tempModule );
			if( jar.getMergeJars().size() > 0 ) doMergeJars( jar, tempModule );
			doPatchModule( jar, tempModule );
		} finally {
			tempModule.renameTo( file );
		}
	}

	private void doMergeJars( ModuleJar moduleJar, File file ) throws IOException {
		doMergeJars( file, moduleJar.getMergeJars().stream().map( s -> new File( getModulePath(), s ) ).collect( Collectors.toSet() ) );
	}

	private void doMergeJars( File primary, Set<File> files ) throws IOException {
		File workFolder = new File( getTempFolder() );
		File tempJarFolder = new File( workFolder, "tempJar" );

		List<File> mergeJars = new ArrayList<>( files );
		mergeJars.add( primary );

		// Move the primary file out of the way
		File tempPrimaryFile = new File( workFolder, "primary.jar" );
		getLog().info( "temp primary=" + tempPrimaryFile );

		// Create a merged manifest
		Manifest mergedManifest = new Manifest();
		for( File mergeJar : mergeJars ) {
			JarFile jarFile = new JarFile( mergeJar );
			Manifest manifest = jarFile.getManifest();
			mergedManifest.getMainAttributes().putAll( manifest.getMainAttributes() );
			mergedManifest.getEntries().forEach( ( key, value ) -> mergedManifest.getAttributes( key ).putAll( value ) );
		}

		// Merge the jars
		Collections.reverse( mergeJars );
		try( JarOutputStream jarOutputStream = new JarOutputStream( new FileOutputStream( tempPrimaryFile ), mergedManifest ) ) {
			Set<String> entries = new HashSet<>();
			entries.add( "META-INF/MANIFEST.MF" );
			for( File mergeJar : mergeJars ) {
				JarFile jarFile = new JarFile( mergeJar );
				for( JarEntry entry : new Enumerator<>( jarFile.entries() ) ) {
					if( entries.contains( entry.getName() ) ) continue;
					jarOutputStream.putNextEntry( entry );
					IOUtils.copy( jarFile.getInputStream( entry ), jarOutputStream );
					jarOutputStream.closeEntry();
					entries.add( entry.getName() );
				}
			}
		}
	}

	private void doPatchModule( ModuleJar moduleJar, File tempModule ) throws IOException, InterruptedException {
		File workFolder = new File( getTempFolder() );
		String moduleName = moduleJar.getModule();
		List<String> modules = moduleJar.getModules();
		boolean ignoreMissing = moduleJar.isIgnoreMissing();

		File javaBin = new File( System.getProperty( "java.home" ), "bin" );
		File jdeps = new File( javaBin, "jdeps" );
		File javac = new File( javaBin, "javac" );
		File jar = new File( javaBin, "jar" );

		File moduleInfoFolder = new File( workFolder, moduleName );
		File moduleInfo = new File( moduleInfoFolder, "module-info.java" );

		StringBuilder builder = new StringBuilder();
		for( String module : modules ) {
			builder.append( "," );
			builder.append( module );
		}
		String addModules = modules.size() == 0 ? "" : builder.substring( 1 );

		List<String> commands = new ArrayList<>();
		commands.add( jdeps.toString() );
		if( ignoreMissing ) commands.add( "--ignore-missing-deps" );
		commands.add( "--upgrade-module-path" );
		commands.add( modulePath );
		if( !addModules.isBlank() ) {
			commands.add( "--add-modules" );
			commands.add( addModules );
		}
		commands.add( "--generate-module-info" );
		commands.add( workFolder.toString() );
		commands.add( tempModule.toString() );

		// Generate the module-info.java file
		String jdepsResult = exec( false, commands );
		if( !"".equals( jdepsResult ) ) throw new RuntimeException( jdepsResult );

		// Compile the module-info.java file
		String javacResult = exec( false, javac.toString(), "-p", modulePath, "--patch-module", moduleName + "=" + tempModule, moduleInfo.toString() );
		if( !"".equals( javacResult ) ) {
			System.err.println( javacResult );
			return;
		}

		// Merge the new module-java.class file into the jar
		String jarResult = exec( false, jar.toString(), "-uf", tempModule.toString(), "-C", moduleInfoFolder.toString(), "module-info.class" );
		if( !"".equals( jarResult ) ) System.err.println( jarResult );
	}

	@SuppressWarnings( "SameParameterValue" )
	private String exec( boolean log, String... commands ) throws IOException, InterruptedException {
		return exec( log, Arrays.asList( commands ) );
	}

	@SuppressWarnings( "SameParameterValue" )
	String exec( boolean log, List<String> commands ) throws IOException, InterruptedException {
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

	@SuppressWarnings( "ResultOfMethodCallIgnored" )
	private static void deleteAll( File file ) {
		if( file.isDirectory() ) {
			for( File child : Objects.requireNonNull( file.listFiles() ) ) {
				deleteAll( child );
			}
		}
		file.delete();
	}

}
