package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

import engine.Assembler;
import engine.Processor;
import gui.dialogs.MessageDialog;
import gui.dialogs.InstructionSetDialog;
import gui.dialogs.ScheduleDialog;

@SuppressWarnings("serial")
public class Simulator extends JFrame {

	public static Processor processor;
	
	private InputPanel inputPanel;
	public AssemblyPanel assemblyPanel;
	public StorageViewer storageViewer;

	public MessageDialog errorDialog;
	private ScheduleDialog scheduleDialog;
	public InstructionSetDialog instructionSetDialog;

	private FileManager fileManager;
	private AutoSaver autoSaver;
	private RecentFiles recentFiles;
	
	private JPanel main;
	private JButton execute;
	private JButton executeStep;
	private JButton assemble;
	private JButton edit;
	
	public Simulator() {
		super("Architectural Simulator");

		try {
			 UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			
		}
		UIManager.put("Label.font", new Font("Consolas", Font.PLAIN, 17));
		UIManager.put("Label.foreground", Color.BLUE);
		UIManager.put("Button.font", new Font("Consolas", Font.PLAIN, 17));
		UIManager.put("TextField.font", new Font("Consolas", Font.PLAIN, 17));
		UIManager.put("TextArea.font", new Font("Consolas", Font.PLAIN, 17));
		UIManager.put("ComboBox.font", new Font("Consolas", Font.PLAIN, 17));
		UIManager.put("Table.font", new Font("Consolas", Font.PLAIN, 17));
		UIManager.put("TableHeader.font", new Font("Consolas", Font.PLAIN, 17));
		UIManager.put("Table.foreground", new Color(100, 100, 100));
						
		ImageIcon i1 = new ImageIcon(getClass().getClassLoader().getResource("gui/resources/microchip1.png")); 
		ImageIcon i2 = new ImageIcon(getClass().getClassLoader().getResource("gui/resources/microchip2.png"));		
		setIconImages(Arrays.asList(i1.getImage(), i2.getImage()));
		
		int[][] cacheCofig = {
			{1024, 0, 32, 100},
			{2, 8, 8, 5},
			{4, 16, 1, 5, 0, 2}
		};
					
		int[][] unitsConfig = {
			{6},
			{1, 2, 1},
			{1, 2, 2},
			{1, 2, 5},
			{1, 2, 10},
			{1, 2},
			{1, 2}
		};
		
		processor = new Processor(cacheCofig, unitsConfig);		
		
		errorDialog = new MessageDialog(this);
		scheduleDialog = new ScheduleDialog(this);
		instructionSetDialog = new InstructionSetDialog(this);
		fileManager = new FileManager(this);
		recentFiles = new RecentFiles();
		
		inputPanel = new InputPanel(this, 25, 35);
		storageViewer = new StorageViewer(this);
		storageViewer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		autoSaver = new AutoSaver(fileManager, inputPanel);
		autoSaver.start();
		String recovered = fileManager.recoverAutoSave();
		if (recovered != null)
		{
			int response = JOptionPane.showConfirmDialog(
				this,
				"An auto-saved file was found. Would you like to recover it?",
				"Auto-Save Recovery",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE
			);
			
			if (response == JOptionPane.YES_OPTION)
			{
				inputPanel.setProgram(recovered);
			}
			else
			{
				fileManager.clearAutoSave();
			}
		}

		execute = new JButton("Execute");
		execute.setFocusable(false);
		execute.setEnabled(false);
		execute.addActionListener(e -> execute(false));
		
		executeStep = new JButton("Execute Step");
		executeStep.setFocusable(false);
		executeStep.setEnabled(false);
		executeStep.addActionListener(e -> execute(true));

		assemble = new JButton("Assemble");
		assemble.setFocusable(false);
		assemble.addActionListener(e -> assemble());

		JButton clear = new JButton("Clear");
		clear.setFocusable(false);
		clear.addActionListener(e -> edit(true));

		edit = new JButton("Edit");
		edit.setFocusable(false);
		edit.setEnabled(false);
		edit.addActionListener(e -> edit(false));
		
		JButton about = new JButton("About");
		about.setFocusable(false);
		about.addActionListener(e -> errorDialog.showAbout());
				
		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		p1.add(assemble);
		p1.add(edit);
		p1.add(clear);
		
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		p2.add(execute);
		p2.add(executeStep);
		p2.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		
		JPanel p3 = new JPanel(new BorderLayout(0, 5));
		p3.add(p1, BorderLayout.NORTH);
		p3.add(p2);
		p3.add(about, BorderLayout.SOUTH);
		
		main = new JPanel(new BorderLayout(0, 10));
		main.add(inputPanel);
		main.add(p3, BorderLayout.SOUTH);
		main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		add(main);
		add(storageViewer, BorderLayout.EAST);
		setResizable(false);

		JMenuBar menuBar = new JMenuBar();

		// File menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');

		JMenuItem newItem = new JMenuItem("New");
		newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		newItem.addActionListener(e -> newFile());
		fileMenu.add(newItem);

		JMenuItem openItem = new JMenuItem("Open...");
		openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		openItem.addActionListener(e -> openFile());
		fileMenu.add(openItem);

		fileMenu.addSeparator();

		JMenuItem saveItem = new JMenuItem("Save");
		saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		saveItem.addActionListener(e -> saveFile());
		fileMenu.add(saveItem);

		JMenuItem saveAsItem = new JMenuItem("Save As...");
		saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 
			InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		saveAsItem.addActionListener(e -> saveFileAs());
		fileMenu.add(saveAsItem);

		fileMenu.addSeparator();

		// Recent files submenu
		JMenu recentMenu = new JMenu("Recent Files");
		updateRecentFilesMenu(recentMenu);
		fileMenu.add(recentMenu);

		menuBar.add(fileMenu);

		// Help menu
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');

		JMenuItem aboutItem = new JMenuItem("About");
		aboutItem.addActionListener(e -> errorDialog.showAbout());
		helpMenu.add(aboutItem);

		JMenuItem instructionSetItem = new JMenuItem("Instruction Set");
		instructionSetItem.addActionListener(e -> instructionSetDialog.setVisible(true));
		helpMenu.add(instructionSetItem);

		menuBar.add(helpMenu);
		setJMenuBar(menuBar);
				
		pack();
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				autoSaver.stop();
				System.exit(0);
			}
		});
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(null);
	}
	
	private void execute(boolean stepped) {
		try {
			boolean finished = processor.execute(stepped);
			storageViewer.refresh();
			assemblyPanel.repaint();
			if (finished) {
				scheduleDialog.showSchedule();
				execute.setEnabled(false);
				executeStep.setEnabled(false);
				assemble.setEnabled(true);
			}
		} catch (Exception ex) {
			storageViewer.refresh();
			assemblyPanel.repaint();
			errorDialog.showError(ex.getMessage());
		}
	}
	
	private void assemble() {
		try {
			Assembler.assemble(inputPanel.getProgram(), processor);
			storageViewer.refresh();
			execute.setEnabled(true);
			executeStep.setEnabled(true);
			edit.setEnabled(true);
			assemble.setEnabled(false);
			main.remove(inputPanel);
			try {
				main.remove(assemblyPanel);
			} catch (Exception ex) {

			}
			assemblyPanel = new AssemblyPanel(storageViewer.hex.getText().equals("HEX"));
			main.add(assemblyPanel);
			main.validate();
			scheduleDialog.setVisible(false);
		} catch (Exception ex) {
			processor.clear();
			errorDialog.showError(ex.getMessage());
		}
	}
	
	public void edit(boolean clear) {
		if (clear) {
			inputPanel.clear();
		}
		processor.clear();
		storageViewer.refresh();
		execute.setEnabled(false);
		edit.setEnabled(false);
		executeStep.setEnabled(false);
		assemble.setEnabled(true);
		try {
			main.remove(assemblyPanel);
		} catch (Exception ex) {

		}
		main.add(inputPanel);
		main.validate();
		repaint();
		scheduleDialog.setVisible(false);
	}
	 
	public static void main(String[] args) {
		new Simulator().setVisible(true);
	}
	
	private void newFile()
	{
		// Confirm if there's unsaved work
		int response = JOptionPane.showConfirmDialog(
			this,
			"Start a new file? Any unsaved changes will be lost.",
			"New File",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE
		);
		
		if (response == JOptionPane.OK_OPTION)
		{
			inputPanel.clear();
			fileManager.newFile();
			edit(false);
		}
	}

	private void openFile()
	{
		String content = fileManager.openFile();
		if (content != null)
		{
			inputPanel.clear();
			// Set the loaded content (you'll need to add this method to InputPanel)
			inputPanel.setProgram(content);
			edit(false);
			
			// Add to recent files
			File currentFile = fileManager.getCurrentFile();
			if (currentFile != null)
			{
				recentFiles.addFile(currentFile);
			}
		}
	}

	private void saveFile()
	{
		String content = inputPanel.getProgram();
		boolean success = fileManager.save(content);
		
		if (success)
		{
			// Add to recent files
			File currentFile = fileManager.getCurrentFile();
			if (currentFile != null)
			{
				recentFiles.addFile(currentFile);
			}
			
			// Clear auto-save since we just saved manually
			fileManager.clearAutoSave();
		}
	}

	private void saveFileAs()
	{
		String content = inputPanel.getProgram();
		boolean success = fileManager.saveAs(content);
		
		if (success)
		{
			// Add to recent files
			File currentFile = fileManager.getCurrentFile();
			if (currentFile != null)
			{
				recentFiles.addFile(currentFile);
			}
			
			// Clear auto-save
			fileManager.clearAutoSave();
		}
	}

	private void updateRecentFilesMenu(JMenu recentMenu)
	{
		recentMenu.removeAll();
		
		List<File> recent = recentFiles.getRecentFiles();
		
		if (recent.isEmpty())
		{
			JMenuItem emptyItem = new JMenuItem("(No recent files)");
			emptyItem.setEnabled(false);
			recentMenu.add(emptyItem);
		}
		else
		{
			for (final File file : recent)
			{
				JMenuItem item = new JMenuItem(file.getName());
				item.setToolTipText(file.getAbsolutePath());
				item.addActionListener(e -> {
					String content = fileManager.loadFile(file);
					if (content != null)
					{
						inputPanel.clear();
						inputPanel.setProgram(content);
						edit(false);
						recentFiles.addFile(file);
					}
				});
				recentMenu.add(item);
			}
			
			recentMenu.addSeparator();
			
			JMenuItem clearItem = new JMenuItem("Clear Recent Files");
			clearItem.addActionListener(e -> recentFiles.clear());
			recentMenu.add(clearItem);
		}
	}
}
