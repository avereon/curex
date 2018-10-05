import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generally used after the maven-dependency-plugin to patch jars that are not
 * yet modules.
 */

@Mojo( name = "patch", defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class PatchMojo extends AbstractMojo {

	@Parameter( property = "modulePath", defaultValue = "${project.build.directory}/dependency" )
	private String modulePath;

	@Parameter( property = "modules" )
	private String[] jars;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		ModuleGenerator generator = new ModuleGenerator( getModulePath() );

		getLog().info( "Module path: " + generator.getModulePath() );

		List<File> files = Arrays.stream( jars ).map( ( name ) -> new File( getModulePath(), name ) ).collect( Collectors.toList() );

		for( File file : files ) {
			try {
				if( file.isFile() ) {
					int result = generator.patch( file );
					switch( result ) {
						case 0: {
							getLog().info( "Patch module: " + file.getName() );
							break;
						}
						case 1: {
							getLog().info( "Ready module: " + file.getName() );
							break;
						}
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

	public String[] getJars() {
		return jars;
	}

	public void setJars( String[] jars ) {
		this.jars = jars;
	}

}
