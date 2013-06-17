package yoshikihigo.tinypdg.scorpio.bellon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

public class Bellon {

	final float threshold;
	final String oracle;
	final String detectionResult;

	public static void main(final String[] args) {

		if (3 != args.length) {
			System.err.println("the number of command opetions must be three.");
			System.err
					.println("the first one is a threshold for matching with oracle.");
			System.err.println("the second one is file of oracle.");
			System.err.println("the third one is file of detection result.");
			System.exit(0);
		}

		final float threshold = Float.parseFloat(args[0]);
		final String oracle = args[1];
		final String detectionResult = args[2];
		final Bellon bellon = new Bellon(threshold, oracle, detectionResult);

		final List<ClonePairInfo> references = bellon.getClonepairs(new File(
				args[1]), true);
		final List<ClonePairInfo> candidates = bellon.getClonepairs(new File(
				args[2]), false);

		final Set<ClonePairInfo> okReferences = bellon.getOKDetectedReferences(
				candidates, references);
		final Set<ClonePairInfo> goodReferences = bellon
				.getGoodDetectedReferences(candidates, references);

		final Set<ClonePairInfo> okCandidates = bellon.getOKDetectedReferences(
				references, candidates);
		final Set<ClonePairInfo> goodCandidates = bellon
				.getGoodDetectedReferences(references, candidates);

		final int numberOfOKContiguousReferences = bellon
				.getNumberOfContiguousClone(okReferences);
		final int numberOfOKNoncontiguousReferences = bellon
				.getNumberOfNoncontiguousClone(okReferences);
		final int numberOfGOODContiguousReferences = bellon
				.getNumberOfContiguousClone(goodReferences);
		final int numberOfGOODNoncontiguousReferences = bellon
				.getNumberOfNoncontiguousClone(goodReferences);

		final int numberOfOKReferences = okReferences.size();
		final int numberOfGOODReferences = goodReferences.size();

		final int numberOfOKCandidates = okCandidates.size();
		final int numberOfGOODCandidates = goodCandidates.size();

		System.out.print("\"detected configuous references with OK\"");
		System.out.print(", ");
		System.out.print("\"detected non-configuous references with OK\"");
		System.out.print(", ");
		System.out.print("\"detected configuous references with GOOD\"");
		System.out.print(", ");
		System.out.print("\"detected non-configuous references with GOOD\"");
		System.out.print(", ");
		System.out.print("\"ok recall\"");
		System.out.print(", ");
		System.out.print("\"good recall\"");
		System.out.print(", ");
		System.out.print("\"ok precision\"");
		System.out.print(", ");
		System.out.println("\"good precision\"");

		System.out.print(Integer.toString(numberOfOKContiguousReferences));
		System.out.print(", ");
		System.out.print(Integer.toString(numberOfOKNoncontiguousReferences));
		System.out.print(", ");
		System.out.print(Integer.toString(numberOfGOODContiguousReferences));
		System.out.print(", ");
		System.out.print(Integer.toString(numberOfGOODNoncontiguousReferences));
		System.out.print(", ");

		System.out.print(Float.toString((float) numberOfOKReferences
				/ (float) references.size()));
		System.out.print(", ");
		System.out.print(Float.toString((float) numberOfGOODReferences
				/ (float) references.size()));
		System.out.print(", ");
		System.out.print(Float.toString((float) numberOfOKCandidates
				/ (float) candidates.size()));
		System.out.print(", ");
		System.out.println(Float.toString((float) numberOfGOODCandidates
				/ (float) candidates.size()));
	}

	Bellon(final float threshold, final String oracle,
			final String detectionResult) {
		this.threshold = threshold;
		this.oracle = oracle;
		this.detectionResult = detectionResult;
	}

