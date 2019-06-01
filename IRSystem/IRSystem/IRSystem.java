package IRSystem;

import java.io.File;
import java.io.IOException;

public class IRSystem {

	public static void main(String[] args) throws IOException {
		try {
			if (args.length != 5) {
				System.out.println("ERROR: Only five four args for input .rar and 4 output files ");
				System.exit(0);
			}

			String inputFilePathRar = args[0].toString();
			String outputFilePathDict = args[1].toString();
			String outputFilePathPost = args[2].toString();
			String outputFilePathDoc = args[3].toString();
			String outFilePath = args[4].toString();
			// String outputFilePathDict = "/home/amit/Desktop/IR3Files/dictionary.csv";
			// String outputFilePathPost = "/home/amit/Desktop/IR3Files/posting.csv";
			// String outputFilePathDoc = "/home/amit/Desktop/IR3Files/doctable.txt";
			// String outFilePath = "/home/amit/Desktop/IR3Files/outFile.txt";

			File inputRarfile = new File(inputFilePathRar);
			File dictOutFile = new File(outputFilePathDict);
			File postingOutFile = new File(outputFilePathPost);
			File docOutFile = new File(outputFilePathDoc);
			File outFile = new File(outFilePath);
			CreateFiles.CreateIRFiles(inputRarfile, dictOutFile, postingOutFile, docOutFile);
			LoadFiles.loadIRfiles(dictOutFile, postingOutFile, docOutFile, outFile);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}


