/*******************************************************************************
 * 
 * Copyright (C) 2020 Brian_Entei (br45entei@gmail.com)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 *******************************************************************************/
package com.gmail.br45entei.io;

import com.gmail.br45entei.util.CodeUtil;
import com.gmail.br45entei.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.eclipse.swt.widgets.Display;

/** Ever wanted to search through a bunch of text files and change some text all
 * at the same time? Well, now you can! :)
 * 
 * @author Brian_Entei */
public final class FindReplaceSearch {
	
	/** A list containing common text file extensions (such as *.txt and *.rtf)
	 * which is used when {@link #onlyConsiderTextFiles} is set to
	 * <tt>true</tt> */
	public static final List<String> commonTextFileExtensions = new ArrayList<>(Arrays.asList(".txt",//
			".rtf",//
			".log",//
			".properties",//
			".classpath",//
			".project",//
			".java",//
			".html",//
			".css",//
			".csv",//
			".xml",//
			".php",//
			".c",//
			".h",//
			".cmd",//
			".bat",//
			".com"));
	
	//Public variable declarations
	/** The folder whose children will be searched through */
	public final File sourceFolder;
	/** The folder where output files will be written to (may be the same as the
	 * source folder) */
	public final File destinationFolder;
	/** If <tt>true</tt>, files that don't contain any of the specified
	 * search-strings will not be copied to the destination folder */
	public final boolean onlyCopyFilesContainingSearchStrings;
	/** If <tt>true</tt>, the source folder's sub-folders (and their sub-folders
	 * etc.)
	 * will be searched through as well */
	public final boolean recursive;
	/** If <tt>true</tt>, only files whose extensions match one of the
	 * {@link #commonTextFileExtensions} will be searched through. Other files
	 * will still be copied unless {@link #onlyCopyFilesContainingSearchStrings}
	 * is set to <tt>true</tt>. */
	public final boolean onlyConsiderTextFiles;
	/** An array containing the search-strings that will be searched for within
	 * the contents of files. Search-strings may be prepended with <tt>(?i)</tt>
	 * to indicate case-insensitive matching. */
	public final String[] findStrings;
	/** An array containing the strings that will be used to replace the matched
	 * search-strings found inside the contents of the files being searched.
	 * Replacement strings may use <tt>%s</tt> to represent the matched
	 * search-string found within the file's contents. */
	public final String[] replaceStrings;
	
	//Current status variables
	private volatile Thread thread = null;
	private volatile boolean running = false, paused = false;
	
	//Result values for last search & replace operation
	private volatile int filesCopied;
	private volatile int searchReplacementsPerformed;
	private volatile int filesSkipped;
	private volatile int foldersTraversed;
	private volatile int filesSearched;
	private volatile int searchesSkipped;
	private volatile int fileReadsFailed;
	private volatile int fileWritesFailed;
	private volatile int fileCopiesFailed;
	
	/** Creates a new {@link FindReplaceSearch} with the given settings.
	 * 
	 * @param sourceFolder The folder whose children will be searched through
	 * @param destinationFolder The folder where output files will be written to
	 *            (may be the same as the source folder)
	 * @param onlyCopyFilesContainingSearchStrings If <tt>true</tt>, files that
	 *            don't contain any of the specified search-strings will not be
	 *            copied to the destination folder
	 * @param recursive If <tt>true</tt>, the source folder's sub-folders (and
	 *            their sub-folders etc.) will be searched through as well
	 * @param onlyConsiderTextFiles If <tt>true</tt>, only files whose
	 *            extensions match one of the {@link #commonTextFileExtensions}
	 *            will be searched through. Other files will still be copied
	 *            unless <em><tt>onlyCopyFilesContainingSearchStrings</tt></em>
	 *            is set to <tt>true</tt>.
	 * @param findStrings An array containing the search-strings that will be
	 *            searched for within the contents of files. Search-strings may
	 *            be prepended with <tt>(?i)</tt> to indicate case-insensitive
	 *            matching.
	 * @param replaceStrings An array containing the strings that will be used
	 *            to replace the matched search-strings found inside the
	 *            contents of the files being searched. Replacement strings may
	 *            use <tt>%s</tt> to represent the matched search-string found
	 *            within the file's contents. */
	public FindReplaceSearch(File sourceFolder, File destinationFolder, boolean onlyCopyFilesContainingSearchStrings, boolean recursive, boolean onlyConsiderTextFiles, String[] findStrings, String[] replaceStrings) {
		this.sourceFolder = sourceFolder;
		this.destinationFolder = destinationFolder;
		this.onlyCopyFilesContainingSearchStrings = onlyCopyFilesContainingSearchStrings;
		this.recursive = recursive;
		this.onlyConsiderTextFiles = onlyConsiderTextFiles;
		this.findStrings = findStrings;
		this.replaceStrings = replaceStrings;
	}
	
