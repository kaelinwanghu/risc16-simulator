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
		
		inputPanel = new InputPanel(this, 25, 35);
		storageViewer = new StorageViewer(this);
		storageViewer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		execute = new JButton("Execute");
		execute.setFocusable(false);
		execute.setEnabled(false);
		execute.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				execute(false);
			}

		});
		
		executeStep = new JButton("Execute Step");
		executeStep.setFocusable(false);
		executeStep.setEnabled(false);
		executeStep.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				execute(true);
			}

		});

		assemble = new JButton("Assemble");
		assemble.setFocusable(false);
		assemble.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				assemble();
			}

		});

		JButton clear = new JButton("Clear");
		clear.setFocusable(false);
		clear.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				edit(true);
			}

		});

		edit = new JButton("Edit");
		edit.setFocusable(false);
		edit.setEnabled(false);
		edit.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				edit(false);
			}

		});
		
		JButton about = new JButton("About");
		about.setFocusable(false);
		about.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				errorDialog.showAbout();
			}

		});
				
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
				
		pack();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

}
