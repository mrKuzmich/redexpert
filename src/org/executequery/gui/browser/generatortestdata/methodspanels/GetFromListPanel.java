package org.executequery.gui.browser.generatortestdata.methodspanels;

import org.executequery.databaseobjects.DatabaseColumn;
import org.executequery.gui.text.SimpleTextArea;
import org.underworldlabs.swing.layouts.GridBagHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Random;

public class GetFromListPanel extends AbstractMethodPanel {
    private SimpleTextArea textArea;
    private JTextField delimiterField;
    private JComboBox orderBox;
    Object[] listObject;
    JFileChooser openFileDialog;
    private JComboBox sourceBox;
    private JLabel labelFile;
    String[] list;
    private JTextField fileField;
    int index;
    private JButton fileSelectButton;


    public GetFromListPanel(DatabaseColumn col) {
        super(col);
        init();
    }

    private void init() {
        //if(col.getFormattedDataType().contains("BLOB"))
        openFileDialog = new JFileChooser();
        textArea = new SimpleTextArea();
        textArea.getTextAreaComponent().setColumns(20);
        delimiterField = new JTextField(";");
        orderBox = new JComboBox(new String[]{
                "In order", "Random"
        });
        sourceBox = new JComboBox(new String[]{
                "From text area", "From file"
        });
        sourceBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    boolean visibling = sourceBox.getSelectedIndex() == 0;
                    labelFile.setVisible(!visibling);
                    fileField.setVisible(!visibling);
                    fileSelectButton.setVisible(!visibling);
                    textArea.setVisible(visibling);
                }
            }
        });

        fileField = new JTextField();
        fileSelectButton = new JButton("...");
        fileSelectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (col.getFormattedDataType().contains("BLOB"))
                    openFileDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (openFileDialog.showOpenDialog(fileSelectButton) == JOptionPane.OK_OPTION)
                    fileField.setText(openFileDialog.getSelectedFile().getAbsolutePath());

            }
        });


        setLayout(new GridBagLayout());
        GridBagHelper gbh = new GridBagHelper();
        GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 0, 0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0);
        gbh.setDefaults(gbc);
        String labelStr = "Choose directory";
        if (!col.getFormattedDataType().contains("BLOB")) {
            JLabel label = new JLabel("source");
            add(label, gbh.defaults().setLabelDefault().get());
            add(sourceBox, gbh.defaults().nextCol().spanX().get());
            labelStr = "Choose file";
        }
        labelFile = new JLabel(labelStr);
        if (!col.getFormattedDataType().contains("BLOB")) {
            labelFile.setVisible(false);
            fileField.setVisible(false);
            fileSelectButton.setVisible(false);

        }
        JLabel label = new JLabel("method");
        add(label, gbh.defaults().nextRowFirstCol().setLabelDefault().get());
        add(orderBox, gbh.defaults().nextCol().spanX().get());
        if (!col.getFormattedDataType().contains("BLOB")) {
            label = new JLabel("Delimiter");
            add(label, gbh.defaults().nextRowFirstCol().setLabelDefault().get());
            add(delimiterField, gbh.defaults().nextCol().spanX().get());
        }
        add(labelFile, gbh.defaults().nextRowFirstCol().setLabelDefault().get());
        add(fileField, gbh.defaults().nextCol().setMaxWeightX().setMaxWeightY().get());
        add(fileSelectButton, gbh.defaults().nextCol().setLabelDefault().get());
        if (!col.getFormattedDataType().contains("BLOB"))
            add(textArea, gbh.defaults().nextRowFirstCol().fillBoth().spanX().spanY().get());
    }

    @Override

    public Object getTestDataObject() {
        if (first) {
            fillList();
            first = false;
            index = 0;
        }
        if (index >= list.length)
            index = 0;
        if (orderBox.getSelectedIndex() == 1) {
            index = new Random().nextInt(list.length - 1);
        }
        index++;
        return listObject[index - 1];
    }

    private void fillList() {
        if (col.getFormattedDataType().contains("BLOB")) {
            File directory = new File(fileField.getText());
            File[] files = directory.listFiles();
            list = new String[files.length];
            for (int i = 0; i < list.length; i++) {
                list[i] = files[i].getAbsolutePath();
            }
        } else {
            if (sourceBox.getSelectedIndex() == 0)
                list = textArea.getTextAreaComponent().getText().split(delimiterField.getText());
            else {
                try {
                    String s = new String(Files.readAllBytes(Paths.get(fileField.getText())));
                    list = s.split(delimiterField.getText());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        listObject = new Object[list.length];
        for (int i = 0; i < list.length; i++) {
            listObject[i] = objectFromString(list[i]);
        }
    }

    Object objectFromString(String str) {
        if (col.getFormattedDataType().contentEquals("BIGINT")) {
            return new BigInteger(str);
        }
        if (col.getFormattedDataType().contentEquals("INTEGER")) {
            return Integer.parseInt(str);
        }
        if (col.getFormattedDataType().contentEquals("SMALLINT")) {
            return Short.parseShort(str);
        }
        if (col.getFormattedDataType().contentEquals("TIME")) {
            return Time.valueOf(str);
        }
        if (col.getFormattedDataType().contentEquals("DATE")) {
            return Date.valueOf(str);
        }
        if (col.getFormattedDataType().contentEquals("TIMESTAMP")) {
            return Timestamp.valueOf(str);
        }
        if (col.getFormattedDataType().contentEquals("DOUBLE PRECISION")
                || col.getFormattedDataType().contentEquals("FLOAT")
                || col.getFormattedDataType().startsWith("DECIMAL")
                || col.getFormattedDataType().startsWith("NUMERIC")) {
            return Double.valueOf(str);
        }
        if (col.getFormattedDataType().contains("CHAR")) {
            return str;
        }
        if (col.getFormattedDataType().contains("BLOB")) {
            try {
                return new FileInputStream(str);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
