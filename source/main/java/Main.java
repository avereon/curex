import java.io.*;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Main {

	private String workFolder;

	private String modulePath;

	public static void main( String[] commands ) {
		new Main().execute( commands );
	}

	public Main() {
		setWorkFolder( System.getProperty( "java.io.tmpdir" ) + "/modpatch" );
		setModulePath( "target/dependency" );
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
			init( file );
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
				System.out.println( "Module already defined: " + reference.descriptor().name() );
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
		System.out.println( "Determined module name: " + moduleName );

		//		echo "Jar path: $JAR_PATH"
		//		echo "Jar name: $JAR_NAME"
		//		echo "Module name: $MOD_NAME"

		System.out.println( "Jar path: " + file.getParent() );
		System.out.println( "Jar name: " + file.getName() );
		System.out.println( "Module path: " + getModulePath() );

		//		# Move the module to fix out of the module path
		//		mkdir -p "$WORK_FOLDER"
		File workFolder = new File( getWorkFolder() );
		workFolder.mkdirs();

		File tempModule = new File( workFolder, file.getName() );
		File moduleInfoModuleFolder = new File( workFolder, moduleName );
		File moduleInfo = new File( moduleInfoModuleFolder, file.getName() );

		//		mv "$1" "$WORK_FOLDER"
		file.renameTo( tempModule );

		File javaBin = new File( System.getProperty( "java.home" ), "bin" );
		File jdeps = new File( javaBin, "jdeps" );
		File javac = new File( javaBin, "javac" );
		File jar = new File( javaBin, "jar" );

		exec( jdeps.toString(), "--module-path", modulePath.toString(), "--generate-module-info", workFolder.toString(), tempModule.toString() );
		//		$JAVA_HOME/bin/jdeps --module-path "$MODULE_PATH" --generate-module-info "$MODULE_INFO_FOLDER" "$WORK_FOLDER/$JAR_NAME"
		//		$JAVA_HOME/bin/javac -p "$MODULE_PATH" --patch-module $MOD_NAME="$WORK_FOLDER/$JAR_NAME" "$WORK_FOLDER/$MOD_NAME/module-info.java"
		//		$JAVA_HOME/bin/jar uf "$WORK_FOLDER/$JAR_NAME" -C "$WORK_FOLDER/$MOD_NAME" module-info.class

		tempModule.renameTo( file );

		deleteAll( workFolder );
	}

	private void exec( String... commands ) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder( commands );
		Process process = builder.inheritIO().start();
		process.waitFor( 10, TimeUnit.SECONDS );
	}

	private String toString( InputStream input ) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		input.transferTo( output );
		return output.toString( StandardCharsets.UTF_8 );
	}

	private void deleteAll( File file ) {
		if( file.isDirectory() ) {
			for( File child : Objects.requireNonNull( file.listFiles() ) ) {
				deleteAll( child );
			}
		}
		file.delete();
	}

}
