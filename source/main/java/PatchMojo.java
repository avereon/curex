import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

@Mojo( name = "patch" )
public class PatchMojo extends AbstractMojo {

	@Parameter( property = "modulePath", defaultValue = "target/dependency" )
	private String modulePath;

	@Parameter( property = "modules" )
	private List<String> modules;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		ModuleGenerator generator = new ModuleGenerator( getModulePath() );

		getLog().info( "Module path: " + generator.getModulePath() );
	}

	public String getModulePath() {
		return modulePath;
	}

	public void setModulePath( String modulePath ) {
		this.modulePath = modulePath;
	}

	public List<String> getModules() {
		return modules;
	}

	public void setModules( List<String> modules ) {
		this.modules = modules;
	}

}
