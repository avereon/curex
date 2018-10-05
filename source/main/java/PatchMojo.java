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

@Mojo( name = "patch",  defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class PatchMojo extends AbstractMojo {

	@Parameter( property = "modulePath", defaultValue = "target/dependency" )
	private String modulePath;

	@Parameter( property = "modules" )
	private String[] modules;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		ModuleGenerator generator = new ModuleGenerator( getModulePath() );

		getLog().info( "Module path: " + generator.getModulePath() );

		generator.execute( modules );
	}

	public String getModulePath() {
		return modulePath;
	}

	public void setModulePath( String modulePath ) {
		this.modulePath = modulePath;
	}

	public String[] getModules() {
		return modules;
	}

	public void setModules( String[] modules ) {
		this.modules = modules;
	}

}
