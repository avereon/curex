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
		MockModuleGenerator generator = new MockModuleGenerator( modulePath );

		List<Jar> jars = new ArrayList<>();
		jars.add( new Jar().setName( "commons-io.jar" ).setStandalone( true ) );
		jars.add( new Jar().setName( "jackson-core.jar" ).setStandalone( true ) );
		jars.add( new Jar().setName( "jackson-annotations.jar" ).setStandalone( true ) );
		jars.add( new Jar().setName( "jackson-databind.jar" ) );
		jars.add( new Jar().setName( "slf4j-api.jar" ) );
		jars.add( new Jar().setName( "slf4j-jdk14.jar" ) );
		jars.add( new Jar().setName( "razor.jar" ) );

		for( Jar jar : jars ) {
			generator.patch( jar );
		}

		List<String[]> expectedCommands = new ArrayList<>();
		expectedCommands.addAll( generateExpectedCommands( modulePath, "commons-io.jar", "org.apache.commons.io", true ) );
		expectedCommands.addAll( generateExpectedCommands( modulePath, "jackson-core.jar", "com.fasterxml.jackson.core", true ) );
		expectedCommands.addAll( generateExpectedCommands( modulePath, "jackson-annotations.jar", "com.fasterxml.jackson.annotation", true ) );
		expectedCommands.addAll( generateExpectedCommands( modulePath, "jackson-databind.jar", "com.fasterxml.jackson.databind", false ) );
		assertThat( generator.getCommands(), contains( expectedCommands.toArray() ) );
	}

	private List<String[]> generateExpectedCommands( String modulePath, String jarName, String moduleName, boolean standalone ) {
		String javaHome = System.getProperty( "java.home" );
		String tempFolder = System.getProperty( "java.io.tmpdir" ) + "/modpatch";
		List<String[]> expectedCommands = new ArrayList<>();
		if( standalone ) {
			expectedCommands.add( new String[]{ javaHome + "/bin/jdeps", "--module-path", modulePath, "--generate-module-info", tempFolder, tempFolder + "/" + jarName } );
		} else {
			expectedCommands.add( new String[]{ javaHome + "/bin/jdeps", "--module-path", modulePath, "--add-modules=ALL-MODULE-PATH", "--generate-module-info", tempFolder, tempFolder + "/" + jarName } );
		}
		expectedCommands.add( new String[]{ javaHome + "/bin/javac", "-p", modulePath, "--patch-module", moduleName + "=" + tempFolder + "/" + jarName, tempFolder + "/" + moduleName + "/module-info.java" } );
		expectedCommands.add( new String[]{ javaHome + "/bin/jar", "uf", tempFolder + "/" + jarName, "-C", tempFolder + "/" + moduleName, "module-info.class" } );
		return expectedCommands;
	}

	private class MockModuleGenerator extends ModuleGenerator {

		private List<String[]> commands;

		public MockModuleGenerator( String modulePath ) {
			super( modulePath );
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
