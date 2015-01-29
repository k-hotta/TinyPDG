package yoshikihigo.tinypdg.pe;

public class TypeInfo extends ProgramElementInfo {

	final public String name;

	public TypeInfo(final String name, final int startLine, final int endLine,
			final int startOffset, final int endOffset) {
		super(startLine, endLine, startOffset, endOffset);
		this.name = name;
		this.setText(name);
	}
}
