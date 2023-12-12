package com.avereon.curex;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class PatchMojoTest {

	@Test
	public void testPatch() throws Exception {
		FileUtils.copyDirectory( new File( "target/test/java" ), new File( "target/modules" ) );

		String modulePath = "target/modules";

		//		PatchMojo mojo = Mockito.spy( PatchMojo.class );
		//		doReturn( "" ).when( mojo ).exec( Mockito.anyBoolean(), any() );

		PatchMojo mojo = new PatchMojo();
		mojo.setModulePath( modulePath );

		List<ModuleJar> jars = new ArrayList<>();
		jars.add( new ModuleJar().setName( "commons-io-*.jar" ) );
		jars.add( new ModuleJar().setName( "jackson-core.jar" ) );
		jars.add( new ModuleJar().setName( "slf4j*.jar" ) );
		jars.add( new ModuleJar().setName( "razor.jar" ).setModules( List.of( "org.apache.commons.io" ) ) );
		jars.add( new ModuleJar().setName( "jackson-annotations.jar" ) );
		jars.add( new ModuleJar().setName( "jackson-databind.jar" ).setModules( List.of( "com.fasterxml.jackson.core", "com.fasterxml.jackson.annotation" ) ) );

		mojo.setJars( jars );
		mojo.execute();

		assertThat( isModule( new File( modulePath, "commons-io-1.2.3.jar" ) ) ).isTrue();
		assertThat( isModule( new File( modulePath, "jackson-core.jar" ) ) ).isTrue();
		assertThat( isModule( new File( modulePath, "slf4j-api.jar" ) ) ).isTrue();
		assertThat( isModule( new File( modulePath, "slf4j-jdk14.jar" ) ) ).isTrue();
		assertThat( isModule( new File( modulePath, "jackson-core.jar" ) ) ).isTrue();
		assertThat( isModule( new File( modulePath, "jackson-annotations.jar" ) ) ).isTrue();
		assertThat( isModule( new File( modulePath, "jackson-databind.jar" ) ) ).isTrue();
	}

	@Test
	public void testPatchCommands() throws Exception {
		FileUtils.copyDirectory( new File( "target/test/java" ), new File( "target/modules" ) );

		String modulePath = "target/modules";

		PatchMojo mojo = Mockito.spy( PatchMojo.class );
		doReturn( "" ).when( mojo ).exec( Mockito.anyBoolean(), any() );
		doReturn( false ).when( mojo ).isAutomaticModule( any() );

		mojo.setModulePath( modulePath );
		String tempFolder = mojo.getTempFolder();

		List<ModuleJar> jars = new ArrayList<>();
		jars.add( new ModuleJar().setName( "commons-io-*.jar" ) );
		jars.add( new ModuleJar().setName( "jackson-core.jar" ) );
		jars.add( new ModuleJar().setName( "slf4j*.jar" ) );
		jars.add( new ModuleJar().setName( "razor.jar" ).setModules( List.of( "org.apache.commons.io" ) ) );
		jars.add( new ModuleJar().setName( "jackson-annotations.jar" ) );
		jars.add( new ModuleJar().setName( "jackson-databind.jar" ).setModules( List.of( "com.fasterxml.jackson.annotation" ) ) );

		mojo.setJars( jars );
		//mojo.setMock( true );
		mojo.execute();

		verifyExpectedCommands( mojo, modulePath, tempFolder, "commons-io-1.2.3.jar", "org.apache.commons.io", null );
		verifyExpectedCommands( mojo, modulePath, tempFolder, "jackson-core.jar", "com.fasterxml.jackson.core", null );
		// Slf4j is an existing module, no commands should be created
		// Razor is an existing module, no commands should be created
		verifyExpectedCommands( mojo, modulePath, tempFolder, "jackson-annotations.jar", "com.fasterxml.jackson.annotation", null );
		verifyExpectedCommands( mojo, modulePath, tempFolder, "jackson-databind.jar", "com.fasterxml.jackson.databind", "com.fasterxml.jackson.annotation" );
	}

	@Test
	public void testPatchWithMerge() throws Exception {
		// Some setup
		File testModulePath = new File( "target/test/java" );
		File tempModulePath = File.createTempFile( "curex", "curex" );
		tempModulePath.delete();
		tempModulePath.mkdirs();
		FileUtils.copyFile( new File( testModulePath, "jackson-databind.jar" ), new File( tempModulePath, "jackson-databind.jar" ) );
		FileUtils.copyFile( new File( testModulePath, "jackson-core.jar" ), new File( tempModulePath, "jackson-core.jar" ) );
		FileUtils.copyFile( new File( testModulePath, "jackson-annotations.jar" ), new File( tempModulePath, "jackson-annotations.jar" ) );

		String modulePath = tempModulePath.toString();
		PatchMojo mojo = Mockito.spy( PatchMojo.class );
		doReturn( "" ).when( mojo ).exec( Mockito.anyBoolean(), any() );
		doReturn( false ).when( mojo ).isAutomaticModule( any() );

		List<ModuleJar> jars = List.of( new ModuleJar().setName( "jackson-databind.jar" ).setMergeJars( Set.of( "jackson-core.jar", "jackson-annotations.jar" ) ) );

		mojo.setModulePath( modulePath );
		mojo.setJars( jars );
		mojo.execute();

		assertTrue( new File( testModulePath, "jackson-databind.jar" ).exists() );

		FileUtils.deleteQuietly( tempModulePath );
	}

	private List<List<String>> generateExpectedCommandList( String modulePath, String tempFolder, String jarName, String moduleName, String addModules ) {
		String javaHome = System.getProperty( "java.home" );
		List<List<String>> expectedCommands = new ArrayList<>();
		if( addModules == null ) {
			expectedCommands.add( List.of( javaHome + "/bin/jdeps", "--upgrade-module-path", modulePath, "--generate-module-info", tempFolder, tempFolder + "/" + jarName ) );
		} else {
			expectedCommands.add( List.of( javaHome + "/bin/jdeps", "--upgrade-module-path", modulePath, "--add-modules", addModules, "--generate-module-info", tempFolder, tempFolder + "/" + jarName ) );
		}
		expectedCommands.add( List.of( javaHome + "/bin/javac", "-p", modulePath, "--patch-module", moduleName + "=" + tempFolder + "/" + jarName, tempFolder + "/" + moduleName + "/module-info.java" ) );
		expectedCommands.add( List.of( javaHome + "/bin/jar", "-uf", tempFolder + "/" + jarName, "-C", tempFolder + "/" + moduleName, "module-info.class" ) );
		return expectedCommands;
	}

	private void verifyExpectedCommands( PatchMojo mojo, String modulePath, String tempFolder, String jarName, String moduleName, String addModules ) throws Exception {
		List<List<String>> commands = generateExpectedCommandList( modulePath, tempFolder, jarName, moduleName, addModules );
		verify( mojo, times( 1 ) ).exec( false, commands.get( 0 ) );
		verify( mojo, times( 1 ) ).exec( false, commands.get( 1 ) );
		verify( mojo, times( 1 ) ).exec( false, commands.get( 2 ) );
	}

	private boolean isModule( File file ) {
		return getModuleReferences( file ).size() == 1;
	}

	private boolean isAutomaticModule( File file ) {
		ModuleReference reference = getModuleReferences( file ).iterator().next();
		return reference.descriptor().isAutomatic();
	}

	private Set<ModuleReference> getModuleReferences( File file ) {
		Set<ModuleReference> moduleReferences = ModuleFinder.of( file.toPath() ).findAll();

		if( moduleReferences.size() > 1 ) {
			List<String> names = moduleReferences.stream().map( ( reference ) -> reference.descriptor().name() ).toList();
			throw new IllegalArgumentException( "ModuleJar contains more than one module: " + names );
		}

		return moduleReferences;
	}

}
