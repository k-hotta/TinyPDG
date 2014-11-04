package yoshikihigo.tinypdg.scorpio;

/**
 * This class is for verbose output.
 * 
 * @author k-hotta
 *
 */
public class Message {

	private static boolean verbose = false;
	
	public static void setVerbose(final boolean value) {
		verbose = value;
	}
	
	public static void log(final String msg) {
		if (verbose) {
			System.out.println(msg);
		}
	}
	
}
