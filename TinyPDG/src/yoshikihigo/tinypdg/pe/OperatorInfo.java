package yoshikihigo.tinypdg.pe;

public class OperatorInfo extends ProgramElementInfo {

	final public String name;

	public OperatorInfo(final String name, final int startLine,
			final int endLine, final int startOffset, final int endOffset) {
		super(startLine, endLine, startOffset, endOffset);
		this.name = name;
		this.setText(name);
	}
}
