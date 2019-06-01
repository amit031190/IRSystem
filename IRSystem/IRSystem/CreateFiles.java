package IRSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.csvreader.CsvWriter;
import com.github.junrar.extract.ExtractArchive;

public class CreateFiles {
	public static void CreateIRFiles(File inputRarfile, File dictOutFile, File postingOutFile, File docOutFile) {
		FileWriter postingFileWriter = null;
		CsvWriter postingCsvWriter = null;
		FileWriter dictFileWriter = null;
		CsvWriter dictCsvWriter = null;
		FileWriter docFileWriter = null;
		CsvWriter docCsvWriter = null;

		try {

			if (!inputRarfile.isFile()) {
				System.out.println("ERROR: input File doesnot exit in specified path");
				System.exit(0);
			}
			if (!inputRarfile.getName().endsWith(".rar")) {
				System.out.println("ERROR: (.rar) format/s are only supported for input file");
				System.exit(0);
			}
			if (!dictOutFile.getName().endsWith(".csv")) {
				System.out.println("ERROR: (.csv) format/s are only supported for output dictionary file");
				System.exit(0);
			}
			if (!postingOutFile.getName().endsWith(".csv")) {
				System.out.println("ERROR: (.csv) format/s are only supported for output posting file");
				System.exit(0);
			}
			if (!docOutFile.getName().endsWith(".txt")) {
				System.out.println("ERROR: (.txt) format/s are only supported for output doct table file");
				System.exit(0);
			}

			if (dictOutFile.exists()) {
				dictOutFile.delete();
			}
			if (postingOutFile.exists()) {
				postingOutFile.delete();
			}
			if (docOutFile.exists()) {
				dictOutFile.delete();
			}
			dictOutFile.createNewFile();
			postingOutFile.createNewFile();
			docOutFile.createNewFile();

			File destextractPath = new File(inputRarfile.getParent());
			extractRarFiles(inputRarfile, destextractPath);
			File[] htmlfiles = new File(destextractPath + "/" + inputRarfile.getName().replaceAll(".rar", ""))
					.listFiles();

			HashSet<String> dictTermHash = new HashSet<String>();
			List<TermDict> termDictList = new ArrayList<TermDict>();
			List<Posting> postingList = new ArrayList<Posting>();
			List<DocTable> docTableList = new ArrayList<DocTable>();
			int docid = 1;
			for (File htmlfile : htmlfiles) {
				ExtractTerms(htmlfile, dictTermHash, termDictList, postingList, docid);
				createDocTable(htmlfile, docTableList);
				docid++;

			}
			// sorting of PostingList
			Collections.sort(postingList,
					Comparator.comparing((Posting p1) -> p1.term).thenComparing((Posting p2) -> p2.docId));

			// sorting of dictionary
			Collections.sort(termDictList, Comparator.comparing(p1 -> p1.term));

			postingFileWriter = new FileWriter(postingOutFile);
			postingCsvWriter = new CsvWriter(postingFileWriter, ',');

			postingCsvWriter.write("docId");
			postingCsvWriter.write("TermFreq");
			postingCsvWriter.endRecord();
			for (Posting postingElement : postingList) {
				// postingCsvWriter.write(postingElement.term);
				postingCsvWriter.write(Integer.toString(postingElement.docId));
				postingCsvWriter.write(Integer.toString(postingElement.tf));
				postingCsvWriter.endRecord();
			}

			dictFileWriter = new FileWriter(dictOutFile);
			dictCsvWriter = new CsvWriter(dictFileWriter, ',');

			dictCsvWriter.write("Term");
			dictCsvWriter.write("DF");
			dictCsvWriter.write("OffSet");
			dictCsvWriter.endRecord();

			for (int i = 0; i < termDictList.size(); i++) {
				// updating offset of dictionary
				termDictList.get(i).offSet = (i == 0) ? 0 : termDictList.get(i - 1).offSet + termDictList.get(i - 1).df;
				dictCsvWriter.write(termDictList.get(i).term);
				dictCsvWriter.write(Integer.toString(termDictList.get(i).df));
				dictCsvWriter.write(Integer.toString(termDictList.get(i).offSet));
				dictCsvWriter.endRecord();
			}

			docFileWriter = new FileWriter(docOutFile);
			docCsvWriter = new CsvWriter(docFileWriter, '|');

			docCsvWriter.write("FileName");
			docCsvWriter.write("Title");
			docCsvWriter.write("Reviewer");
			docCsvWriter.write("Rate");
			docCsvWriter.write("Snippet");
			docCsvWriter.endRecord();

			for (DocTable docTable : docTableList) {

				docCsvWriter.write(docTable.fileName);
				docCsvWriter.write(docTable.title);
				docCsvWriter.write(docTable.reviewer);
				docCsvWriter.write(docTable.rate);
				docCsvWriter.write(docTable.snippet);
				docCsvWriter.endRecord();
			}
			// System.out.println(termDictList.size());
			DeleteDirectory(destextractPath.getPath() + "/" + inputRarfile.getName().replaceAll(".rar", ""));

		} catch (Exception e) {
			System.out.println(e.getStackTrace());
		} finally {
			if (postingCsvWriter != null)
				try {
					postingCsvWriter.close();
				} catch (Exception ex) {
				}
			if (postingFileWriter != null)
				try {
					postingFileWriter.close();
				} catch (Exception ex) {
				}
			if (dictCsvWriter != null)
				try {
					dictCsvWriter.close();
				} catch (Exception ex) {
				}
			if (dictFileWriter != null)
				try {
					dictFileWriter.close();
				} catch (Exception ex) {
				}
			if (docCsvWriter != null)
				try {
					docCsvWriter.close();
				} catch (Exception ex) {
				}
			if (docFileWriter != null)
				try {
					docFileWriter.close();
				} catch (Exception ex) {
				}
		}

	}

