package IRSystem;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import com.csvreader.CsvReader;

public class LoadFiles {
	@SuppressWarnings("resource")
	public static void loadIRfiles(File dictInputFile, File postingInputFile, File docInputFile, File outFile) {
		FileReader postingFile = null;
		CsvReader postingCsv = null;
		FileReader dictFile = null;
		CsvReader dictCsv = null;
		FileReader docFile = null;
		CsvReader docCsv = null;
		FileWriter outFileWriter = null;
		PrintWriter outFileBufWriter = null;

		try {
			if (!outFile.getName().endsWith(".txt")) {
				System.out.println("ERROR: (.txt) format/s are only supported for output result file");
				System.exit(0);
			}
			if (outFile.exists()) {
				outFile.delete();
			}
			outFile.createNewFile();

			List<TermDict> termDictList = new ArrayList<TermDict>();
			List<Posting> postingList = new ArrayList<Posting>();
			List<DocTable> docTableList = new ArrayList<DocTable>();

			dictFile = new FileReader(dictInputFile);
			dictCsv = new CsvReader(dictFile, ',');
			dictCsv.readHeaders();
			TermDict termDict;
			while (dictCsv.readRecord()) {
				termDict = new TermDict();
				termDict.term = dictCsv.get("Term");
				termDict.df = Integer.parseInt(dictCsv.get("DF"));
				termDict.offSet = Integer.parseInt(dictCsv.get("OffSet"));
				termDictList.add(termDict);
			}
			dictCsv.close();

			postingFile = new FileReader(postingInputFile);
			postingCsv = new CsvReader(postingFile, ',');
			postingCsv.readHeaders();
			Posting posting;
			while (postingCsv.readRecord()) {
				posting = new Posting();
				posting.docId = Integer.parseInt(postingCsv.get("docId"));
				posting.tf = Integer.parseInt(postingCsv.get("TermFreq"));
				postingList.add(posting);
			}
			postingCsv.close();

			docFile = new FileReader(docInputFile);
			docCsv = new CsvReader(docFile, '|');
			docCsv.readHeaders();
			DocTable doctable;
			while (docCsv.readRecord()) {
				doctable = new DocTable();
				doctable.fileName = docCsv.get("FileName");
				doctable.title = docCsv.get("Title");
				doctable.reviewer = docCsv.get("Reviewer");
				doctable.rate = docCsv.get("Rate");
				doctable.snippet = docCsv.get("Snippet");
				docTableList.add(doctable);

			}
			docCsv.close();

			Scanner scnr;
			String queryTerms[];
			outFileWriter = new FileWriter(outFile);
			outFileBufWriter = new PrintWriter(outFileWriter);
			while (true) {
				System.out.println("Enter Query here:");
				scnr = new Scanner(System.in);
				String fullQuery = scnr.nextLine();
				queryTerms = fullQuery.split(" ");
				List<TermDict> rtrvdDicList = new ArrayList<TermDict>();
				ArrayList<Integer> rtrvdocid = new ArrayList<Integer>();
				ArrayList<Integer> rtrvdocidFinal = new ArrayList<Integer>();

				List<TermDict> rtrvdDicListNot = new ArrayList<TermDict>();
				ArrayList<Integer> rtrvdocidNot = new ArrayList<Integer>();
				ArrayList<Integer> rtrvdocidFinalNot = new ArrayList<Integer>();
				List<DocTable> rtrvdDocList = new ArrayList<DocTable>();
				if (queryTerms.length > 0) {
					if ((queryTerms[0].toUpperCase().equals("EXIT"))) {
						break;
					}

				}
				if (queryTerms.length > 1) {
					if ((queryTerms[0].equals("AND"))) {
						String splitquery[] = fullQuery.split("AND");

						String andTerms[] = splitquery[1].split(" ");

						for (int i = 0; i < termDictList.size(); i++) {
							for (int queryIndex = 1; queryIndex < andTerms.length; queryIndex++) {
								if (termDictList.get(i).term.equals(andTerms[queryIndex].toLowerCase())) {
									rtrvdDicList.add((TermDict) termDictList.get(i));

								}
							}
						}
						for (int i = 0; i < rtrvdDicList.size(); i++) {
							for (int offset = rtrvdDicList.get(i).offSet; offset < (rtrvdDicList.get(i).offSet
									+ rtrvdDicList.get(i).df); offset++) {
								rtrvdocid.add(postingList.get(offset).docId);

							}
						}
						HashSet<Integer> docIdhash = new HashSet<Integer>();
						for (int docIndex = 0; docIndex < rtrvdocid.size(); docIndex++) {
							int docCout = 1;
							for (int docFinalIndex = docIndex + 1; docFinalIndex < rtrvdocid.size(); docFinalIndex++) {

								if (rtrvdocid.get(docIndex) == rtrvdocid.get(docFinalIndex)) {
									docCout++;
								}
							}

							if (docCout == (andTerms.length - 1)) {
								if (docIdhash.add(rtrvdocid.get(docIndex))) {
									rtrvdocidFinal.add(rtrvdocid.get(docIndex));
								}
							}
						}

						// AND Not
						if (splitquery.length > 2) {
							String andNotTerms[] = splitquery[2].split("NOT")[1].split(" ");

							for (int i = 0; i < termDictList.size(); i++) {
								for (int queryIndex = 1; queryIndex < andNotTerms.length; queryIndex++) {
									if (termDictList.get(i).term.equals(andNotTerms[queryIndex].toLowerCase())) {
										rtrvdDicListNot.add((TermDict) termDictList.get(i));

									}
								}
							}
							for (int i = 0; i < rtrvdDicListNot.size(); i++) {
								for (int offset = rtrvdDicListNot.get(i).offSet; offset < (rtrvdDicListNot.get(i).offSet
										+ rtrvdDicListNot.get(i).df); offset++) {
									rtrvdocidNot.add(postingList.get(offset).docId);

								}
							}
							HashSet<Integer> docIdhashNot = new HashSet<Integer>();
							for (int docIndex = 0; docIndex < rtrvdocidNot.size(); docIndex++) {
								int docCout = 1;
								for (int docFinalIndex = docIndex + 1; docFinalIndex < rtrvdocidNot
										.size(); docFinalIndex++) {

									if (rtrvdocidNot.get(docIndex) == rtrvdocidNot.get(docFinalIndex)) {
										docCout++;
									}
								}

								if (docCout == (andNotTerms.length - 1)) {
									if (docIdhashNot.add(rtrvdocidNot.get(docIndex))) {
										rtrvdocidFinalNot.add(rtrvdocidNot.get(docIndex));
									}
								}
							}
						}
						rtrvdocidFinal.removeAll(rtrvdocidFinalNot);
						for (int i = 0; i < rtrvdocidFinal.size(); i++) {

							DocTable doctabl = new DocTable();
							doctabl.fileName = docTableList.get(rtrvdocidFinal.get(i) - 1).fileName;
							doctabl.title = docTableList.get(rtrvdocidFinal.get(i) - 1).title;
							doctabl.reviewer = docTableList.get(rtrvdocidFinal.get(i) - 1).reviewer;
							doctabl.rate = docTableList.get(rtrvdocidFinal.get(i) - 1).rate;
							doctabl.snippet = docTableList.get(rtrvdocidFinal.get(i) - 1).snippet;

							rtrvdDocList.add(doctabl);
						}

					} else if (queryTerms[0].equals("OR")) {
						for (int i = 0; i < termDictList.size(); i++) {
							for (int queryIndex = 1; queryIndex < queryTerms.length; queryIndex++) {
								if (termDictList.get(i).term.equals(queryTerms[queryIndex].toLowerCase())) {
									rtrvdDicList.add((TermDict) termDictList.get(i));
								}
							}
						}

						for (int i = 0; i < rtrvdDicList.size(); i++) {
							for (int offset = rtrvdDicList.get(i).offSet; offset < (rtrvdDicList.get(i).offSet
									+ rtrvdDicList.get(i).df); offset++) {
								rtrvdocid.add(postingList.get(offset).docId);

							}
						}

						HashSet<Integer> docIdhash = new HashSet<Integer>();
						for (int docIndex = 0; docIndex < rtrvdocid.size(); docIndex++) {
							if (docIdhash.add(rtrvdocid.get(docIndex))) {
								rtrvdocidFinal.add(rtrvdocid.get(docIndex));
							}
						}

						for (int i = 0; i < rtrvdocidFinal.size(); i++) {
							DocTable doctabl = new DocTable();
							doctabl.fileName = docTableList.get(rtrvdocidFinal.get(i) - 1).fileName;
							doctabl.title = docTableList.get(rtrvdocidFinal.get(i) - 1).title;
							doctabl.reviewer = docTableList.get(rtrvdocidFinal.get(i) - 1).reviewer;
							doctabl.rate = docTableList.get(rtrvdocidFinal.get(i) - 1).rate;
							doctabl.snippet = docTableList.get(rtrvdocidFinal.get(i) - 1).snippet;

							rtrvdDocList.add(doctabl);
						}
					} else {
						System.out.println("please provide correct query input");
					}

					if (rtrvdDocList.size() > 0) {
						List<DocTable> docTableListP = new ArrayList<DocTable>();
						List<DocTable> docTableListN = new ArrayList<DocTable>();
						List<DocTable> docTableListNA = new ArrayList<DocTable>();
						for (DocTable docr : rtrvdDocList) {
							if (docr.rate.equals("P")) {
								docTableListP.add(docr);
							} else if (docr.rate.equals("N")) {
								docTableListN.add(docr);
							} else {
								docTableListNA.add(docr);
							}

						}
						Collections.sort(docTableListP, Comparator.comparing((DocTable d1) -> d1.fileName));
						Collections.sort(docTableListN, Comparator.comparing((DocTable d1) -> d1.fileName));
						Collections.sort(docTableListNA, Comparator.comparing((DocTable d1) -> d1.fileName));

						outFileBufWriter.write("Query:" + fullQuery);
						outFileBufWriter.write(
								"\n---------------------------------------------------------------------------------------------------");
						for (DocTable docTableP : docTableListP) {
							System.out.println("filename: " + docTableP.fileName);
							System.out.println("title: " + docTableP.title);
							System.out.println("reviewer: " + docTableP.reviewer);
							System.out.println("rate: " + docTableP.rate);
							System.out.println("snippet: " + docTableP.snippet);

							outFileBufWriter.write("\nfilename: " + docTableP.fileName);
							outFileBufWriter.write("\ntitle: " + docTableP.title);
							outFileBufWriter.write("\nreviewer: " + docTableP.reviewer);
							outFileBufWriter.write("\nrate: " + docTableP.rate);
							outFileBufWriter.write("\nsnippet: " + docTableP.snippet);
							outFileBufWriter.write("\n");
						}
						for (DocTable docTableN : docTableListN) {
							System.out.println("filename: " + docTableN.fileName);
							System.out.println("title: " + docTableN.title);
							System.out.println("reviewer: " + docTableN.reviewer);
							System.out.println("rate: " + docTableN.rate);
							System.out.println("snippet: " + docTableN.snippet);

							outFileBufWriter.write("\nfilename: " + docTableN.fileName);
							outFileBufWriter.write("\ntitle: " + docTableN.title);
							outFileBufWriter.write("\nreviewer: " + docTableN.reviewer);
							outFileBufWriter.write("\nrate: " + docTableN.rate);
							outFileBufWriter.write("\nsnippet: " + docTableN.snippet);
							outFileBufWriter.write("\n");
						}
						for (DocTable docTableNA : docTableListNA) {
							System.out.println("filename: " + docTableNA.fileName);
							System.out.println("title: " + docTableNA.title);
							System.out.println("reviewer: " + docTableNA.reviewer);
							System.out.println("rate: " + docTableNA.rate);
							System.out.println("snippet: " + docTableNA.snippet);

							outFileBufWriter.write("\nfilename: " + docTableNA.fileName);
							outFileBufWriter.write("\ntitle: " + docTableNA.title);
							outFileBufWriter.write("\nreviewer: " + docTableNA.reviewer);
							outFileBufWriter.write("\nrate: " + docTableNA.rate);
							outFileBufWriter.write("\nsnippet: " + docTableNA.snippet);
							outFileBufWriter.write("\n");
						}
						System.out.println("");
					} else {
						System.out.println("Query:" + fullQuery);
						System.out.println("NO RESULTs");

						outFileBufWriter.write("Query:" + fullQuery);
						outFileBufWriter.write("NO RESULTs");
					}
				} else {
					System.out.println("please provide correct query input");
				}

			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			if (postingFile != null)
				try {
					postingFile.close();
				} catch (Exception ex) {
				}
			if (postingCsv != null)
				try {
					postingCsv.close();
				} catch (Exception ex) {
				}
			if (dictFile != null)
				try {
					dictFile.close();
				} catch (Exception ex) {
				}
			if (dictCsv != null)
				try {
					dictCsv.close();
				} catch (Exception ex) {
				}
			if (docFile != null)
				try {
					docFile.close();
				} catch (Exception ex) {
				}
			if (docCsv != null)
				try {
					docCsv.close();
				} catch (Exception ex) {
				}
			if (outFileWriter != null)
				try {
					outFileWriter.close();
				} catch (Exception ex) {
				}
			if (outFileBufWriter != null)
				try {
					outFileBufWriter.close();
				} catch (Exception ex) {
				}

		}
	}

}
