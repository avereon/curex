import java.util.Arrays;

public class Main {

	public static void main( String[] commands ) {
		new ModuleGenerator( commands[ 0 ] ).execute( Arrays.copyOfRange( commands, 1, commands.length ) );
	}

}
