package org.executequery.gui.table;

import org.executequery.gui.browser.ColumnData;
import org.executequery.log.Log;
import org.underworldlabs.swing.NumberTextField;
import org.underworldlabs.util.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectTypePanel extends JPanel {
    private JLabel typeLabel;
    private JLabel sizeLabel;
    private JLabel scaleLabel;
    private JLabel subtypeLabel;
    private JLabel encodingLabel;
    private JComboBox typeBox;
    private JComboBox encodingBox;
    private NumberTextField sizeField;
    private NumberTextField scaleField;
    private NumberTextField subtypeField;

    String[] dataTypes;
    int[] intDataTypes;
    KeyListener keyListener;
    ColumnData cd;
    boolean refreshing = false;
    List<String> charsets;
    Map<Integer, String> types;

    public SelectTypePanel(String[] types, int[] intTypes, ColumnData cd) {
        this.dataTypes = types;
        this.intDataTypes = intTypes;
        sortTypes();
        removeDuplicates();
        this.cd = cd;
        loadCharsets();
        init();
    }

    void init() {
        typeLabel = new JLabel("Type");
        sizeLabel = new JLabel("Size");
        scaleLabel = new JLabel("Scale");
        subtypeLabel = new JLabel("Subtype");
        encodingLabel = new JLabel("Encoding");
        typeBox = new JComboBox();
        encodingBox = new JComboBox();
        sizeField = new NumberTextField();
        scaleField = new NumberTextField();
        subtypeField = new NumberTextField();

        keyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {

            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {

            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
                NumberTextField field = (NumberTextField) keyEvent.getSource();
                if (field.getValue() <= 0)
                    field.setValue(1);
                if (field == sizeField) {
                    cd.setColumnSize(field.getValue());
                } else if (field == scaleField) {
                    cd.setColumnScale(field.getValue());
                } else if (field == subtypeField) {
                    cd.setColumnSubtype(field.getValue());
                }
            }
        };


        typeBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                refreshType();
            }
        });

        typeBox.setModel(new DefaultComboBoxModel(dataTypes));
        typeBox.setSelectedIndex(0);

        encodingBox.setModel(new DefaultComboBoxModel(charsets.toArray(new String[charsets.size()])));
        encodingBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                cd.setCharset((String) encodingBox.getSelectedItem());
            }
        });

        this.setLayout(new GridBagLayout());
        this.add(typeLabel, new GridBagConstraints(0, 0, 1, 1,
                0, 0, GridBagConstraints.NORTH,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
        this.add(sizeLabel, new GridBagConstraints(0, 1, 1, 1,
                0, 0, GridBagConstraints.NORTH,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
        this.add(scaleLabel, new GridBagConstraints(0, 2, 1, 1,
                0, 0, GridBagConstraints.NORTH,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
        this.add(subtypeLabel, new GridBagConstraints(0, 3, 1, 1,
                0, 0, GridBagConstraints.NORTH,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
        this.add(encodingLabel, new GridBagConstraints(0, 4, 1, 1,
                0, 0, GridBagConstraints.NORTH,
                GridBagConstraints.NONE, new Insets(1, 1, 1, 1), 0, 0));
        this.add(typeBox, new GridBagConstraints(1, 0, 1, 1,
                1, 0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));
        this.add(sizeField, new GridBagConstraints(1, 1, 1, 1,
                1, 0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));
        this.add(scaleField, new GridBagConstraints(1, 2, 1, 1,
                1, 0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));
        this.add(subtypeField, new GridBagConstraints(1, 3, 1, 1,
                1, 0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));
        this.add(encodingBox, new GridBagConstraints(1, 4, 1, 1,
                1, 0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));
        this.add(new JPanel(), new GridBagConstraints(1, 4, 1, 1,
                1, 1, GridBagConstraints.NORTH,
                GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));
    }

    void refreshType() {
        int index = typeBox.getSelectedIndex();
        cd.setColumnType(dataTypes[index]);
        cd.setSQLType(intDataTypes[index]);
        setSizeVisible(cd.getSQLType() == Types.NUMERIC || cd.getSQLType() == Types.CHAR || cd.getSQLType() == Types.VARCHAR
                || cd.getSQLType() == Types.DECIMAL || cd.getSQLType() == Types.BLOB
                || cd.getSQLType() == Types.LONGVARBINARY || cd.getSQLType() == Types.LONGVARCHAR
                || cd.getColumnType().toUpperCase().equals("VARCHAR")
                || cd.getColumnType().toUpperCase().equals("CHAR"));
        setScaleVisible(cd.getSQLType() == Types.NUMERIC || cd.getSQLType() == Types.DECIMAL);
        setScaleVisible(cd.getSQLType() == Types.NUMERIC || cd.getSQLType() == Types.DECIMAL);
        setSubtypeVisible(cd.getSQLType() == Types.BLOB);
        setEncodingVisible(cd.getSQLType() == Types.CHAR || cd.getSQLType() == Types.VARCHAR
                || cd.getSQLType() == Types.LONGVARCHAR || cd.getSQLType() == Types.CLOB
                || cd.getColumnType().toUpperCase().equals("VARCHAR")
                || cd.getColumnType().toUpperCase().equals("CHAR"));

        if (cd.getSQLType() == Types.LONGVARBINARY || cd.getSQLType() == Types.LONGVARCHAR || cd.getSQLType() == Types.BLOB) {
            sizeField.setText("80");
        }
        if (cd.getSQLType() == Types.LONGVARBINARY)
            subtypeField.setText("0");
        if (cd.getSQLType() == Types.LONGVARCHAR)
            subtypeField.setText("1");
        if (cd.getSQLType() == Types.BLOB)
            subtypeField.setText("0");
    }

    public void refreshColumn() {
        cd.setColumnSize(sizeField.getValue());
        cd.setColumnScale(scaleField.getValue());
        cd.setColumnSubtype(subtypeField.getValue());
    }

    void setSizeVisible(boolean flag) {
        sizeField.setEnabled(flag);
        //sizeLabel.setEnabled(flag);
        if (flag)
            sizeField.setValue(1);
        else sizeField.setValue(0);
        if (refreshing)
            sizeField.setValue(cd.getColumnSize());
        cd.setColumnSize(sizeField.getValue());
    }

    void setScaleVisible(boolean flag) {
        scaleField.setEnabled(flag);
        //scaleLabel.setVisible(flag);
        if (flag) {
            scaleField.setValue(1);
        } else scaleField.setValue(0);
        if (refreshing)
            scaleField.setValue(cd.getColumnScale());
        cd.setColumnScale(scaleField.getValue());
    }

    void setSubtypeVisible(boolean flag) {
        subtypeField.setEnabled(flag);
        if (flag) {
            subtypeField.setValue(1);
        } else subtypeField.setValue(0);
        if (refreshing)
            subtypeField.setValue(cd.getColumnSubtype());
        cd.setColumnSubtype(subtypeField.getValue());
    }

    void setEncodingVisible(boolean flag) {
        encodingBox.setEnabled(flag);
        if (refreshing)
            encodingBox.setSelectedItem(cd.getCharset());
        cd.setCharset((String) encodingBox.getSelectedItem());
    }

    void removeDuplicates() {
        if (types == null)
            types = new HashMap<>();
        else types.clear();
        java.util.List<String> newTypes = new ArrayList<>();
        List<Integer> newIntTypes = new ArrayList<>();
        String last = "";
        for (int i = 0; i < this.dataTypes.length; i++) {
            if (!newTypes.contains(this.dataTypes[i])) {
                newTypes.add(this.dataTypes[i]);
                newIntTypes.add(this.intDataTypes[i]);
                types.put(intDataTypes[i], dataTypes[i]);
                last = dataTypes[i];
            } else {
                types.put(intDataTypes[i], last);
            }
        }
        this.dataTypes = newTypes.toArray(new String[0]);
        this.intDataTypes = newIntTypes.stream().mapToInt(Integer::intValue).toArray();
    }

    void sortTypes() {
        if (dataTypes != null) {
            for (int i = 0; i < dataTypes.length; i++) {
                for (int g = 0; g < dataTypes.length - 1; g++) {
                    int compare = dataTypes[g].compareTo(dataTypes[g + 1]);
                    if (compare > 0) {
                        int temp1 = intDataTypes[g];
                        String temp2 = dataTypes[g];
                        intDataTypes[g] = intDataTypes[g + 1];
                        dataTypes[g] = dataTypes[g + 1];
                        intDataTypes[g + 1] = temp1;
                        dataTypes[g + 1] = temp2;
                    }
                }
            }
        }
    }

    public void refresh() {
        refreshing = true;
        cd.setColumnType(getStringType(cd.getSQLType()));
        typeBox.setSelectedItem(cd.getColumnType());
        refreshType();
        refreshing = false;
    }

    String getStringType(int x) {
        try {
            return types.get(x);
        } catch (Exception e) {
            Log.error(e.getMessage());
            return "";
        }
    }

    private void loadCharsets() {
        try {
            if (charsets == null)
                charsets = new ArrayList<String>();
            else
                charsets.clear();

            String resource = FileUtils.loadResource("org/executequery/charsets.properties");
            String[] strings = resource.split("\n"/*System.getProperty("line.separator")*/);
            for (String s : strings) {
                if (!s.startsWith("#") && !s.isEmpty())
                    charsets.add(s);
            }
            java.util.Collections.sort(charsets);
            charsets.add(0, CreateTableSQLSyntax.NONE);

        } catch (Exception e) {
            Log.error("Error getting charsets for SelectTypePanel:", e);
            return;
        }
    }
}
