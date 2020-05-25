package com.gmail.br45entei.main;

import com.gmail.br45entei.util.SWTUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.wb.swt.SWTResourceManager;

/** @author Brian_Entei */
public final class Main {
	
	/** Causes the currently executing thread to sleep (temporarily cease
	 * execution) for the specified number of milliseconds, subject to
	 * the precision and accuracy of system timers and schedulers. The thread
	 * does not lose ownership of any monitors.
	 * 
	 * @param millis The length of time to sleep in milliseconds
	 * @return An InterruptedException if the thread was interrupted while
	 *         sleeping
	 * @throws IllegalArgumentException Thrown if the value of <tt>millis</tt>
	 *             is negative */
	public static final InterruptedException sleep(long millis) throws IllegalArgumentException {
		try {
			Thread.sleep(millis);
			return null;
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
			return ex;
		}
	}
	
	/** Wraps the given {@link OutputStream} with a new {@link PrintStream} that
	 * uses the given line separator.
	 * 
	 * @param out The output stream to wrap
	 * @param lineSeparator The line separator that the returned PrintStream
	 *            will use
	 * @return The resulting PrintStream */
	public static final PrintStream wrapOutputStream(final OutputStream out, final String lineSeparator) {
		final String originalLineSeparator = AccessController.doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));
		try {
			AccessController.doPrivileged(new PrivilegedAction<Void>() {
				@Override
				public Void run() {
					System.setProperty("line.separator", lineSeparator);
					return null;
				}
			});
			return new PrintStream(out, true);
		} finally {
			System.setProperty("line.separator", originalLineSeparator);
		}
	}
	
	/** Reads and returns a single line of text from the given input stream,
	 * using the given charset to convert the read data into a string.
	 * 
	 * @param in The {@link InputStream} to read the text from
	 * @param trim Whether or not the end of the line should have any existing
	 *            (single) carriage return character removed
	 * @param charset The {@link Charset} to use when converting the read data
	 *            into a {@link String}
	 * @return The read line, or <tt><b>null</b></tt> if the end of the stream
	 *         was reached
	 * @throws IOException Thrown if a read error occurs */
	public static final String readLine(InputStream in, boolean trim, Charset charset) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int b;
		while((b = in.read()) != -1) {
			if(b == 10) {//LF character '\n' (line feed)
				break;
			}
			baos.write(b);
		}
		if(b == -1 && baos.size() == 0) {
			return null;
		}
		byte[] data = baos.toByteArray();
		String line = new String(data, 0, data.length, charset);
		return trim && line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
	}
	
	/** Reads and returns a single line of text from the given input stream,
	 * using the {@link StandardCharsets#ISO_8859_1 ISO_8859_1} standard charset
	 * to convert the read data into a string.
	 * 
	 * @param in The {@link InputStream} to read the text from
	 * @param trim Whether or not the end of the line should have any existing
	 *            (single) carriage return character removed
	 * @return The read line, or <tt><b>null</b></tt> if the end of the stream
	 *         was reached
	 * @throws IOException Thrown if a read error occurs */
	public static final String readLine(InputStream in, boolean trim) throws IOException {
		return readLine(in, trim, StandardCharsets.ISO_8859_1);
	}
	
	/** Reads and returns a single line of text from the given input stream,
	 * using the {@link StandardCharsets#ISO_8859_1 ISO_8859_1} standard charset
	 * to convert the read data into a string.
	 * 
	 * @param in The {@link InputStream} to read the text from
	 * @return The read line, or <tt><b>null</b></tt> if the end of the stream
	 *         was reached
	 * @throws IOException Thrown if a read error occurs */
	public static final String readLine(InputStream in) throws IOException {
		return readLine(in, true);
	}
	
	protected Display display;
	protected Shell shell;
	protected StyledText stxtFind;
	protected StyledText stxtReplaceWith;
	
	protected Label lblSeparator;
	
	protected Button btnOnlyCopyFiles;
	protected Button btnRecursiveFileSearch;
	protected Button btnOnlyConsidertxt;
	
	protected Text txtSourceFolderPath;
	private Button btnBrowseSourcePath;
	
	protected Text txtDestinationFolderPath;
	private Button btnBrowseDestinationPath;
	
	protected Label lblSeparator_1;
	
	protected Button btnStartFindReplaceSearch;
	protected Button btnPauseSearch;
	protected Button btnStopSearch;
	
	protected Label lblSeparator_2;
	
	protected StyledText stxtOutput;
	
	protected volatile FindReplaceSearch activeSearch = null;
	
	/** Creates a new dialog. */
	public Main() {
		this.createContents();
	}
	
	/** Opens this dialog.
	 * 
	 * @param runLoop If <tt>true</tt>, this method will block and call
	 *            {@link Display#readAndDispatch() readAndDispatch} until the
	 *            dialog is hidden, closed, or disposed.
	 * @return This dialog */
	public Main open(boolean runLoop) {
		this.shell.open();
		this.shell.layout();
		
		if(runLoop) {
			while(this.runLoop() && this.shell.isVisible()) {
			}
		}
		return this;
	}
	
	/** Opens this dialog.
	 * 
	 * @return This dialog */
	public Main open() {
		return this.open(true);
	}
	
	/** @return True if the program should continue running */
	public boolean runLoop() {
		if(!this.shell.isDisposed()) {
			this.updateUI();
		}
		if(!this.display.isDisposed() && !this.display.readAndDispatch()) {
			//display.sleep();
			try {
				Thread.sleep(10L);
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
		return !this.shell.isDisposed();
	}
	
	/** @param args Program command line arguments */
	public static void main(String[] args) {
		Main main = new Main();
		main.open().dispose().display.dispose();
	}
	
	private void createContents() {
		if(this.display != null) {
			return;
		}
		this.display = Display.getDefault();
		this.shell = new Shell(this.display, SWT.CLOSE | SWT.TITLE | SWT.MIN | SWT.MAX | SWT.RESIZE);
		this.shell.setSize(800, 600);
		this.shell.setMinimumSize(this.shell.getSize());
		this.shell.setText("Batch Find/Replace Text Changer & File Copier");
		this.shell.setImages(SWTUtil.getTitleImages());
		this.shell.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				Main.this.updateUI();
			}
		});
		
		Label lblFind = new Label(this.shell, SWT.NONE);
		lblFind.setBounds(10, 10, 40, 15);
		lblFind.setText("Find:");
		
		this.stxtFind = new StyledText(this.shell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		this.stxtFind.setToolTipText("Enter your search-string(s) here; each one must be placed on its own line.\r\nStart the line off with (?i) to make the search case-insensitive for that search-string.");
		this.stxtFind.setBounds(10, 31, this.shell.getSize().x - 36, 95);
		
		Label lblReplaceWith = new Label(this.shell, SWT.NONE);
		lblReplaceWith.setBounds(10, 132, 80, 15);
		lblReplaceWith.setText("Replace with:");
		
		this.stxtReplaceWith = new StyledText(this.shell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		this.stxtReplaceWith.setToolTipText("Enter your replacement strings here. (They must be in the same order as the search strings to make a pair)\r\nUse %s to have the matched string from the file be a part of the replacement string.");
		this.stxtReplaceWith.setBounds(10, 153, this.shell.getSize().x - 36, 95);
		
		this.lblSeparator = new Label(this.shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		this.lblSeparator.setBounds(10, 254, this.shell.getSize().x - 36, 2);
		
		this.btnOnlyCopyFiles = new Button(this.shell, SWT.CHECK);
		this.btnOnlyCopyFiles.setToolTipText("Check this box to only copy files containing one or more of the search-strings you specified in the Find: text field above.\r\nLeave this box unchecked to copy all files with the specified folder below.");
		this.btnOnlyCopyFiles.setBounds(10, 262, 314, 16);
		this.btnOnlyCopyFiles.setText("Only copy files containing one or more search-strings");
		
		this.btnOnlyConsidertxt = new Button(this.shell, SWT.CHECK);
		this.btnOnlyConsidertxt.setToolTipText("If checked, only files ending with the following file extensions will be searched:\r\n*.txt\r\n*.rtf\r\n*.log\r\n*.properties\r\n*.classpath\r\n*.project\r\n*.java\r\n*.html\r\n*.css\r\n*.csv\r\n*.xml\r\n*.php\r\n*.c\r\n*.h\r\n*.cmd\r\n*.bat\r\n*.com");
		this.btnOnlyConsidertxt.setBounds(473, 262, 220, 16);
		this.btnOnlyConsidertxt.setText("Only consider common text file types");
		
		Label lblSourceFolder = new Label(this.shell, SWT.NONE);
		lblSourceFolder.setBounds(10, 287, 109, 15);
		lblSourceFolder.setText("Source Folder:");
		
		this.txtSourceFolderPath = new Text(this.shell, SWT.BORDER | SWT.READ_ONLY);
		this.txtSourceFolderPath.setToolTipText("The folder in which the Find/Replace Search will take place");
		this.txtSourceFolderPath.setBounds(125, 284, this.shell.getSize().x - 232, 21);
		
		this.btnBrowseSourcePath = new Button(this.shell, SWT.NONE);
		this.btnBrowseSourcePath.setToolTipText("Browse for and select the folder in which the Find/Replace Search will take place");
		this.btnBrowseSourcePath.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(Main.this.shell, SWT.NONE);
				dialog.setText("Choose Source Folder");
				dialog.setMessage("Please select the folder where the Find/Replace search will take place.");
				String checkPath = Main.this.txtSourceFolderPath.getText();
				if(!checkPath.isEmpty() && new File(checkPath).isDirectory()) {
					dialog.setFilterPath(checkPath);
				}
				
				String path = dialog.open();
				Main.this.txtSourceFolderPath.setText(path == null ? dialog.getFilterPath() : path);
			}
		});
		this.btnBrowseSourcePath.setBounds(this.shell.getSize().x - 101, 282, 75, 25);
		this.btnBrowseSourcePath.setText("Browse...");
		
		Label lblDestinationFolder = new Label(this.shell, SWT.NONE);
		lblDestinationFolder.setText("Destination Folder:");
		lblDestinationFolder.setBounds(10, 316, 109, 15);
		
		this.txtDestinationFolderPath = new Text(this.shell, SWT.BORDER);
		this.txtDestinationFolderPath.setToolTipText("The folder in which the Find/Replace Search will output files to");
		this.txtDestinationFolderPath.setBounds(125, 313, this.shell.getSize().x - 232, 21);
		
		this.btnBrowseDestinationPath = new Button(this.shell, SWT.NONE);
		this.btnBrowseDestinationPath.setToolTipText("Browse for and select the folder in which the Find/Replace Search will output files to");
		this.btnBrowseDestinationPath.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(Main.this.shell, SWT.NONE);
				dialog.setText("Choose Destination Folder");
				dialog.setMessage("Please select the folder where the Find/Replace search will output files to.");
				String checkPath = Main.this.txtDestinationFolderPath.getText();
				if(!checkPath.isEmpty() && new File(checkPath).isDirectory()) {
					dialog.setFilterPath(checkPath);
				}
				
				String path = dialog.open();
				Main.this.txtDestinationFolderPath.setText(path == null ? dialog.getFilterPath() : path);
			}
		});
		this.btnBrowseDestinationPath.setText("Browse...");
		this.btnBrowseDestinationPath.setBounds(this.shell.getSize().x - 101, 311, 75, 25);
		
		Label lblEachSearchString = new Label(this.shell, SWT.NONE);
		lblEachSearchString.setBounds(125, 10, 649, 15);
		lblEachSearchString.setText("Each search-string is separated via a new line, and you can specify a case-insensitive search by starting the line with (?i).");
		
		this.lblSeparator_1 = new Label(this.shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		this.lblSeparator_1.setBounds(10, 340, this.shell.getSize().x - 36, 2);
		
		this.btnRecursiveFileSearch = new Button(this.shell, SWT.CHECK);
		this.btnRecursiveFileSearch.setToolTipText("Check this box to search for and copy files within the subfolders (and their children etc.) of the specified folder below.");
		this.btnRecursiveFileSearch.setBounds(330, 262, 137, 16);
		this.btnRecursiveFileSearch.setText("Recursive file search");
		
		this.btnStartFindReplaceSearch = new Button(this.shell, SWT.PUSH);
		this.btnStartFindReplaceSearch.setToolTipText("Click to start the Find/Replace Search");
		this.btnStartFindReplaceSearch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				File sourceFolder = new File(Main.this.txtSourceFolderPath.getText());
				File destinationFolder = new File(Main.this.txtDestinationFolderPath.getText());
				boolean onlyCopyFilesContainingSearchStrings = Main.this.btnOnlyCopyFiles.getSelection();
				boolean recursive = Main.this.btnRecursiveFileSearch.getSelection();
				boolean onlyConsiderTextFiles = Main.this.btnOnlyConsidertxt.getSelection();
				String[] findStrings, replaceStrings;
				{
					List<String> fs = new ArrayList<>(Arrays.asList(Main.this.stxtFind.getText().split(Pattern.quote("\n"))));
					List<String> rs = new ArrayList<>(Arrays.asList(Main.this.stxtReplaceWith.getText().split(Pattern.quote("\n"))));
					
					List<String> cfs = new ArrayList<>(),//	cleaned Find-Strings
							crs = new ArrayList<>();//		cleaned Replacement-Strings
					for(int i = 0; i < fs.size(); i++) {
						final String searchString = fs.get(i);
						if(searchString.isEmpty() || (searchString.toLowerCase().startsWith("(?i)") && searchString.substring(4).isEmpty())) {
							continue;
						}
						cfs.add(searchString);
						if(i < rs.size()) {
							crs.add(rs.get(i));
						} else {
							crs.add("%s");
						}
					}
					findStrings = cfs.toArray(new String[cfs.size()]);
					replaceStrings = crs.toArray(new String[crs.size()]);
				}
				
				if(!sourceFolder.isDirectory()) {
					MessageBox box = new MessageBox(Main.this.shell, SWT.ICON_ERROR | SWT.OK);
					box.setText("Error Opening Source Folder");
					box.setMessage("Unable to open the specified source folder.\nPlease check that it exists and is accessible, and then try again.");
					
					box.open();
					return;
				}
				destinationFolder.mkdirs();
				if(!destinationFolder.isDirectory()) {
					MessageBox box = new MessageBox(Main.this.shell, SWT.ICON_ERROR | SWT.OK);
					box.setText("Error Opening Destination Folder");
					box.setMessage("Unable to open the specified destination folder.\nPlease check that it exists( or can be created) and is accessible, and then try again.");
					
					box.open();
					return;
				}
				
				Main.this.startFindReplaceSearch(new FindReplaceSearch(sourceFolder, destinationFolder, onlyCopyFilesContainingSearchStrings, recursive, onlyConsiderTextFiles, findStrings, replaceStrings));
			}
		});
		this.btnStartFindReplaceSearch.setBounds(10, 348, 156, 25);
		this.btnStartFindReplaceSearch.setText("Start Find/Replace Search");
		
		this.btnPauseSearch = new Button(this.shell, SWT.TOGGLE);
		this.btnPauseSearch.setToolTipText("Click to pause the Find/Replace Search");
		this.btnPauseSearch.setEnabled(false);
		this.btnPauseSearch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FindReplaceSearch search = Main.this.activeSearch;
				if(search != null) {
					if(Main.this.btnPauseSearch.getSelection()) {
						search.pauseSearch();
					} else {
						search.resumeSearch();
					}
				} else {
					Main.this.btnPauseSearch.setSelection(false);
				}
				
				Main.this.btnPauseSearch.setText(Main.this.btnPauseSearch.getSelection() ? "Resume Search" : "Pause Search");
			}
		});
		this.btnPauseSearch.setBounds(172, 348, 96, 25);
		this.btnPauseSearch.setText("Pause Search");
		
		Label lblEachReplacementString = new Label(this.shell, SWT.NONE);
		lblEachReplacementString.setBounds(125, 132, 649, 15);
		lblEachReplacementString.setText("Each replacement is paired with the corresponding line above, and %s is replaced with the string found in the file.");
		
		this.lblSeparator_2 = new Label(this.shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		this.lblSeparator_2.setBounds(10, 379, this.shell.getSize().x - 36, 2);
		
		this.stxtOutput = new StyledText(this.shell, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		this.stxtOutput.setToolTipText("The output of the current Find/Replace Search");
		this.stxtOutput.setSelectionForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		this.stxtOutput.setSelectionBackground(SWTResourceManager.getColor(SWT.COLOR_GREEN));
		this.stxtOutput.setForeground(SWTResourceManager.getColor(SWT.COLOR_GREEN));
		this.stxtOutput.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		this.stxtOutput.setBounds(10, 387, this.shell.getSize().x - 36, this.shell.getSize().y - 436);
		
		this.btnStopSearch = new Button(this.shell, SWT.NONE);
		this.btnStopSearch.setToolTipText("Click to stop the Find/Replace Search");
		this.btnStopSearch.setEnabled(false);
		this.btnStopSearch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FindReplaceSearch search = Main.this.activeSearch;
				if(search != null) {
					Main.this.btnStopSearch.setEnabled(false);
					Main.this.btnPauseSearch.setEnabled(false);
					search.stopSearch();
				}
			}
		});
		this.btnStopSearch.setBounds(274, 348, 80, 25);
		this.btnStopSearch.setText("Stop Search");
	}
	
	protected void updateUI() {
		Point shellSize = this.shell.getSize();
		Point size = new Point(shellSize.x - 36, 95);
		SWTUtil.setSize(this.stxtFind, size);
		SWTUtil.setSize(this.stxtReplaceWith, size);
		
		size.y = 2;
		SWTUtil.setSize(this.lblSeparator, size);
		SWTUtil.setSize(this.lblSeparator_1, size);
		SWTUtil.setSize(this.lblSeparator_2, size);
		
		Point location = new Point(shellSize.x - 101, 282);
		SWTUtil.setLocation(this.btnBrowseSourcePath, location);
		location.y = 311;
		SWTUtil.setLocation(this.btnBrowseDestinationPath, location);
		
		size = new Point(shellSize.x - 232, 21);
		SWTUtil.setSize(this.txtSourceFolderPath, size);
		SWTUtil.setSize(this.txtDestinationFolderPath, size);
		
		size = new Point(shellSize.x - 36, shellSize.y - 436);
		SWTUtil.setSize(this.stxtOutput, size);
	}
	
	public void startFindReplaceSearch(final FindReplaceSearch search) {
		this.stxtFind.setEnabled(false);
		this.stxtReplaceWith.setEnabled(false);
		this.btnOnlyCopyFiles.setEnabled(false);
		this.btnRecursiveFileSearch.setEnabled(false);
		this.btnOnlyConsidertxt.setEnabled(false);
		this.btnBrowseSourcePath.setEnabled(false);
		this.btnBrowseDestinationPath.setEnabled(false);
		this.txtDestinationFolderPath.setEnabled(false);
		this.btnStartFindReplaceSearch.setEnabled(false);
		this.btnPauseSearch.setEnabled(true);
		this.btnPauseSearch.setSelection(search.isSearchPaused());
		this.btnStopSearch.setEnabled(true);
		
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try(PrintStream pr = wrapOutputStream(baos, "\n")) {
			final Thread searchThread = search.startSearch(pr);
			if(searchThread != null) {
				this.activeSearch = search;
				
				long lastTextUpdate = 0L;
				while(searchThread.isAlive() && this.runLoop()) {
					long now = System.currentTimeMillis();
					if(now - lastTextUpdate >= 160L) {
						lastTextUpdate = now;
						
						byte[] data = baos.toByteArray();
						String text = new String(data, 0, data.length, StandardCharsets.ISO_8859_1);
						text = text.length() > 20000 ? text.substring(text.length() - 20000) : text;//Keep text length at or under 20000
						
						if(!this.stxtOutput.getText().equals(text)) {
							this.stxtOutput.setText(text);
							
							this.display.readAndDispatch();
							if(this.shell.isDisposed()) {
								break;
							}
							
							this.stxtOutput.setSelection(text.length());
						}
					}
					
				}
				
				pr.println("Find/Replace Search complete.");
				pr.flush();
			}
		} finally {
			if(this.shell.isDisposed()) {
				return;
			}
			this.stxtFind.setEnabled(true);
			this.stxtReplaceWith.setEnabled(true);
			this.btnOnlyCopyFiles.setEnabled(true);
			this.btnRecursiveFileSearch.setEnabled(true);
			this.btnOnlyConsidertxt.setEnabled(true);
			this.btnBrowseSourcePath.setEnabled(true);
			this.btnBrowseDestinationPath.setEnabled(true);
			this.txtDestinationFolderPath.setEnabled(true);
			this.btnStartFindReplaceSearch.setEnabled(true);
			this.btnPauseSearch.setEnabled(false);
			this.btnPauseSearch.setSelection(false);
			this.btnStopSearch.setEnabled(false);
			this.activeSearch = null;
			
			byte[] data = baos.toByteArray();
			String text = new String(data, 0, data.length, StandardCharsets.ISO_8859_1);
			text = text.concat("\r\n").concat(search.getResults());
			text = text.length() > 20000 ? text.substring(text.length() - 20000) : text;//Keep text length at or under 20000
			if(!this.stxtOutput.getText().equals(text)) {
				this.stxtOutput.setText(text);
				this.stxtOutput.setSelection(text.length());
			}
		}
	}
	
	/** Disposes of the operating system resources associated with
	 * the receiver and all its descendants. After this method has
	 * been invoked, the receiver and all descendants will answer
	 * <code>true</code> when sent the message <code>isDisposed()</code>.
	 * Any internal connections between the widgets in the tree will
	 * have been removed to facilitate garbage collection.
	 * This method does nothing if the widget is already disposed.
	 * <p>
	 * NOTE: This method is not called recursively on the descendants
	 * of the receiver. This means that, widget implementers can not
	 * detect when a widget is being disposed of by re-implementing
	 * this method, but should instead listen for the <code>Dispose</code>
	 * event.
	 * </p>
	 * 
	 * @return This dialog
	 *
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 *
	 * @see Widget#addDisposeListener
	 * @see Widget#removeDisposeListener
	 * @see Widget#checkWidget */
	public Main dispose() {
		this.shell.dispose();
		return this;
	}
	
	/** Returns <code>true</code> if the widget has been disposed,
	 * and <code>false</code> otherwise.
	 * <p>
	 * This method gets the dispose state for the widget.
	 * When a widget has been disposed, it is an error to
	 * invoke any other method (except {@link #dispose()}) using the widget.
	 * </p>
	 *
	 * @return <code>true</code> when the widget is disposed and
	 *         <code>false</code> otherwise */
	public boolean isDisposed() {
		return this.shell == null ? false : this.shell.isDisposed();
	}
	
	private static final class FindReplaceSearch {
		
		public final File sourceFolder, destinationFolder;
		public final boolean onlyCopyFilesContainingSearchStrings, recursive,
				onlyConsiderTextFiles;
		public final String[] findStrings, replaceStrings;
		
		public final List<String> validFileExtensions = new ArrayList<>(Arrays.asList(".txt",//
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
		
		private volatile Thread thread = null;
		private volatile boolean running = false, paused = false;
		
		private volatile int filesCopied, searchReplacementsPerformed,
				filesSkipped, foldersTraversed, filesSearched, searchesSkipped,
				fileReadsFailed, fileWritesFailed, fileCopiesFailed;
		
		public FindReplaceSearch(File sourceFolder, File destinationFolder, boolean onlyCopyFilesContainingSearchStrings, boolean recursive, boolean onlyConsiderTextFiles, String[] findStrings, String[] replaceStrings) {
			this.sourceFolder = sourceFolder;
			this.destinationFolder = destinationFolder;
			this.onlyCopyFilesContainingSearchStrings = onlyCopyFilesContainingSearchStrings;
			this.recursive = recursive;
			this.onlyConsiderTextFiles = onlyConsiderTextFiles;
			this.findStrings = findStrings;
			this.replaceStrings = replaceStrings;
		}
		
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
						
						while(this.paused) {
							sleep(10L);
						}
						
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
		
		public void findAndReplace(File src, File dest, PrintStream pr) {
			if(this.findStrings.length == 0) {
				pr.println(String.format("Skipping search within file \"%s\" due to lack of search strings...", src.getAbsolutePath()));
				this.searchesSkipped++;
				this.copy(src, dest, pr);
				return;
			}
			pr.println(String.format("Searching within file \"%s\"...", src.getAbsolutePath()));
			
			List<String> lines = new ArrayList<>();
			try(FileInputStream in = new FileInputStream(src)) {
				String line;
				while((line = readLine(in)) != null) {
					lines.add(line);
					
					while(this.paused) {
						sleep(10L);
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
				return;
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
					
					//TODO re-do this while loop so that it scans along the line, replacing each instance separately, instead of all of them in one go or none of them ...
					
					int index, lastIndex = -1;
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
						
						while(this.paused) {
							sleep(10L);
						}
						
					}
					
					while(this.paused) {
						sleep(10L);
					}
					
				}
				
				replacedLines.add(line);
				
				while(this.paused) {
					sleep(10L);
				}
				
				lineNum++;
			}
			
			if(this.onlyCopyFilesContainingSearchStrings && !foundAnyMatches) {
				pr.println(String.format("\tSkipping copy of file \"%s\" as it does not contain any of the search-strings...", src.getAbsolutePath()));
				this.filesSkipped++;
				return;
			}
			if(!foundAnyMatches && !src.equals(dest)) {
				pr.println(String.format("\tPerforming byte-copy instead of line-by-line copy of file \"%s\" as it does not contain any of the search-strings.", src.getAbsolutePath()));
				this.copy(src, dest, pr);
				return;
			}
			if(!foundAnyMatches && src.equals(dest)) {
				pr.println(String.format("\tSkipping copy of file \"%s\" as it does not contain any of the search-strings, and is the same file as the destination.", src.getAbsolutePath()));
				this.filesSkipped++;
				return;
			}
			
			//Reading the source file and writing to the destination file has been separated out due to the possibility of the user selecting the same source directory as the destination,
			//thereby causing the srcFile and destFile to be the same file! I have intentionally allowed this possibility and worked around it to allow the user to just do an in-place replacement without having to actually copy the files.
			
			pr.println(String.format("\tCopying file \"%s\" to destination file \"%s\" line-by-line...", src.getAbsolutePath(), dest.getAbsolutePath()));
			try(PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dest), StandardCharsets.ISO_8859_1), true)) {
				for(String line : replacedLines) {
					out.println(line);
				}
				out.flush();
				this.filesCopied++;
			} catch(IOException ex) {
				this.fileWritesFailed++;
				pr.print(String.format("Failed to write to destination file \"%s\": ", dest.getAbsolutePath()));
				System.err.print(String.format("Failed to write to destination file \"%s\": ", dest.getAbsolutePath()));
				ex.printStackTrace(pr);
				ex.printStackTrace(System.err);
				pr.flush();
				System.err.flush();
			}
		}
		
		public boolean isSearchPaused() {
			return this.paused;
		}
		
		public FindReplaceSearch pauseSearch() {
			this.paused = true;
			return this;
		}
		
		public FindReplaceSearch resumeSearch() {
			this.paused = false;
			return this;
		}
		
		public FindReplaceSearch stopSearch() {
			Display display;
			while(this.thread != null && this.thread.isAlive()) {
				this.running = false;
				this.paused = false;
				
				display = Display.getCurrent();
				if(display != null && !display.isDisposed()) {
					if(!display.readAndDispatch()) {
						sleep(10L);
					}
				} else {
					sleep(10L);
				}
			}
			this.thread = null;
			return this;
		}
		
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
					while(this.paused) {
						sleep(10L);
					}
					
					String path = file.getAbsolutePath();
					path = path.startsWith(srcPath) ? path.substring(srcPath.length()) : path;
					
					if(this.onlyConsiderTextFiles) {
						String name = file.getName();
						boolean isValidExtension = false;
						if(name.contains(".")) {
							for(String validExtension : this.validFileExtensions) {
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
						sleep(10L);
					}
				} else {
					sleep(10L);
				}
			}
			return this.thread;
		}
		
		public Thread getSearchThread() {
			return this.thread;
		}
		
		public boolean isASearchActive() {
			Thread thread = this.thread;
			return thread != null && thread.isAlive();
		}
		
	}
	
}
