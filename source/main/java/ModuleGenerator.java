import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.charset.StandardCharsets;
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
		List<File> files = Arrays.stream( commands ).map( ( name ) -> new File( getModulePath(), name ) ).collect( Collectors.toList() );

		for( File file : files ) {
			try {
				int result = patch( file );
				switch( result ) {
					case 0: {
						System.out.println( "Patch module: " + file.getName() );
						break;
					}
					case 1: {
						System.out.println( "Ready module: " + file.getName() );
						break;
					}
				}
			} catch( Exception exception ) {
				exception.printStackTrace( System.err );
			}
		}
	}

	public int patch( File file ) throws Exception {
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
			} else {
				return 1;
			}
		}

		if( moduleName == null ) moduleName = file.getName().replace( "-", "." );

		patch( file, moduleName );

		if( isAutomaticModule( file ) ) throw new RuntimeException( "Module is still an automatic module: " + moduleName );

		return 0;
	}

	private boolean isAutomaticModule( File file ) {
		Set<ModuleReference> moduleReferences = ModuleFinder.of( file.toPath() ).findAll();

		if( moduleReferences.size() > 1 ) {
			List<String> names = moduleReferences.stream().map( ( reference ) -> reference.descriptor().name() ).collect( Collectors.toList() );
			throw new IllegalArgumentException( "Jar contains more than one module: " + names.toString() );
		}

		return moduleReferences.iterator().next().descriptor().isAutomatic();
	}

	private void patch( File file, String moduleName ) throws IOException, InterruptedException {
		System.out.println( "Patch module: " + moduleName );

		File workFolder = new File( getWorkFolder() );
		File tempModule = new File( workFolder, file.getName() );

		// Move the module to fix out of the module path
		try {
			workFolder.mkdirs();
			file.renameTo( tempModule );
			patchModule( moduleName, workFolder, tempModule );
		} finally {
			tempModule.renameTo( file );
			deleteAll( workFolder );
		}
	}

	private void patchModule( String moduleName, File workFolder, File tempModule ) throws IOException, InterruptedException {
		File javaBin = new File( System.getProperty( "java.home" ), "bin" );
		File jdeps = new File( javaBin, "jdeps" );
		File javac = new File( javaBin, "javac" );
		File jar = new File( javaBin, "jar" );

		File moduleInfoFolder = new File( workFolder, moduleName );
		File moduleInfo = new File( moduleInfoFolder, "module-info.java" );

		String jdepsResult = exec( false, jdeps.toString(), "--module-path", modulePath, "--add-modules=ALL-MODULE-PATH", "--generate-module-info", workFolder.toString(), tempModule.toString() );
		if( !"".equals( jdepsResult ) ) {
			System.err.println( jdepsResult );
			return;
		}
		String javacResult = exec( false, javac.toString(), "-p", modulePath, "--patch-module", moduleName + "=" + tempModule, moduleInfo.toString() );
		if( !"".equals( javacResult ) ) {
			System.err.println( javacResult );
			return;
		}
		String jarResult = exec( false, jar.toString(), "uf", tempModule.toString(), "-C", moduleInfoFolder.toString(), "module-info.class" );
		if( !"".equals( jarResult ) ) {
			System.err.println( jarResult );
			return;
		}
	}

	private String exec( boolean log, String... commands ) throws IOException, InterruptedException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		ProcessBuilder builder = new ProcessBuilder( commands );
		if( log ) {
			System.out.println( builder.command() );
			builder.inheritIO();
		} else {
			builder.redirectErrorStream( true );
		}
		Process process = builder.start();
		process.waitFor( 10, TimeUnit.SECONDS );

		process.getInputStream().transferTo( output );

		int result = process.exitValue();

		return result == 0 ? "" : output.toString( StandardCharsets.UTF_8.toString() );
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
