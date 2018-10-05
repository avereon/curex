import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ModuleGenerator {

	private String workFolder;

	private String modulePath;

	public ModuleGenerator( String modulePath ) {
		setModulePath( modulePath );
		setWorkFolder( System.getProperty( "java.io.tmpdir" ) + "/modpatch" );
	}

	public String getWorkFolder() {
		return workFolder;
	}

	public void setWorkFolder( String workFolder ) {
		this.workFolder = workFolder;
	}

	public String getModulePath() {
		return modulePath;
	}

	public void setModulePath( String modulePath ) {
		this.modulePath = modulePath;
	}

	public void execute( String[] commands ) {
		List<File> files = Arrays.stream( commands ).map( File::new ).collect( Collectors.toList() );

		for( File file : files ) {
			if( file.isDirectory() ) {
				for( File child : Objects.requireNonNull( file.listFiles() ) ) {
					process( child );
				}
			} else {
				process( file );
			}
		}
	}

	private void process( File file ) {
		try {
			init( new File( getModulePath(), file.getName() ) );
		} catch( Exception exception ) {
			exception.printStackTrace( System.err );
		}
	}

	private void init( File file ) throws Exception {
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
				//System.out.println( "Automatic module name: " + moduleName );
			} else {
				System.out.println( "Already module: " + reference.descriptor().name() );
				return;
			}
		}

		if( moduleName == null ) {
			moduleName = file.getName().replace( "-", "." );
			//System.out.println( "Generated module name: " + moduleName );
		}

		fix( file, moduleName );
	}

	private void fix( File file, String moduleName ) throws IOException, InterruptedException {
		System.out.println( "Patch module:  " + moduleName );

		//		System.out.println( "Jar path: " + file.getParent() );
		//		System.out.println( "Jar name: " + file.getName() );
		//		System.out.println( "Module path: " + getModulePath() );

		File workFolder = new File( getWorkFolder() );
		File tempModule = new File( workFolder, file.getName() );

		workFolder.mkdirs();

		// Move the module to fix out of the module path
		file.renameTo( tempModule );
		patchModule( moduleName, workFolder, tempModule );
		tempModule.renameTo( file );

		deleteAll( workFolder );
	}

	private void patchModule( String moduleName, File workFolder, File tempModule ) throws IOException, InterruptedException {
		File javaBin = new File( System.getProperty( "java.home" ), "bin" );
		File jdeps = new File( javaBin, "jdeps" );
		File javac = new File( javaBin, "javac" );
		File jar = new File( javaBin, "jar" );

		File moduleInfoFolder = new File( workFolder, moduleName );
		File moduleInfo = new File( moduleInfoFolder, "module-info.java" );

		exec( jdeps.toString(), "--module-path", modulePath, "--generate-module-info", workFolder.toString(), tempModule.toString() );
		exec( javac.toString(), "-p", modulePath, "--patch-module", moduleName + "=" + tempModule, moduleInfo.toString() );
		exec( jar.toString(), "uf", tempModule.toString(), "-C", moduleInfoFolder.toString(), "module-info.class" );
	}

	private void exec( String... commands ) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder( commands );
		Process process = builder.start();
		process.waitFor( 10, TimeUnit.SECONDS );
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
