package org.apache.hadoop.BHW;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class GUI{
	public static void main(String[] args){
		Frame gui = new Frame("Hello World");
		gui.setLayout(new GridLayout(4, 2));
		JPanel jp1 = new JPanel(), jp2 = new JPanel();
		JPanel jp3 = new JPanel(), jp4 = new JPanel();
		JPanel jp5 = new JPanel(), jp6 = new JPanel();
		JLabel lr = new JLabel("radius:");
		JLabel lh = new JLabel("height:");
		JLabel la = new JLabel("acceleration(s^-2):");
		JLabel ld = new JLabel("deceleration(s^-2)(>0):");
		JLabel lx = new JLabel("distance:");
		JLabel lt = new JLabel("time(s):");
		JTextField tr = new JTextField("25");
		JTextField th = new JTextField("50");
		JTextField ta = new JTextField("100");
		JTextField td = new JTextField("100");
		JTextField tx = new JTextField("100");
		JTextField tt = new JTextField("2");
		Button b2 = new Button("2");
		jp1.add(lr);jp1.add(tr);jp2.add(lh);jp2.add(th);
		jp3.add(la);jp3.add(ta);jp4.add(ld);jp4.add(td);
		jp5.add(lx);jp5.add(tx);jp6.add(lt);jp6.add(tt);
		JPanel jp12 = new JPanel();
		JPanel jp34 = new JPanel();
		JPanel jp56 = new JPanel();
		jp12.setLayout(new GridLayout(1, 2));
		jp12.add(jp1);jp12.add(jp2);
		jp34.setLayout(new GridLayout(1, 2));
		jp34.add(jp3);jp34.add(jp4);
		jp56.setLayout(new GridLayout(1, 2));
		jp56.add(jp5);jp56.add(jp6);
		gui.add(jp12);
		gui.add(jp34);
		gui.add(jp56);
		Button button = new Button("generate");
		gui.add(button);
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				
			}
		});
		gui.setSize(500, 200);
		gui.setVisible(true);
		gui.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
	}
}
