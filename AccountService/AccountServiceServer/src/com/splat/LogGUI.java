package com.splat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class LogGUI {
    JTextArea incoming = null;
    JTextField fieldTotalAddAmount = null;
    JTextField fieldTotalGetAmount = null;
    JLabel labelTotalAddAmount = null;
    JLabel labelTotalGetAmount = null;
    JTextField fieldLastSecAddAmount = null;
    JTextField fieldLastSecGetAmount = null;
    JLabel labelLastSecAddAmount = null;
    JLabel labelLastSecGetAmount = null;

    public void logIntervalPrint(int get, int add) {
        Date d = new Date();
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy hh:mm");

        String str = String.format(" Statistics (for last %d sec): getAmount calls=%d; addAmount calls=%d",
                1, get, add);
        getIncoming().append(format.format(d) + " : " + str + "\n");
    }

    public void lastSecPrint(int get, int add) {
        getFieldLastSecGetAmount().setText(" " + get);
        getFieldLastSecAddAmount().setText(" " + add);
    }

    public void logTotalPrint(int get, int add) {
        getFieldTotalGetAmount().setText(" " + get);
        getFieldTotalAddAmount().setText(" " + add);
    }

    public void log(int n) {

        JFrame frame = new JFrame("LOG");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        incoming = new JTextArea(15, 45);
        incoming.setLineWrap(true);
        incoming.setWrapStyleWord(true);
        incoming.setEditable(false);
        JScrollPane qScroller = new JScrollPane(incoming);
        qScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        qScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        fieldTotalAddAmount = new JTextField(10);
        fieldTotalGetAmount = new JTextField(10);

        labelTotalAddAmount = new JLabel("Total AddAmount calls:");
        labelTotalGetAmount = new JLabel("Total GetAmount calls:");

        fieldLastSecAddAmount = new JTextField(6);
        fieldLastSecGetAmount = new JTextField(6);

        labelLastSecAddAmount = new JLabel("Last second AddAmount calls: ");
        labelLastSecGetAmount = new JLabel("Last second GetAmount calls: ");

        JButton clearLog = new JButton("Clear Log");
        clearLog.addActionListener(new clearLogListener());

        JPanel mainPanel = new JPanel();

        mainPanel.add(qScroller);

        mainPanel.add(labelLastSecGetAmount);
        mainPanel.add(fieldLastSecGetAmount);
        mainPanel.add(labelLastSecAddAmount);
        mainPanel.add(fieldLastSecAddAmount);

        mainPanel.add(labelTotalGetAmount);
        mainPanel.add(fieldTotalGetAmount);
        mainPanel.add(labelTotalAddAmount);
        mainPanel.add(fieldTotalAddAmount);

        mainPanel.add(clearLog);

        frame.getContentPane().add(BorderLayout.CENTER, mainPanel);
        frame.setSize(550, 390);
        frame.setVisible(true);
    }

    public JTextArea getIncoming() {
        return incoming;
    }

    public JTextField getFieldTotalAddAmount() {
        return fieldTotalAddAmount;
    }

    public JTextField getFieldTotalGetAmount() {
        return fieldTotalGetAmount;
    }

    public JTextField getFieldLastSecAddAmount() {
        return fieldLastSecAddAmount;
    }

    public JTextField getFieldLastSecGetAmount() {
        return fieldLastSecGetAmount;
    }

    private class clearLogListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            getIncoming().setText("");
            Server obj = new Server();
            try {
                obj.clearLogs();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }
}
