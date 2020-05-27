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
package com.gmail.br45entei.main;

import com.gmail.br45entei.io.FindReplaceSearch;
import com.gmail.br45entei.util.FileUtil;
import com.gmail.br45entei.util.SWTUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
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

import etinyplugins.commons.swt.UndoRedoImpl;

/** @author Brian_Entei */
public final class BatchFindAndReplace {
	
	protected Display display;
	protected Shell shell;
	protected StyledText stxtFind;
	protected UndoRedoImpl stxtFindUndoRedo;
	protected StyledText stxtReplaceWith;
	protected UndoRedoImpl stxtReplaceWithUndoRedo;
	
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
	public BatchFindAndReplace() {
		this.createContents();
	}
	
	/** Opens this dialog.
	 * 
	 * @param runLoop If <tt>true</tt>, this method will block and call
	 *            {@link Display#readAndDispatch() readAndDispatch} until the
	 *            dialog is hidden, closed, or disposed.
	 * @return This dialog */
	public BatchFindAndReplace open(boolean runLoop) {
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
	public BatchFindAndReplace open() {
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
		BatchFindAndReplace main = new BatchFindAndReplace();
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
				BatchFindAndReplace.this.updateUI();
			}
		});
		
		Label lblFind = new Label(this.shell, SWT.NONE);
		lblFind.setBounds(10, 10, 40, 15);
		lblFind.setText("Find:");
		
		Label lblEachSearchString = new Label(this.shell, SWT.NONE);
		lblEachSearchString.setBounds(125, 10, 649, 15);
		lblEachSearchString.setText("Each search-string is separated via a new line, and you can specify a case-insensitive search by starting the line with (?i).");
		
		this.stxtFind = new StyledText(this.shell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		this.stxtFind.setToolTipText("Enter your search-string(s) here; each one must be placed on its own line.\r\nStart the line off with (?i) to make the search case-insensitive for that search-string.");
		this.stxtFind.setBounds(10, 31, this.shell.getSize().x - 36, 95);
		this.stxtFindUndoRedo = new UndoRedoImpl(this.stxtFind);
		this.stxtFind.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.keyCode == SWT.TAB) {
					e.doit = false;
					boolean shiftPressed = (e.stateMask & SWT.SHIFT) != 0;
					if(shiftPressed) {
						BatchFindAndReplace.this.stxtOutput.forceFocus();
					} else {
						BatchFindAndReplace.this.stxtReplaceWith.forceFocus();
					}
				}
			}
		});
		this.stxtFind.addVerifyKeyListener(new VerifyKeyListener() {
			@Override
			public void verifyKey(VerifyEvent e) {
				if(e.keyCode == SWT.TAB) {
					e.doit = false;
				}
			}
		});
		SWTUtil.addTextEditorPopupMenu(this.stxtFind, this.stxtFindUndoRedo);
		
		Label lblReplaceWith = new Label(this.shell, SWT.NONE);
		lblReplaceWith.setBounds(10, 132, 80, 15);
		lblReplaceWith.setText("Replace with:");
		
		Label lblEachReplacementString = new Label(this.shell, SWT.NONE);
		lblEachReplacementString.setBounds(125, 132, 649, 15);
		lblEachReplacementString.setText("Each replacement is paired with the corresponding line above, and %s is replaced with the string found in the file.");
		
		this.stxtReplaceWith = new StyledText(this.shell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		this.stxtReplaceWith.setToolTipText("Enter your replacement strings here. (They must be in the same order as the search strings to make a pair)\r\nUse %s to have the matched string from the file be a part of the replacement string.");
		this.stxtReplaceWith.setBounds(10, 153, this.shell.getSize().x - 36, 95);
		this.stxtReplaceWithUndoRedo = new UndoRedoImpl(this.stxtReplaceWith);
		this.stxtReplaceWith.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.keyCode == SWT.TAB) {
					boolean shiftPressed = (e.stateMask & SWT.SHIFT) != 0;
					if(shiftPressed) {
						BatchFindAndReplace.this.stxtFind.forceFocus();
					} else {
						BatchFindAndReplace.this.btnOnlyCopyFiles.forceFocus();
					}
				}
			}
		});
		this.stxtReplaceWith.addVerifyKeyListener(new VerifyKeyListener() {
			@Override
			public void verifyKey(VerifyEvent e) {
				if(e.keyCode == SWT.TAB) {
					e.doit = false;
				}
			}
		});
		SWTUtil.addTextEditorPopupMenu(this.stxtReplaceWith, this.stxtReplaceWithUndoRedo);
		
		this.lblSeparator = new Label(this.shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		this.lblSeparator.setBounds(10, 254, this.shell.getSize().x - 36, 2);
		
		this.btnOnlyCopyFiles = new Button(this.shell, SWT.CHECK);
		this.btnOnlyCopyFiles.setToolTipText("Check this box to only copy files containing one or more of the search-strings you specified in the Find: text field above.\r\nLeave this box unchecked to copy all files with the specified folder below.");
		this.btnOnlyCopyFiles.setBounds(10, 262, 314, 16);
		this.btnOnlyCopyFiles.setText("Only copy files containing one or more search-strings");
		
		this.btnRecursiveFileSearch = new Button(this.shell, SWT.CHECK);
		this.btnRecursiveFileSearch.setToolTipText("Check this box to search for and copy files within the subfolders (and their children etc.) of the specified folder below.");
		this.btnRecursiveFileSearch.setBounds(330, 262, 137, 16);
		this.btnRecursiveFileSearch.setText("Recursive file search");
		
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
				DirectoryDialog dialog = new DirectoryDialog(BatchFindAndReplace.this.shell, SWT.NONE);
				dialog.setText("Choose Source Folder");
				dialog.setMessage("Please select the folder where the Find/Replace search will take place.");
				String checkPath = BatchFindAndReplace.this.txtSourceFolderPath.getText();
				if(!checkPath.isEmpty() && new File(checkPath).isDirectory()) {
					dialog.setFilterPath(checkPath);
				}
				
				String path = dialog.open();
				BatchFindAndReplace.this.txtSourceFolderPath.setText(path == null ? dialog.getFilterPath() : path);
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
				DirectoryDialog dialog = new DirectoryDialog(BatchFindAndReplace.this.shell, SWT.NONE);
				dialog.setText("Choose Destination Folder");
				dialog.setMessage("Please select the folder where the Find/Replace search will output files to.");
				String checkPath = BatchFindAndReplace.this.txtDestinationFolderPath.getText();
				if(!checkPath.isEmpty() && new File(checkPath).isDirectory()) {
					dialog.setFilterPath(checkPath);
				}
				
				String path = dialog.open();
				BatchFindAndReplace.this.txtDestinationFolderPath.setText(path == null ? dialog.getFilterPath() : path);
			}
		});
		this.btnBrowseDestinationPath.setText("Browse...");
		this.btnBrowseDestinationPath.setBounds(this.shell.getSize().x - 101, 311, 75, 25);
		
		this.lblSeparator_1 = new Label(this.shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		this.lblSeparator_1.setBounds(10, 340, this.shell.getSize().x - 36, 2);
		
		this.btnStartFindReplaceSearch = new Button(this.shell, SWT.PUSH);
		this.btnStartFindReplaceSearch.setToolTipText("Click to start the Find/Replace Search");
		this.btnStartFindReplaceSearch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				File sourceFolder = new File(BatchFindAndReplace.this.txtSourceFolderPath.getText());
				File destinationFolder = new File(BatchFindAndReplace.this.txtDestinationFolderPath.getText());
				boolean onlyCopyFilesContainingSearchStrings = BatchFindAndReplace.this.btnOnlyCopyFiles.getSelection();
				boolean recursive = BatchFindAndReplace.this.btnRecursiveFileSearch.getSelection();
				boolean onlyConsiderTextFiles = BatchFindAndReplace.this.btnOnlyConsidertxt.getSelection();
				String[] findStrings, replaceStrings;
				{
					List<String> fs = new ArrayList<>(Arrays.asList(BatchFindAndReplace.this.stxtFind.getText().split(Pattern.quote("\n"))));
					List<String> rs = new ArrayList<>(Arrays.asList(BatchFindAndReplace.this.stxtReplaceWith.getText().split(Pattern.quote("\n"))));
					
					List<String> cfs = new ArrayList<>(),//	cleaned Find-Strings
							crs = new ArrayList<>();//		cleaned Replacement-Strings
					int i = 0;
					for(; i < fs.size(); i++) {
						String searchString = fs.get(i);
						searchString = searchString.endsWith("\r") ? searchString.substring(0, searchString.length() - 1) : searchString;
						if(searchString.isEmpty() || (searchString.toLowerCase().startsWith("(?i)") && searchString.substring(4).isEmpty())) {
							continue;
						}
						cfs.add(searchString);
						String replacementString = i < rs.size() ? rs.get(i) : null;
						replacementString = replacementString != null && replacementString.endsWith("\r") ? replacementString.substring(0, replacementString.length() - 1) : replacementString;
						if(replacementString != null && !(i + 1 == rs.size() ? replacementString.isEmpty() : false)) {
							crs.add(replacementString);
						} else {
							crs.add("%s");
						}
					}
					if(i < rs.size()) {
						for(; i < rs.size(); i++) {
							String unusedReplacementString = rs.get(i);
							unusedReplacementString = unusedReplacementString.endsWith("\r") ? unusedReplacementString.substring(0, unusedReplacementString.length() - 1) : unusedReplacementString;
							crs.add(unusedReplacementString);
						}
					}
					findStrings = cfs.toArray(new String[cfs.size()]);
					replaceStrings = crs.toArray(new String[crs.size()]);
				}
				
				if(!sourceFolder.isDirectory()) {
					MessageBox box = new MessageBox(BatchFindAndReplace.this.shell, SWT.ICON_ERROR | SWT.OK);
					box.setText("Error Opening Source Folder");
					box.setMessage("Unable to open the specified source folder.\nPlease check that it exists and is accessible, and then try again.");
					
					box.open();
					return;
				}
				destinationFolder.mkdirs();
				if(!destinationFolder.isDirectory()) {
					MessageBox box = new MessageBox(BatchFindAndReplace.this.shell, SWT.ICON_ERROR | SWT.OK);
					box.setText("Error Opening Destination Folder");
					box.setMessage("Unable to open the specified destination folder.\nPlease check that it exists( or can be created) and is accessible, and then try again.");
					
					box.open();
					return;
				}
				
				BatchFindAndReplace.this.startFindReplaceSearch(new FindReplaceSearch(sourceFolder, destinationFolder, onlyCopyFilesContainingSearchStrings, recursive, onlyConsiderTextFiles, findStrings, replaceStrings));
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
				FindReplaceSearch search = BatchFindAndReplace.this.activeSearch;
				if(search != null) {
					if(BatchFindAndReplace.this.btnPauseSearch.getSelection()) {
						search.pauseSearch();
					} else {
						search.resumeSearch();
					}
				} else {
					BatchFindAndReplace.this.btnPauseSearch.setSelection(false);
				}
				
				BatchFindAndReplace.this.btnPauseSearch.setText(BatchFindAndReplace.this.btnPauseSearch.getSelection() ? "Resume Search" : "Pause Search");
			}
		});
		this.btnPauseSearch.setBounds(172, 348, 96, 25);
		this.btnPauseSearch.setText("Pause Search");
		
		this.btnStopSearch = new Button(this.shell, SWT.NONE);
		this.btnStopSearch.setToolTipText("Click to stop the Find/Replace Search");
		this.btnStopSearch.setEnabled(false);
		this.btnStopSearch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FindReplaceSearch search = BatchFindAndReplace.this.activeSearch;
				if(search != null) {
					BatchFindAndReplace.this.btnStopSearch.setEnabled(false);
					BatchFindAndReplace.this.btnPauseSearch.setEnabled(false);
					search.stopSearch();
				}
			}
		});
		this.btnStopSearch.setBounds(274, 348, 80, 25);
		this.btnStopSearch.setText("Stop Search");
		
		this.lblSeparator_2 = new Label(this.shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		this.lblSeparator_2.setBounds(10, 379, this.shell.getSize().x - 36, 2);
		
		this.stxtOutput = new StyledText(this.shell, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
		this.stxtOutput.setFont(SWTResourceManager.getFont("Consolas", 8, SWT.NORMAL));
		this.stxtOutput.setTabs(4);
		this.stxtOutput.setToolTipText("The output of the current Find/Replace Search");
		this.stxtOutput.setSelectionForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		this.stxtOutput.setSelectionBackground(SWTResourceManager.getColor(SWT.COLOR_GREEN));
		this.stxtOutput.setForeground(SWTResourceManager.getColor(SWT.COLOR_GREEN));
		this.stxtOutput.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		this.stxtOutput.setBounds(10, 387, this.shell.getSize().x - 36, this.shell.getSize().y - 436);
		SWTUtil.addTextEditorPopupMenu(this.stxtOutput, null);
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
	
	/** Starts the given find & replace search operation.<br>
	 * <b>Note:</b>&nbsp;This method blocks until the operation is completed.
	 * 
	 * @param search The search to be performed */
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
		try(PrintStream pr = FileUtil.wrapOutputStream(baos, "\n")) {
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
			text = text.length() > 20000 ? text.substring(text.length() - 20000) : text;//Keep text length at or under 20000 to prevent excessive lag and possible crashes
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
	public BatchFindAndReplace dispose() {
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
	
}
