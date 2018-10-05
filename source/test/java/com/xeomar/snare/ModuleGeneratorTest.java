package com.xeomar.snare;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class ModuleGeneratorTest {

	@Test
	public void testPatch() throws Exception {
		String modulePath = "source/test/resources";
		MockModuleGenerator generator = new MockModuleGenerator();
		generator.setModulePath( modulePath );

		List<Jar> jars = new ArrayList<>();
		jars.add( new Jar().setName( "commons-io.jar" ) );
		jars.add( new Jar().setName( "jackson-core.jar" ) );
		jars.add( new Jar().setName( "jackson-annotations.jar" ) );
		jars.add( new Jar().setName( "jackson-databind.jar" ).setModules( List.of( "com.fasterxml.jackson.annotation" ) ) );
		jars.add( new Jar().setName( "slf4j-api.jar" ) );
		jars.add( new Jar().setName( "slf4j-jdk14.jar" ) );
		jars.add( new Jar().setName( "razor.jar" ).setModules( List.of( "org.apache.commons.io" )) );

		for( Jar jar : jars ) {
			generator.patch( jar );
		}

		List<String[]> expectedCommands = new ArrayList<>();
		expectedCommands.addAll( generateExpectedCommands( modulePath, "commons-io.jar", "org.apache.commons.io" ) );
		expectedCommands.addAll( generateExpectedCommands( modulePath, "jackson-core.jar", "com.fasterxml.jackson.core" ) );
		expectedCommands.addAll( generateExpectedCommands( modulePath, "jackson-annotations.jar", "com.fasterxml.jackson.annotation" ) );
		expectedCommands.addAll( generateExpectedCommands( modulePath, "jackson-databind.jar", "com.fasterxml.jackson.databind", "com.fasterxml.jackson.annotation" ) );
		assertThat( generator.getCommands(), contains( expectedCommands.toArray() ) );
	}

	private List<String[]> generateExpectedCommands( String modulePath, String jarName, String moduleName ) {
		return generateExpectedCommands( modulePath, jarName, moduleName, null );
	}

	private List<String[]> generateExpectedCommands( String modulePath, String jarName, String moduleName, String addModules ) {
		String javaHome = System.getProperty( "java.home" );
		String tempFolder = System.getProperty( "java.io.tmpdir" ) + "/modpatch";
		List<String[]> expectedCommands = new ArrayList<>();
		if( addModules == null ) {
			expectedCommands.add( new String[]{ javaHome + "/bin/jdeps", "--upgrade-module-path", modulePath, "--generate-module-info", tempFolder, tempFolder + "/" + jarName } );
		} else {
			expectedCommands.add( new String[]{ javaHome + "/bin/jdeps", "--upgrade-module-path", modulePath, "--add-modules=" + addModules, "--generate-module-info", tempFolder, tempFolder + "/" + jarName } );
		}
		expectedCommands.add( new String[]{ javaHome + "/bin/javac", "-p", modulePath, "--patch-module", moduleName + "=" + tempFolder + "/" + jarName, tempFolder + "/" + moduleName + "/module-info.java" } );
		expectedCommands.add( new String[]{ javaHome + "/bin/jar", "uf", tempFolder + "/" + jarName, "-C", tempFolder + "/" + moduleName, "module-info.class" } );
		return expectedCommands;
	}

	private class MockModuleGenerator extends PatchMojo {

		private List<String[]> commands;

		public MockModuleGenerator() {
			this.commands = new CopyOnWriteArrayList<>();
		}

		public List<String[]> getCommands() {
			return commands;
		}

		@Override
		String exec( boolean log, String... commands ) throws IOException, InterruptedException {
			this.commands.add( commands );
			return "";
		}
	}

}