	public static void ExtractTerms(File file, HashSet<String> dictTermHash, List<TermDict> termDictList,
			List<Posting> postingList, int docId) {
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try {

			fileReader = new FileReader(file);
			bufferedReader = new BufferedReader(fileReader);

			String line = "";

			StringBuilder htmlStringBuilder = new StringBuilder();

			while ((line = bufferedReader.readLine()) != null) {
				// building one long string by appending each line read
				htmlStringBuilder.append(line + " ");
			}

			String plainText = htmlStringBuilder.toString().toLowerCase().replaceAll("\\<.*?>", " ")
					.replaceAll("([^ ])(-)([^ ])", "$1 $3"); // replace html tags and "-" if it comes in between the
																// word

			plainText = plainText.replace(", ", " ").replace(". ", " ").replace("? ", " ").replace(": ", " ")
					.replace("; ", " ").replace("! ", " "); // replacing ",.?:;!" if followed by space

			plainText = plainText.replace(" '", " ").replace(" \"", " ").replace(" (", " ").replace(" )", " ")
					.replace(" [", " ").replace(" ]", " "); // replace "'"()[]" if are at start of token

			plainText = plainText.replace("' ", " ").replace("\" ", " ").replace("( ", " ").replace(") ", " ")
					.replace("[ ", " ").replace("] ", " "); // replace "'"()[]" if are at end of word
			// array of stop words
			List<String> stopWords = Arrays.asList(
					("and, a, the, an, by, from, for, hence, of, the, with, in, within, who, when, where, why, how, whom, have, had, has, not, for, but, do, does, done")
							.toLowerCase().replaceAll(" ", "").split(","));
			String[] dictTokens = plainText.split(" ");
			// used hashset to check and skip duplicates
			HashSet<String> filterDictTokens = new HashSet<String>();
			Posting posting;
			TermDict termDict;
			for (String token : dictTokens) {
				// System.out.println(token);
				token = token.trim();
				if (isNumeric(token) || (token.length() > 1 && !stopWords.contains(token))) {
					// removes apostrophes from word
					token = token.replace("'", "");
					// Stemming for words
					if (token.endsWith("ies") && !token.endsWith("eies") && !token.endsWith("aies")) {
						token = token.replaceAll("ies$", "y");
					} else if (token.endsWith("es") && !token.endsWith("aes") && !token.endsWith("ees")
							&& !token.endsWith("oes")) {
						token = token.replaceAll("es$", "e");
					} else if (token.endsWith("s") && !token.endsWith("us") && !token.endsWith("ss")) {
						token = token.replaceAll("s$", "");
					}

					if (filterDictTokens.add(token)) {// checks for duplicate values
						posting = new Posting();
						posting.docId = docId;
						posting.term = token;
						posting.tf = 1;

						postingList.add(posting);
						termDict = new TermDict();
						termDict.df = 1;
						termDict.offSet = 0;
						termDict.term = token;
						if (dictTermHash.add(token)) {
							termDictList.add(termDict);
						} else {
							for (TermDict termDictElement : termDictList) {

								if (termDictElement.term.equals(token)) {
									termDictElement.df = termDictElement.df + 1;
									// break;
								}
							}
						}
					} else {

						for (Posting pstingElement : postingList) {

							if (pstingElement.term.equals(token) && pstingElement.docId == docId) {
								pstingElement.tf = pstingElement.tf + 1;
								// break;
							}
						}
					}
				}
			}

		} catch (Exception e) {
			System.out.println(e.getStackTrace());
		} finally {

			if (bufferedReader != null)
				try {
					bufferedReader.close();
				} catch (Exception ex) {

				}
			if (fileReader != null)
				try {
					fileReader.close();
				} catch (Exception ex) {

				}
		}
	}
	
	
	public static void createDocTable(File htmlFile, List<DocTable> doctableList) {
		try {
			FileReader fileReader = new FileReader(htmlFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line = "";

			StringBuilder htmlStringBuilder = new StringBuilder();

			while ((line = bufferedReader.readLine()) != null) {
				// building one long string by appending each line read
				htmlStringBuilder.append(line + " ");
			}
			fileReader.close();
			bufferedReader.close();
			DocTable docTable = new DocTable();
			Document doc = null;

			doc = Jsoup.parse(htmlStringBuilder.toString());// , "UTF-8", "http://example.com/");

			docTable.title = doc.title();
			docTable.fileName = htmlFile.getName();

			for (int i = 0; i < doc.select("a").size(); i++) {
				// System.out.println(doc.select("a").get(i).attr("href"));
				if (doc.select("a").get(i).attr("href").toLowerCase().contains("reviewsby")) {
					docTable.reviewer = doc.select("a").get(i).text();

					break;
				}
			}
			String body = doc.body().text().replace("\n", " ").replace("\n", " ");
			String[] words = null;
			if (body.toLowerCase().contains("capsule review:")) {
				int reviewIndex = body.toLowerCase().indexOf("capsule review:");
				words = body.substring(reviewIndex + 15).split(" ");
			} else {
				// System.out.println(body);
				words = body.split(" ");
			}
			int wordCount = 0;
			for (String word : words) {
				if (!word.isEmpty()) {
					docTable.snippet += word + " ";
					wordCount++;
					if (wordCount == 50) {
						break;
					}
				}
			}

			// Rate
			String movieRate = docTable.title.toLowerCase().replace("reveiw for", "") + " rate";

			if (body.toLowerCase().contains("-4 to +4 scale")) {
				int reviewIndex = body.toLowerCase().indexOf("-4 to +4 scale");
				reviewIndex--;
				while (!Character.isDigit(body.charAt(reviewIndex))) {
					reviewIndex--;
				}
				int rate = Integer.valueOf(body.charAt(reviewIndex));
				if (rate >= 0) {
					docTable.rate = "P";
				} else {
					docTable.rate = "N";
				}
			} else if (body.toLowerCase().contains("capsule review") || body.toLowerCase().contains("capsule")) {

				String capsuleReview = doc.select(":contains(capsule)").get(1).text();
				int posCount = 0;
				int negCount = 0;
				for (String capRevElement : capsuleReview.split(" ")) {
					for (String pos : postNegWords.posWords) {
						if (capRevElement.trim().toUpperCase().contains(pos)) {
							posCount++;
						}
					}
					for (String neg : postNegWords.negWords) {
						if (capRevElement.trim().toUpperCase().contains(neg)) {
							negCount++;
						}
					}
				}
				if ((posCount - negCount) > 0) {
					docTable.rate = "P";
				} else {
					docTable.rate = "N";
				}
			} else if (body.toLowerCase().contains(movieRate)) {

				String movieRateText = doc.select(":contains(" + movieRate + ")").get(1).text();
				int movieRateIndex = movieRateText.indexOf(movieRate);
				movieRateText = movieRateText.substring(movieRateIndex + movieRate.length());
				boolean isPos = false;
				boolean isNeg = false;
				for (String pos : postNegWords.posWords) {
					if (movieRateText.toUpperCase().contains(pos)) {
						isPos = true;
					}
				}
				if (isPos) {
					docTable.rate = "P";
				} else {
					for (String neg : postNegWords.negWords) {
						if (movieRateText.toUpperCase().contains(neg)) {
							isNeg = true;
						}
					}
					if (isNeg) {
						docTable.rate = "N";
					} else {
						docTable.rate = "NA";
					}
				}

			} else {
				docTable.rate = "NA";
			}
			doctableList.add(docTable);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void extractRarFiles(File inputRarfile, File destextractPath) {
		ExtractArchive extractArchive = new ExtractArchive();
		extractArchive.extractArchive(inputRarfile, destextractPath);

	}
	
	public static boolean isNumeric(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
	
	public static void DeleteDirectory(String dirPath) {
		File filedir = new File(dirPath);

		if (filedir.exists()) {
			File[] files = filedir.listFiles();
			for (File file : files) {
				file.delete();
			}
			filedir.delete();
		}
	}

}