	/** @return The results of the currently running (or the last run) search
	 *         operation. */
	public String getResults() {
		return new StringBuilder()//
				.append(String.format("Folders Traversed: %s\r\n", Integer.toString(this.foldersTraversed)))//
				.append(String.format("Files Searched: %s\r\n", Integer.toString(this.filesSearched)))//
				.append(String.format("Search Replacements Performed: %s\r\n", Integer.toString(this.searchReplacementsPerformed)))//
				.append(String.format("Searches Skipped: %s\r\n", Integer.toString(this.searchesSkipped)))//
				.append(String.format("Files Skipped: %s\r\n", Integer.toString(this.filesSkipped)))//
				.append(String.format("Files Copied: %s\r\n", Integer.toString(this.filesCopied)))//
				.append(String.format("File Copies Failed: %s\r\n", Integer.toString(this.fileCopiesFailed)))//
				.append(String.format("File Reads Failed: %s\r\n", Integer.toString(this.fileReadsFailed)))//
				.append(String.format("File Writes Failed: %s\r\n", Integer.toString(this.fileWritesFailed)))//
				.toString();
	}
	
	protected final boolean copy(File src, File dest, PrintStream pr) {
		if(src.equals(dest)) {
			this.filesSkipped++;
			pr.println(String.format("Skipping copy of file \"%s\" as it is the same as the destination: ", src.getAbsolutePath()));
			return true;
		}
		try(FileInputStream in = new FileInputStream(src)) {
			try(FileOutputStream out = new FileOutputStream(dest)) {
				byte[] b = new byte[4096];
				int len;
				while((len = in.read(b)) != -1) {
					out.write(b, 0, len);
					
					this.pauseSleep();
					
				}
				out.flush();
				this.filesCopied++;
				return true;
			} catch(IOException ex) {
				this.fileWritesFailed++;
				pr.print(String.format("Failed to write to destination file \"%s\": ", dest.getAbsolutePath()));
				System.err.print(String.format("Failed to write to destination file \"%s\": ", dest.getAbsolutePath()));
				ex.printStackTrace(pr);
				ex.printStackTrace(System.err);
				pr.flush();
				System.err.flush();
			}
		} catch(IOException ex) {
			this.fileReadsFailed++;
			pr.print(String.format("Failed to read from source file \"%s\": ", src.getAbsolutePath()));
			System.err.print(String.format("Failed to read from source file \"%s\": ", src.getAbsolutePath()));
			ex.printStackTrace(pr);
			ex.printStackTrace(System.err);
			pr.flush();
			System.err.flush();
		}
		this.fileCopiesFailed++;
		return false;
	}
	
