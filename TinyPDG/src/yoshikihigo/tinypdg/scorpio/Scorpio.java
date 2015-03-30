package yoshikihigo.tinypdg.scorpio;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import yoshikihigo.tinypdg.ast.OffsetBasedTinyPDGASTVisitor;
import yoshikihigo.tinypdg.ast.TinyPDGASTVisitor;
import yoshikihigo.tinypdg.cfg.node.CFGNodeFactory;
import yoshikihigo.tinypdg.pdg.PDG;
import yoshikihigo.tinypdg.pdg.edge.PDGEdge;
import yoshikihigo.tinypdg.pdg.node.PDGNode;
import yoshikihigo.tinypdg.pdg.node.PDGNodeFactory;
import yoshikihigo.tinypdg.pe.MethodInfo;
import yoshikihigo.tinypdg.scorpio.data.ClonePairInfo;
import yoshikihigo.tinypdg.scorpio.data.PDGPairInfo;
import yoshikihigo.tinypdg.scorpio.io.BellonWriter;
import yoshikihigo.tinypdg.scorpio.io.Writer;

public class Scorpio {

	public static void main(String[] args) {

		try {

			final Options options = new Options();

			{
				final Option d = new Option("d", "directory", true,
						"target directory");
				d.setArgName("directory");
				d.setArgs(1);
				d.setRequired(true);
				options.addOption(d);
			}

			{
				final Option ad = new Option("ad", "another-directory", true,
						"another target directory");
				ad.setArgName("another-directory");
				ad.setArgs(1);
				ad.setRequired(false);
				options.addOption(ad);
			}

			{
				final Option o = new Option("o", "output", true, "output file");
				o.setArgName("file");
				o.setArgs(1);
				o.setRequired(true);
				options.addOption(o);
			}

			{
				final Option s = new Option("s", "size", true, "size");
				s.setArgName("size");
				s.setArgs(1);
				s.setRequired(true);
				options.addOption(s);
			}

			{
				final Option t = new Option("t", "thread", true,
						"number of threads");
				t.setArgName("thread");
				t.setArgs(1);
				t.setRequired(false);
				options.addOption(t);
			}

			{
				final Option cross = new Option("cross", "cross-project-only",
						true, "whether to detect cross project clones only");
				cross.setArgName("on or off");
				cross.setRequired(false);
				options.addOption(cross);
			}

			{
				final Option lowmem = new Option("lowmem", "low-memory-mode",
						true, "whether to run on the low memory mode");
				lowmem.setArgName("on or off");
				lowmem.setRequired(false);
				options.addOption(lowmem);
			}

			{
				final Option v = new Option("v", "verbose", true,
						"verbose output");
				v.setArgName("on or off");
				v.setRequired(false);
				options.addOption(v);
			}

			{
				final Option C = new Option("C", "control", true,
						"use of control dependency");
				C.setArgName("on or off");
				C.setArgs(1);
				C.setRequired(false);
				options.addOption(C);
			}

			{
				final Option D = new Option("D", "data", true,
						"use of data dependency");
				D.setArgName("on or off");
				D.setArgs(1);
				D.setRequired(false);
				options.addOption(D);
			}

			{
				final Option E = new Option("E", "execution", true,
						"use of execution dependency");
				E.setArgName("on or off");
				E.setArgs(1);
				E.setRequired(false);
				options.addOption(E);
			}

			{
				final Option M = new Option("M", "merging", true,
						"merging consecutive similar nodes");
				M.setArgName("on or off");
				M.setArgs(1);
				M.setRequired(false);
				options.addOption(M);
			}

			final CommandLineParser parser = new PosixParser();
			final CommandLine cmd = parser.parse(options, args);

			final File target = new File(cmd.getOptionValue("d"));
			if (!target.exists()) {
				System.err
						.println("specified directory or file does not exist.");
				System.exit(0);
			}

			final String output = cmd.getOptionValue("o");
			final int SIZE_THRESHOLD = Integer
					.parseInt(cmd.getOptionValue("s"));
			final int NUMBER_OF_THREADS = cmd.hasOption("t") ? Integer
					.parseInt(cmd.getOptionValue("t")) : 1;

			boolean useOfControl = !cmd.hasOption("C");
			if (!useOfControl) {
				if (cmd.getOptionValue("C").equals("on")) {
					useOfControl = true;
				} else if (cmd.getOptionValue("C").equals("off")) {
					useOfControl = false;
				} else {
					System.err
							.println("option of \"-C\" must be \"on\" or \"off\".");
				}
			}

			boolean useOfData = !cmd.hasOption("D");
			if (!useOfData) {
				if (cmd.getOptionValue("D").equals("on")) {
					useOfData = true;
				} else if (cmd.getOptionValue("D").equals("off")) {
					useOfData = false;
				} else {
					System.err
							.println("option of \"-D\" must be \"on\" or \"off\".");
				}
			}

			boolean useOfExecution = !cmd.hasOption("E");
			if (!useOfExecution) {
				if (cmd.getOptionValue("E").equals("on")) {
					useOfExecution = true;
				} else if (cmd.getOptionValue("E").equals("off")) {
					useOfExecution = false;
				} else {
					System.err
							.println("option of \"-E\" must be \"on\" or \"off\".");
				}
			}

			boolean useOfMerging = !cmd.hasOption("M");
			if (!useOfMerging) {
				if (cmd.getOptionValue("M").equals("on")) {
					useOfMerging = true;
				} else if (cmd.getOptionValue("M").equals("off")) {
					useOfMerging = false;
				} else {
					System.err
							.println("option of \"-M\" must be \"on\" or \"off\".");
				}
			}

			if (!useOfExecution && useOfMerging) {
				useOfMerging = false;
			}

			boolean crossProjectOnly = false;
			if (cmd.hasOption("cross")) {
				if (cmd.getOptionValue("cross").equals("on")) {
					crossProjectOnly = true;
				} else if (cmd.getOptionValue("cross").equals("off")) {
					crossProjectOnly = false;
				} else {
					System.err
							.println("option of \"-cross\" must be \"on\" or \"off\".");
				}
			}

			boolean lowMemoryMode = false;
			if (cmd.hasOption("lowmem")) {
				if (cmd.getOptionValue("lowmem").equals("on")) {
					lowMemoryMode = true;
				} else if (cmd.getOptionValue("lowmem").equals("off")) {
					lowMemoryMode = false;
				} else {
					System.err
							.println("option of \"-lowmem\" must be \"on\" or \"off\".");
				}
			}

			// default verbose level is "off"
			if (cmd.hasOption("v")) {
				if (cmd.getOptionValue("v").equals("on")) {
					Message.setVerbose(true);
				} else if (cmd.getOptionValue("v").equals("off")) {
					Message.setVerbose(false);
				} else {
					System.err
							.println("option of \"-v\" must be \"on\" or \"off\".");
				}
			}

			File anotherTarget = null;
			if (cmd.hasOption("ad")) {
				anotherTarget = new File(cmd.getOptionValue("ad"));
				if (!anotherTarget.exists()) {
					System.err
							.println("specified directory or file does not exist.");
					System.exit(0);
				}
			}

			if (crossProjectOnly && anotherTarget == null) {
				System.err
						.println("detecting cross project only is ON, but no second directory or file has been specified");
				System.exit(0);
			}

			final long time1 = System.nanoTime();
			System.out.print("generating PDGs ... ");
			Message.log("");
			final PDG[] pdgArray;
			{
				final List<File> files = getFiles(target);

				if (anotherTarget != null) {
					files.addAll(getFiles(anotherTarget));
				}

				int count = 0;
				final int numOfFiles = files.size();

				final List<MethodInfo> methods = new ArrayList<MethodInfo>();
				for (final File file : files) {
					Message.log("\t[" + (++count) + "/" + numOfFiles
							+ "] building an AST for " + file.getAbsolutePath());

					final CompilationUnit unit = TinyPDGASTVisitor
							.createAST(file);
					final ASTVisitor visitor;

					if (lowMemoryMode) {
						visitor = new OffsetBasedTinyPDGASTVisitor(
								file.getAbsolutePath(), unit, methods);
					} else {
						visitor = new TinyPDGASTVisitor(file.getAbsolutePath(),
								unit, methods);
					}

					unit.accept(visitor);
				}
				
				long memoryElapsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				System.out.println("MEMORY: " + (memoryElapsed / 1024));

				final SortedSet<PDG> pdgs = Collections
						.synchronizedSortedSet(new TreeSet<PDG>());
				final CFGNodeFactory cfgNodeFactory = new CFGNodeFactory();
				final PDGNodeFactory pdgNodeFactory = new PDGNodeFactory();
				final Thread[] pdgGenerationThreads = new Thread[NUMBER_OF_THREADS];
				for (int i = 0; i < pdgGenerationThreads.length; i++) {
					pdgGenerationThreads[i] = new Thread(
							new PDGGenerationThread(methods, pdgs,
									cfgNodeFactory, pdgNodeFactory,
									useOfControl, useOfData, useOfExecution,
									useOfMerging, SIZE_THRESHOLD));
					pdgGenerationThreads[i].start();
				}
				for (final Thread thread : pdgGenerationThreads) {
					try {
						thread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				pdgArray = pdgs.toArray(new PDG[0]);
			}
			System.out.print("done: ");
			final long time2 = System.nanoTime();
			printTime(time2 - time1);

			System.out.print("calculating hash values ... ");
			Message.log("");
			final SortedMap<PDG, SortedMap<PDGNode<?>, Integer>> mappingPDGToPDGNodes = Collections
					.synchronizedSortedMap(new TreeMap<PDG, SortedMap<PDGNode<?>, Integer>>());
			final SortedMap<PDG, SortedMap<PDGEdge, Integer>> mappingPDGToPDGEdges = Collections
					.synchronizedSortedMap(new TreeMap<PDG, SortedMap<PDGEdge, Integer>>());
			{
				final Thread[] hashCalculationThreads = new Thread[NUMBER_OF_THREADS];
				for (int i = 0; i < hashCalculationThreads.length; i++) {
					hashCalculationThreads[i] = new Thread(
							new HashCalculationThread(pdgArray,
									mappingPDGToPDGNodes, mappingPDGToPDGEdges));
					hashCalculationThreads[i].start();
				}
				for (final Thread thread : hashCalculationThreads) {
					try {
						thread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			System.out.print("done: ");
			final long time3 = System.nanoTime();
			printTime(time3 - time2);

			System.out.print("detecting clone pairs ... ");
			Message.log("");
			final SortedSet<ClonePairInfo> clonepairs = Collections
					.synchronizedSortedSet(new TreeSet<ClonePairInfo>());
			{
				int numIgnored = 0;
				final List<PDGPairInfo> pdgpairs = new ArrayList<PDGPairInfo>();
				Message.log("\tmaking PDG pairs ... ");
				for (int i = 0; i < pdgArray.length; i++) {
					for (int j = i + 1; j < pdgArray.length; j++) {
						final PDG pdg1 = pdgArray[i];
						final PDG pdg2 = pdgArray[j];

						if (!crossProjectOnly
								|| isCrossProject(pdg1, pdg2, target,
										anotherTarget)) {
							pdgpairs.add(new PDGPairInfo(pdgArray[i],
									pdgArray[j]));
						} else {
							// Message.log("\t\tignore the PDG pair \"" +
							// pdg1.unit.name + " in "
							// + pdg1.unit.path + "\" and \""
							// + pdg2.unit.name + " in " + pdg2.unit.path
							// + "\"");
							numIgnored++;
						}
					}
				}
				Message.log("\tdone: the number of ignored PDG pairs is "
						+ numIgnored);

				final PDGPairInfo[] pdgpairArray = pdgpairs
						.toArray(new PDGPairInfo[0]);
				final Thread[] slicingThreads = new Thread[NUMBER_OF_THREADS];
				for (int i = 0; i < slicingThreads.length; i++) {
					slicingThreads[i] = new Thread(new SlicingThread(
							pdgpairArray, pdgArray, mappingPDGToPDGNodes,
							mappingPDGToPDGEdges, clonepairs, SIZE_THRESHOLD,
							crossProjectOnly));
					slicingThreads[i].start();
				}
				for (final Thread thread : slicingThreads) {
					try {
						thread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			System.out.print("done: ");
			final long time4 = System.nanoTime();
			printTime(time4 - time3);

			System.out.print("writing to a file ... ");
			final Writer writer = new BellonWriter(output, clonepairs);
			writer.write();
			System.out.print("done: ");
			final long time5 = System.nanoTime();
			printTime(time5 - time4);

			System.out.print("total elapsed time: ");
			printTime(time5 - time1);

			System.out.print("number of comparisons: ");
			printNumberOfComparison(Slicing.getNumberOfComparison());

		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(0);
		}
	}

	private static List<File> getFiles(final File file) {

		final List<File> files = new ArrayList<File>();

		if (file.isFile()) {
			if (file.getName().endsWith(".java")) {
				files.add(file);
			}
		}

		else if (file.isDirectory()) {
			for (final File child : file.listFiles()) {
				files.addAll(getFiles(child));
			}
		}

		else {
			assert false : "\"file\" is invalid.";
		}

		return files;
	}

	private static void printNumberOfRemoval(final long number) {
		System.out.print("number of removed edges: ");
		System.out.println(String.format("%1$,3d", number));
	}

	private static void printNumberOfComparison(final long number) {
		System.out.println(String.format("%1$,3d", number));
	}

	private static void printTime(final long time) {
		final long micro = time / 1000;
		final long mili = micro / 1000;
		final long sec = mili / 1000;

		final long hour = sec / 3600;
		final long minute = (sec % 3600) / 60;
		final long second = (sec % 3600) % 60;

		if (1l == hour) {
			System.out.print(hour);
			System.out.print(" hour ");
		} else if (1l < hour) {
			System.out.print(hour);
			System.out.print(" hours ");
		}

		if (1l == minute) {
			System.out.print(minute);
			System.out.print(" minute ");
		} else if (1l < minute) {
			System.out.print(minute);
			System.out.print(" minutes ");
		} else if ((0l == minute) && (1l <= hour)) {
			System.out.print(" 0 minute ");
		}

		if (2 <= second) {
			System.out.print(second);
			System.out.println(" seconds.");
		} else {
			System.out.print(second);
			System.out.println(" second.");
		}
	}

	private static boolean isCrossProject(final PDG pdg1, final PDG pdg2,
			final File root1, final File root2) {
		final String rootPath1 = root1.getAbsolutePath();
		final String rootPath2 = root2.getAbsolutePath();

		final boolean check1 = pdg1.unit.path.startsWith(rootPath1)
				&& pdg2.unit.path.startsWith(rootPath2);
		final boolean check2 = pdg1.unit.path.startsWith(rootPath2)
				&& pdg2.unit.path.startsWith(rootPath1);

		return check1 || check2;
	}

}