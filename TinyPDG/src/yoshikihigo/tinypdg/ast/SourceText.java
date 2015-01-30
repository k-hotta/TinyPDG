package yoshikihigo.tinypdg.ast;

public class SourceText {

	private final String text;

	public SourceText(final String text) {
		this.text = text;
	}

	public final String getText() {
		return this.text;
	}

	public final String getText(final int startOffset, final int endOffset) {
		return this.text.substring(startOffset, endOffset + 1);
	}

}