	/** Finds any search-strings in the source file and replaces them in-memory,
	 * and then writes the output to the destination file.<br>
	 * If there are no search-strings or no matches are found, the contents of
	 * the source file are simply copied to the destination file instead.
	 * 
	 * @param src The file to read from
	 * @param dest The file to write to
	 * @param pr The {@link PrintStream} to print status messages to
	 * @return True if any data was written to the destination file */
	public boolean findAndReplace(File src, File dest, PrintStream pr) {
		if(this.findStrings.length == 0) {
			pr.println(String.format("Byte-copying file and skipping search within \"%s\" due to lack of search strings...", src.getAbsolutePath()));
			this.searchesSkipped++;
			return this.copy(src, dest, pr);
		}
		pr.println(String.format("Searching within file \"%s\"...", src.getAbsolutePath()));
		
		List<String> lines = new ArrayList<>();
		try(FileInputStream in = new FileInputStream(src)) {
			String line;
			while((line = FileUtil.readLine(in)) != null) {
				lines.add(line);
				
				if(!this.pauseSleep()) {
					return false;
				}
				
			}
			this.filesSearched++;
		} catch(IOException ex) {
			this.fileReadsFailed++;
			pr.print(String.format("Failed to read source file \"%s\": ", src.getAbsolutePath()));
			System.err.print(String.format("Failed to read source file \"%s\": ", src.getAbsolutePath()));
			ex.printStackTrace(pr);
			ex.printStackTrace(System.err);
			pr.flush();
			System.err.flush();
			return false;
		}
		
		List<String> replacedLines = new ArrayList<>();
		boolean foundAnyMatches = false;
		int lineNum = 1, numLines = lines.size();
		for(String line : lines) {
			for(int i = 0; i < this.findStrings.length; i++) {
				String searchString = this.findStrings[i];
				boolean ignoreCase = searchString.startsWith("(?i)");
				searchString = ignoreCase ? searchString.substring(4) : searchString;
				String replacementString = i < this.replaceStrings.length ? this.replaceStrings[i] : "%s";
				
				List<Integer> indicies = new ArrayList<>();
				char[] array = line.toCharArray();
				for(int j = 0; j < array.length; j++) {
					String check = line.length() >= j + searchString.length() ? line.substring(j, j + searchString.length()) : null;
					if(check == null) {
						break;
					}
					if(ignoreCase ? check.equalsIgnoreCase(searchString) : check.equals(searchString)) {
						indicies.add(Integer.valueOf(j));
						foundAnyMatches = true;
					}
					
					if(!this.pauseSleep()) {
						return false;
					}
				}
				StringBuilder sb = new StringBuilder();
				int lastIndex = 0;
				String firstFoundTarget = null;
				for(Integer index : indicies) {
					int j = index.intValue();
					for(int k = lastIndex; k < j; k++) {
						sb.append(array[k]);
					}
					String target = new String(array, j, searchString.length());
					String replacement = replacementString.replace("%s", target);
					sb.append(replacement);
					lastIndex = j + target.length();
					if(firstFoundTarget == null) {
						firstFoundTarget = target;
					}
					
					if(!this.pauseSleep()) {
						return false;
					}
				}
				
				if(firstFoundTarget != null) {
					if(lastIndex < array.length) {
						for(int k = lastIndex; k < array.length; k++) {
							sb.append(array[k]);
						}
					}
					
					String before = line;
					line = sb.toString();
					String after = line;
					
					pr.println(String.format("\tFound \"%s\" in line # %s/%s;\n\t\tLine before: \"%s\";\n\t\tResulting line: \"%s\";", firstFoundTarget, Integer.toString(lineNum), Integer.toString(numLines), before, after));
				}
				
				//TODO re-do this while loop so that it scans along the line, replacing each instance separately, instead of all of them in one go or none of them ...
				
				/*int index, lastIndex = -1;
				while((index = ignoreCase ? line.toLowerCase().indexOf(searchString.toLowerCase()) : line.indexOf(searchString)) != -1 && index > lastIndex) {
					lastIndex = index;
					foundAnyMatches = true;
					this.searchReplacementsPerformed++;
					
					String target = line.substring(index, index + searchString.length());
					String replacement = replacementString.replace("%s", target);
					
					String before = line;
					line = line.replace(target, replacement);
					String after = line;
					
					pr.println(String.format("\tFound \"%s\" in line # %s/%s;\n\t\tLine before: \"%s\";\n\t\tResulting line: \"%s\";", target, Integer.toString(lineNum), Integer.toString(numLines), before, after));
					
					this.pauseSleep();
					
				}*/
				
				if(!this.pauseSleep()) {
					return false;
				}
			}
			
			replacedLines.add(line);
			
			if(!this.pauseSleep()) {
				return false;
			}
			
			lineNum++;
		}
		
		if(this.onlyCopyFilesContainingSearchStrings && !foundAnyMatches) {
			pr.println(String.format("\tSkipping copy of file \"%s\" as it does not contain any of the search-strings...", src.getAbsolutePath()));
			this.filesSkipped++;
			return false;
		}
		if(!foundAnyMatches && !src.equals(dest)) {
			pr.println(String.format("\tPerforming byte-copy instead of line-by-line copy of file \"%s\" as it does not contain any of the search-strings.", src.getAbsolutePath()));
			return this.copy(src, dest, pr);
		}
		if(!foundAnyMatches && src.equals(dest)) {
			pr.println(String.format("\tSkipping copy of file \"%s\" as it does not contain any of the search-strings, and is the same file as the destination.", src.getAbsolutePath()));
			this.filesSkipped++;
			return false;
		}
		
		//Reading the source file and writing to the destination file has been separated out due to the possibility of the user selecting the same source directory as the destination,
		//thereby causing the srcFile and destFile to be the same file! I have intentionally allowed this possibility and worked around it to allow the user to just do an in-place replacement without having to actually copy the files.
		//Without the workaround, opening and writing to the same file at the same time results in a blank file when the program is done with it! (at least on my Windows 10 computer ...)
		
		pr.println(String.format("\tCopying file \"%s\" to destination file \"%s\" line-by-line...", src.getAbsolutePath(), dest.getAbsolutePath()));
		try(PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dest), StandardCharsets.ISO_8859_1), true)) {
			for(String line : replacedLines) {
				out.println(line);
			}
			out.flush();
			this.filesCopied++;
			return true;
		} catch(IOException ex) {
			this.fileWritesFailed++;
			pr.print(String.format("Failed to write to destination file \"%s\": ", dest.getAbsolutePath()));
			System.err.print(String.format("Failed to write to destination file \"%s\": ", dest.getAbsolutePath()));
			ex.printStackTrace(pr);
			ex.printStackTrace(System.err);
			pr.flush();
			System.err.flush();
			return false;
		}
	}
	
	/** @return True if a search operation is in progress and is currently
	 *         paused */
	public boolean isSearchPaused() {
		return this.isASearchActive() && this.paused;
	}
	
	/** Causes the current thread to sleep while the current search operation is
	 * paused.
	 * 
	 * @return Whether or not the operation should continue running */
	protected boolean pauseSleep() {
		while(this.paused) {
			CodeUtil.sleep(10L);
		}
		return this.running;
	}
	
	/** Pauses the current search operation.
	 * 
	 * @return This FindReplaceSearch */
	public FindReplaceSearch pauseSearch() {
		this.paused = true;
		return this;
	}
	
	/** Resumes the current search operation.
	 * 
	 * @return This FindReplaceSearch */
	public FindReplaceSearch resumeSearch() {
		this.paused = false;
		return this;
	}
	
	/** Tells the current search operation that it needs to stop, and then waits
	 * for it to do so.<br>
	 * If the current thread is a SWT thread, {@link Display#readAndDispatch()}
	 * is called for the duration of the wait.
	 * 
	 * @return This FindReplaceSearch */
	public FindReplaceSearch stopSearch() {
		Display display;
		while(this.thread != null && this.thread.isAlive()) {
			this.running = false;
			this.paused = false;
			
			display = Display.getCurrent();
			if(display != null && !display.isDisposed()) {
				if(!display.readAndDispatch()) {
					CodeUtil.sleep(10L);
				}
			} else {
				CodeUtil.sleep(10L);
			}
		}
		this.thread = null;
		return this;
	}
	
	/** Begins a new search operation and returns the thread performing the
	 * search.<br>
	 * <b>Note:</b>&nbsp;The returned thread is marked as a daemon thread.
	 * 
	 * @param pr The {@link PrintStream} to print status messages to
	 * @return The thread performing the new search operation */
	public Thread startSearch(final PrintStream pr) {
		if(this.thread != null && this.thread.isAlive()) {
			return this.thread;
		}
		if(this.findStrings.length == 0 && this.onlyCopyFilesContainingSearchStrings) {
			pr.println("Skipping entire operation due to incompatible settings \"onlyCopyFilesContainingSearchStrings\" and <blank search strings>...");
			pr.flush();
			return null;
		}
		this.running = true;
		this.paused = false;
		this.filesCopied = this.searchReplacementsPerformed = this.filesSkipped = this.foldersTraversed = //
				this.filesSearched = this.fileReadsFailed = this.fileWritesFailed = this.fileCopiesFailed = 0;
		
		this.thread = new Thread(() -> {
			String srcPath = this.sourceFolder.getAbsolutePath();
			srcPath = srcPath.endsWith(File.separator) ? srcPath.substring(0, srcPath.length() - 1) : srcPath;
			String destPath = this.destinationFolder.getAbsolutePath();
			destPath = destPath.endsWith(File.separator) ? destPath.substring(0, destPath.length() - 1) : destPath;
			final ConcurrentLinkedDeque<File> files;
			{
				File[] children = this.sourceFolder.listFiles();
				files = children == null ? new ConcurrentLinkedDeque<>() : new ConcurrentLinkedDeque<>(Arrays.asList(children));
				if(children != null) {
					this.foldersTraversed++;
				}
			}
			int i = -1;
			File file;
			while(this.running && (file = files.poll()) != null) {
				i++;
				if(file.isDirectory()) {
					if(!this.recursive) {
						continue;
					}
					
					File[] children = file.listFiles();
					if(children != null) {
						this.foldersTraversed++;
						files.addAll(Arrays.asList(children));
					}
					continue;
				}
				if(!this.pauseSleep()) {
					break;
				}
				
				String path = file.getAbsolutePath();
				path = path.startsWith(srcPath) ? path.substring(srcPath.length()) : path;
				
				if(this.onlyConsiderTextFiles) {
					String name = file.getName();
					boolean isValidExtension = false;
					if(name.contains(".")) {
						for(String validExtension : commonTextFileExtensions) {
							isValidExtension |= name.toLowerCase().endsWith(validExtension.toLowerCase());
							if(isValidExtension) {
								break;
							}
						}
						if(!isValidExtension) {
							if(!this.onlyCopyFilesContainingSearchStrings) {
								pr.println(String.format("Performing byte-copy of non-text file \"%s\"...", path));
								File dest = new File(destPath.concat(path.startsWith(File.separator) ? path : File.separator.concat(path)));
								File parent = dest.getParentFile();
								if(parent != null) {
									parent.mkdirs();
								}
								this.copy(file, dest, pr);
							} else {
								pr.println(String.format("Skipping search within and copy of non-text file \"%s\"...", path));
								this.filesSkipped++;
								this.searchesSkipped++;
							}
							continue;
						}
					}
				}
				
				path = destPath.concat(path.startsWith(File.separator) ? path : File.separator.concat(path));
				
				File dest = new File(path);
				File parent = dest.getParentFile();
				if(parent != null) {
					parent.mkdirs();
				}
				this.findAndReplace(file, dest, pr);
				pr.flush();
			}
			pr.flush();
		}, "Find/ReplaceSearchThread");
		this.thread.setDaemon(true);
		this.thread.start();
		Display display;
		while(this.thread.getState() == Thread.State.NEW) {
			display = Display.getCurrent();
			if(display != null && !display.isDisposed()) {
				if(!display.readAndDispatch()) {
					CodeUtil.sleep(10L);
				}
			} else {
				CodeUtil.sleep(10L);
			}
		}
		return this.thread;
	}
	
	/** @return The thread performing the current search operation (if one is in
	 *         progress) */
	public Thread getSearchThread() {
		return this.thread;
	}
	
	/** @return True if a search operation is in progress */
	public boolean isASearchActive() {
		Thread thread = this.thread;
		return thread != null && thread.isAlive();
	}
	
}
