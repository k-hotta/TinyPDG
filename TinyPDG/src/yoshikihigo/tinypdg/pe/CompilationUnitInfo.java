package yoshikihigo.tinypdg.pe;

/**
 * This class represents a compilation unit, which is usually a file. This is
 * NOT a subclass of {@link ProgramInfo}. The main objective of this class is to
 * keep string of the file after normalized.
 * 
 * @author k-hotta
 *
 */
public class CompilationUnitInfo {

	/**
	 * The file path. This should be unique so that this is used as the key.
	 */
	private final String path;

	/**
	 * The text of the file AFTER normalized.
	 */
	private String text;

	public CompilationUnitInfo(final String path) {
		this.path = path;
		this.text = null;
	}

	public final String getPath() {
		return path;
	}

	public final void setText(final String text) {
		assert text != null : "text is null";
		this.text = text;
	}

	public final String getText() {
		return this.text;
	}

}
