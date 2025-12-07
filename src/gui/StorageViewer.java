package gui;

import gui.components.ResizableTable;
import gui.dialogs.StorageSettingsDialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;

@SuppressWarnings("serial")
public class StorageViewer extends JPanel {
	
	public JButton hex;
	private JTextArea data;
	private JComboBox<String> type;
	private ResizableTable resizableTable;
	private StorageSettingsDialog storageSettings;
	
	public StorageViewer(final Simulator simulator) {
		super(new BorderLayout(0, 10));
		
		storageSettings = new StorageSettingsDialog(simulator);
		
		resizableTable = new ResizableTable(new int[]{20, 10, 0, 0});
		resizableTable.setRowHeight(20);
		resizableTable.setIntercellSpacing(new Dimension(10, 0));

		JScrollPane scrollPane = new JScrollPane(resizableTable); 
		scrollPane.setBorder(new LineBorder(Color.GRAY, 1));
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setOpaque(false);
		
		type = new JComboBox<String>(new String[]{"Registers", "Memory"});
		type.setFocusable(false);
		type.addItemListener(new ItemListener() {
			
			public void itemStateChanged(ItemEvent arg0) {
				refresh();
			}
			
		});

		hex = new JButton("HEX");
		hex.setFocusable(false);
		hex.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				hex.setText(hex.getText().equals("HEX")? "DEC" : "HEX");
				refresh();
				if (simulator.assemblyPanel != null)
					simulator.assemblyPanel.setFormat(hex.getText().equals("HEX"));
			}

		});
		
		data = new JTextArea(3, 10);
		data.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(5, 10, 5, 5)));
		data.setEnabled(false);
		data.setDisabledTextColor(new Color(100, 100, 100));
		
		JButton settings = new JButton("Settings");
		settings.setFocusable(false);
		settings.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				storageSettings.setVisible(true);
			}

		});
		
		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		p1.add(settings);
		p1.add(Box.createRigidArea(new Dimension(5, 0)));
		p1.add(hex);
		
		JPanel typePanel = new JPanel(new BorderLayout(0, 10));
		typePanel.add(type, BorderLayout.WEST);
		typePanel.add(p1, BorderLayout.EAST);
		
		JLabel l1 = new JLabel("Storage");
		l1.setFont(new Font("Consolas", Font.PLAIN, 19));
		l1.setForeground(Color.RED);
		
		JPanel p3 = new JPanel(new BorderLayout(0, 10));
		p3.add(l1, BorderLayout.NORTH);
		p3.add(typePanel);
		p3.add(data, BorderLayout.SOUTH);
		
		add(p3, BorderLayout.NORTH);
		add(scrollPane);
		
		refresh();
	}

	public void refresh() {
		Object[] text = null;
		boolean isHex = hex.getText().equals("HEX");
		int levels = type.getItemCount() - 4;
		switch(type.getSelectedIndex()) {
			case 0 : text = Simulator.processor.getRegisterFile().displayRegisters(isHex); break;
			case 1 : text = Simulator.processor.getMemory().displayDataWords(isHex); break;
		}
		resizableTable.setData((String[][])text[0], (String[])text[1]);
		data.setText((String) text[2]);
	}
	
	public void refreshTypes() {
		ArrayList<String> types = new ArrayList<String>(
				Arrays.asList("Registers", "L1 Instruction Cache", "L1 Data Cache", "Memory (Words)", "Memory (Bytes)"));
		if (Simulator.processor.getDataCache(1) != null)
			types.add(3, "L2 Data Cache");
		if (Simulator.processor.getDataCache(2) != null)
			types.add(4, "L3 Data Cache");
		
		type.setModel(new DefaultComboBoxModel<String>(types.toArray(new String[0])));
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(500, super.getPreferredSize().height);
	}
	
}