	private List<ClonePairInfo> getClonepairs(final File file,
			final boolean oracle) {

		final List<ClonePairInfo> clonepairs = new ArrayList<ClonePairInfo>();

		try {
			final BufferedReader reader = new BufferedReader(new FileReader(
					file));
			while (reader.ready()) {
				final String line = reader.readLine();
				final ClonePairInfo pair = this.getClonepair(line, oracle);
				clonepairs.add(pair);
			}

			reader.close();

		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		return clonepairs;
	}

	private ClonePairInfo getClonepair(final String line, final boolean oracle) {

		final StringTokenizer lineTokenizer = new StringTokenizer(line, "\t");
		final String leftPath = lineTokenizer.nextToken();
		final String leftStartLine = lineTokenizer.nextToken();
		final String leftEndLine = lineTokenizer.nextToken();
		final String rightPath = lineTokenizer.nextToken();
		final String rightStartLine = lineTokenizer.nextToken();
		final String rightEndLine = lineTokenizer.nextToken();
		final int type;
		if (oracle) {
			type = Integer.parseInt(lineTokenizer.nextToken());
		} else {
			type = 0;
		}
		final String leftGaps = lineTokenizer.nextToken();
		final String rightGaps = lineTokenizer.nextToken();

		final CodeFragmentInfo leftFragment = new CodeFragmentInfo(leftPath,
				Integer.parseInt(leftStartLine), Integer.parseInt(leftEndLine));
		final CodeFragmentInfo rightFragment = new CodeFragmentInfo(rightPath,
				Integer.parseInt(rightStartLine),
				Integer.parseInt(rightEndLine));

		if (!leftGaps.equals("-")) {
			final StringTokenizer gapTokenizer = new StringTokenizer(leftGaps,
					", ");
			while (gapTokenizer.hasMoreTokens()) {
				final String gap = gapTokenizer.nextToken();
				leftFragment.remove(Integer.parseInt(gap));
			}
		}

		if (!rightGaps.equals("-")) {
			final StringTokenizer gapTokenizer = new StringTokenizer(rightGaps,
					", ");
			while (gapTokenizer.hasMoreTokens()) {
				final String gap = gapTokenizer.nextToken();
				rightFragment.remove(Integer.parseInt(gap));
			}
		}

		return new ClonePairInfo(leftFragment, rightFragment, type);
	}

	private boolean isOKClone(final ClonePairInfo candidate,
			final List<ClonePairInfo> references) {

		for (final ClonePairInfo reference : references) {

			if (candidate.left.path.equals(reference.left.path)
					&& candidate.right.path.equals(reference.right.path)) {

				final SortedSet<Integer> leftIntersection = new TreeSet<Integer>();
				leftIntersection.addAll(candidate.left);
				leftIntersection.retainAll(reference.left);

				final SortedSet<Integer> rightIntersection = new TreeSet<Integer>();
				rightIntersection.addAll(candidate.right);
				rightIntersection.retainAll(reference.right);

				final float ok = Math.min(Math.max(
						(float) leftIntersection.size()
								/ (float) candidate.left.size(),
						(float) leftIntersection.size()
								/ (float) reference.left.size()), Math.max(
						(float) rightIntersection.size()
								/ (float) candidate.right.size(),
						(float) rightIntersection.size()
								/ (float) reference.right.size()));

				if (0.7f <= ok) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean isGoodClone(final ClonePairInfo candidate,
			final List<ClonePairInfo> references) {

		for (final ClonePairInfo reference : references) {

			if (candidate.left.path.equals(reference.left.path)
					&& candidate.right.path.equals(reference.right.path)) {

				final SortedSet<Integer> leftIntersection = new TreeSet<Integer>();
				leftIntersection.addAll(candidate.left);
				leftIntersection.retainAll(reference.left);

				final SortedSet<Integer> leftUnion = new TreeSet<Integer>();
				leftUnion.addAll(candidate.left);
				leftUnion.addAll(reference.left);

				final SortedSet<Integer> rightIntersection = new TreeSet<Integer>();
				rightIntersection.addAll(candidate.right);
				rightIntersection.retainAll(reference.right);

				final SortedSet<Integer> rightUnion = new TreeSet<Integer>();
				rightUnion.addAll(candidate.right);
				rightUnion.addAll(reference.right);

				final float good = Math.min(
						(float) leftIntersection.size()
								/ (float) leftUnion.size(),
						(float) rightIntersection.size()
								/ (float) rightUnion.size());

				if (0.7f <= good) {
					return true;
				}
			}
		}

		return false;
	}

	private Set<ClonePairInfo> getOKDetectedReferences(
			final List<ClonePairInfo> candidates,
			final List<ClonePairInfo> references) {

		final Set<ClonePairInfo> detectedReferences = new HashSet<ClonePairInfo>();
		for (final ClonePairInfo reference : references) {
			if (this.isOKClone(reference, candidates)) {
				detectedReferences.add(reference);
			}
		}
		return detectedReferences;
	}

	private Set<ClonePairInfo> getGoodDetectedReferences(
			final List<ClonePairInfo> candidates,
			final List<ClonePairInfo> references) {

		final Set<ClonePairInfo> detectedReferences = new HashSet<ClonePairInfo>();
		for (final ClonePairInfo reference : references) {
			if (this.isGoodClone(reference, candidates)) {
				detectedReferences.add(reference);
			}
		}
		return detectedReferences;
	}

	private int getNumberOfContiguousClone(final Set<ClonePairInfo> pairs) {
		int number = 0;
		for (final ClonePairInfo pair : pairs) {
			if ((1 == pair.type) || (2 == pair.type)) {
				number++;
			}
		}
		return number;
	}

	private int getNumberOfNoncontiguousClone(final Set<ClonePairInfo> pairs) {
		int number = 0;
		for (final ClonePairInfo pair : pairs) {
			if (3 == pair.type) {
				number++;
			}
		}
		return number;
	}
}