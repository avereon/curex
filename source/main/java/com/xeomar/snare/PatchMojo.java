package com.xeomar.snare;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generally used after the maven-dependency-plugin to patch jars that are not
 * yet modules.
 */

@Mojo( name = "patch", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class PatchMojo extends AbstractMojo {

	@Parameter( property = "modulePath", defaultValue = "${project.build.directory}/dependency" )
	private String modulePath;

	@Parameter( property = "modules" )
	private Jar[] jars;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		ModuleGenerator generator = new ModuleGenerator( getModulePath() );

		getLog().info( "Module path: " + generator.getModulePath() );

		for( Jar jar : jars ) {
			try {
					int result = generator.patch( jar );
					switch( result ) {
						case 0: {
							getLog().info( "Patch module: " + jar.getName() );
							break;
						}
						case 1: {
							getLog().info( "Ready module: " + jar.getName() );
							break;
						}
				}
			} catch( Throwable throwable ) {
				getLog().error( "Error occurred generating module patch", throwable );
			}

		}

	}

	public String getModulePath() {
		return modulePath;
	}

	public void setModulePath( String modulePath ) {
		this.modulePath = modulePath;
	}

	public Jar[] getJars() {
		return jars;
	}

	public void setJars( Jar[] jars ) {
		this.jars = jars;
	}

}
