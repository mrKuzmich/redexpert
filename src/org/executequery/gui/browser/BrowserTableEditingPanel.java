/*
 * BrowserTableEditingPanel.java
 *
 * Copyright (C) 2002-2017 Takis Diakoumis
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.executequery.gui.browser;

import org.executequery.EventMediator;
import org.executequery.GUIUtilities;
import org.executequery.databasemediators.DatabaseConnection;
import org.executequery.databaseobjects.DatabaseColumn;
import org.executequery.databaseobjects.DatabaseTable;
import org.executequery.databaseobjects.TablePrivilege;
import org.executequery.databaseobjects.impl.ColumnConstraint;
import org.executequery.databaseobjects.impl.DefaultDatabaseIndex;
import org.executequery.event.ApplicationEvent;
import org.executequery.event.DefaultKeywordEvent;
import org.executequery.event.KeywordEvent;
import org.executequery.event.KeywordListener;
import org.executequery.gui.BaseDialog;
import org.executequery.gui.DefaultPanelButton;
import org.executequery.gui.DefaultTable;
import org.executequery.gui.ExecuteQueryDialog;
import org.executequery.gui.databaseobjects.CreateIndexPanel;
import org.executequery.gui.databaseobjects.EditableColumnConstraintTable;
import org.executequery.gui.databaseobjects.EditableDatabaseTable;
import org.executequery.gui.databaseobjects.TableColumnIndexTableModel;
import org.executequery.gui.forms.AbstractFormObjectViewPanel;
import org.executequery.gui.table.EditConstraintPanel;
import org.executequery.gui.table.InsertColumnPanel;
import org.executequery.gui.table.KeyCellRenderer;
import org.executequery.gui.table.TableConstraintFunction;
import org.executequery.gui.text.SimpleSqlTextPanel;
import org.executequery.gui.text.TextEditor;
import org.executequery.localization.Bundles;
import org.executequery.log.Log;
import org.executequery.print.TablePrinter;
import org.underworldlabs.jdbc.DataSourceException;
import org.underworldlabs.swing.*;
import org.underworldlabs.swing.layouts.GridBagHelper;
import org.underworldlabs.swing.table.TableSorter;
import org.underworldlabs.swing.toolbar.PanelToolBar;
import org.underworldlabs.swing.util.SwingWorker;
import org.underworldlabs.util.SystemProperties;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.Printable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * @author Takis Diakoumis
 */
public class BrowserTableEditingPanel extends AbstractFormObjectViewPanel
        implements ActionListener,
        KeywordListener,
        FocusListener,
        TableConstraintFunction,
        ChangeListener,
        VetoableChangeListener {

    // TEXT FUNCTION CONTAINER

    private static final int TABLE_DATA_TAB_INDEX = 5;

    public static final String NAME = "BrowserTableEditingPanel";

    /**
     * Contains the table name
     */
    private DisabledField tableNameField;

    /**
     * Contains the data row count
     */
    private DisabledField rowCountField;

    /**
     * The table view tabbed pane
     */
    private JTabbedPane tabPane;

    /**
     * The SQL text pane for alter text
     */
    private SimpleSqlTextPanel alterSqlText;

    /**
     * The SQL text pane for create table text
     */
    private SimpleSqlTextPanel createSqlText;

    private EditableDatabaseTable descriptionTable;

    /**
     * The panel displaying the table's constraints
     */
    private EditableColumnConstraintTable constraintsTable;

    /**
     * Contains the column indexes for a selected table
     */
    private JTable columnIndexTable;

    /**
     * A reference to the currently selected table.
     * This is not a new <code>JTable</code> instance
     */
    private JTable focusTable;

    private TableColumnIndexTableModel citm;

    /**
     * Holds temporary SQL text during modifications
     */
    private StringBuffer sbTemp;

    /**
     * table data tab panel
     */
    private TableDataTab tableDataPanel;

    /**
     * current node selection object
     */
    private BaseDatabaseObject metaObject;

    /**
     * the erd panel
     */
    private ReferencesDiagramPanel referencesPanel;

    /**
     * table privileges list
     */
    private TablePrivilegeTab tablePrivilegePanel;

    /**
     * the apply changes button
     */
    private JButton applyButton;

    /**
     * the cancel changes button
     */
    private JButton cancelButton;

    /**
     * the button to get count os table rows
     */
    private JButton rowsCountButton;

    /**
     * the browser's control object
     */
    private BrowserController controller;

    private DatabaseObjectMetaDataPanel metaDataPanel;
    DependenciesPanel dependenciesPanel;

    /**
     * the last focused editor
     */
    private TextEditor lastFocusEditor;

    private JPanel buttonsEditingColumnPanel;
    private JPanel buttonsEditingConstraintPanel;
    private JPanel buttonsEditingIndexesPanel;

    Semaphore lock;

    public BrowserTableEditingPanel(BrowserController controller) {
        this.controller = controller;
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<JComponent> externalFileComponents;
    private DisabledField externalFileField;
    private DisabledField adapterField;

    private void init() {
        // the column index table
        //citm = new ColumnIndexTableModel();
        externalFileComponents = new ArrayList<>();
        externalFileField = new DisabledField();
        adapterField = new DisabledField();
        columnIndexTable = new DefaultTable();
        citm = new TableColumnIndexTableModel();
        columnIndexTable.setModel(new TableSorter(citm, columnIndexTable.getTableHeader()));

        columnIndexTable.setColumnSelectionAllowed(false);
        columnIndexTable.getTableHeader().setReorderingAllowed(false);

        //externalFilePanel
        GridBagHelper gbh = new GridBagHelper();

        // column indexes panel
        JPanel indexesPanel = new JPanel(new GridBagLayout());
        indexesPanel.setBorder(BorderFactory.createTitledBorder(bundleString("table-indexes")));
        createButtonsEditingIndexesPanel();
        indexesPanel.add(buttonsEditingIndexesPanel, new GridBagConstraints(
                1, 0, 1, 1, 0, 0,
                GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL,
                new Insets(2, 2, 2, 2), 0, 0));
        indexesPanel.add(new JScrollPane(columnIndexTable), new GridBagConstraints(
                1, 1, 1, 1, 1.0, 1.0,
                GridBagConstraints.SOUTHEAST,
                GridBagConstraints.BOTH,
                new Insets(2, 2, 2, 2), 0, 0));
        columnIndexTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    int row = columnIndexTable.getSelectedRow();
                    if (row >= 0) {
                        row = ((TableSorter) columnIndexTable.getModel()).modelIndex(row);
                        DefaultDatabaseIndex index = ((TableColumnIndexTableModel) ((TableSorter) columnIndexTable.getModel()).getTableModel()).getIndexes().get(row);
                        BaseDialog dialog = new BaseDialog("Edit Index", true);
                        CreateIndexPanel panel = new CreateIndexPanel(table.getHost().getDatabaseConnection(), dialog, index, table.getName());
                        dialog.addDisplayComponent(panel);
                        dialog.display();
                        try {
                            table.reset();
                            setValues(table);

                        } catch (DataSourceException ex) {
                            GUIUtilities.displayExceptionErrorDialog(ex.getMessage(), ex);
                        }
                    }
                }
            }
        });

        // table meta data table
        metaDataPanel = new DatabaseObjectMetaDataPanel();

        // table data panel
        tableDataPanel = new TableDataTab(true);

        // table privileges panel
        tablePrivilegePanel = new TablePrivilegeTab();

        // table references erd panel
        referencesPanel = new ReferencesDiagramPanel();

        // alter sql text panel
        alterSqlText = new SimpleSqlTextPanel();
        alterSqlText.setBorder(BorderFactory.createTitledBorder(bundleString("alter-table")));
        alterSqlText.setPreferredSize(new Dimension(100, 100));
        alterSqlText.getEditorTextComponent().addFocusListener(this);

        // create sql text panel
        createSqlText = new SimpleSqlTextPanel();
        createSqlText.setBorder(BorderFactory.createTitledBorder(bundleString("create-table")));
        createSqlText.getEditorTextComponent().addFocusListener(this);

        // sql text split pane
        FlatSplitPane splitPane = new FlatSplitPane(FlatSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(alterSqlText);
        splitPane.setBottomComponent(createSqlText);
        splitPane.setDividerSize(7);
        splitPane.setResizeWeight(0.25);

        constraintsTable = new EditableColumnConstraintTable();
        JPanel constraintsPanel = new JPanel(new GridBagLayout());
        constraintsPanel.setBorder(BorderFactory.createTitledBorder(bundleString("table-keys")));
        createButtonsEditingConstraintPanel();
        constraintsPanel.add(buttonsEditingConstraintPanel, new GridBagConstraints(
                1, 0, 1, 1, 0, 0,
                GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL,
                new Insets(2, 2, 2, 2), 0, 0));
        constraintsPanel.add(new JScrollPane(constraintsTable), new GridBagConstraints(
                1, 1, 1, 1, 1.0, 1.0,
                GridBagConstraints.SOUTHEAST,
                GridBagConstraints.BOTH,
                new Insets(2, 2, 2, 2), 0, 0));
        constraintsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    int row = constraintsTable.getSelectedRow();
                    if (row >= 0) {
                        row = ((TableSorter) constraintsTable.getModel()).modelIndex(row);
                        ColumnConstraint constraint = constraintsTable.getColumnConstraintTableModel().getConstraints().get(row);
                        BaseDialog dialog = new BaseDialog("Edit Constraint", true);
                        EditConstraintPanel panel = new EditConstraintPanel(table, dialog, constraint);
                        dialog.addDisplayComponent(panel);
                        dialog.display();
                        try {
                            table.reset();
                            setValues(table);
                        } catch (DataSourceException ex) {
                            GUIUtilities.displayExceptionErrorDialog(ex.getMessage(), ex);
                        }
                    }
                }
            }
        });

        descriptionTable = new EditableDatabaseTable();
        createButtonsEditingColumnPanel();
        JPanel descTablePanel = new JPanel(new GridBagLayout());
        descTablePanel.setBorder(BorderFactory.createTitledBorder(bundleString("table-columns")));
        GridBagConstraints gbcDesc = new GridBagConstraints(
                1, 0, 1, 1, 0, 0,
                GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL,
                new Insets(2, 2, 2, 2), 0, 0);
        gbcDesc.gridy++;
        descTablePanel.add(buttonsEditingColumnPanel, gbcDesc);
        descTablePanel.add(
                new JScrollPane(descriptionTable),
                new GridBagConstraints(
                        1, 1, 1, 1, 1.0, 1.0,
                        GridBagConstraints.SOUTHEAST,
                        GridBagConstraints.BOTH,
                        new Insets(2, 2, 2, 2), 0, 0));

        descriptionTable.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    int row = descriptionTable.getSelectedRow();
                    if (row >= 0) {
                        row = ((TableSorter) descriptionTable.getModel()).modelIndex(row);
                        DatabaseColumn column = descriptionTable.getDatabaseTableModel().getDatabaseColumns().get(row);
                        BaseDialog dialog = new BaseDialog("Edit Column", true);
                        InsertColumnPanel panel = new InsertColumnPanel(table, dialog, column);
                        dialog.addDisplayComponent(panel);
                        dialog.display();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {

            }
        });

        tableNameField = new DisabledField();
        //schemaNameField = new DisabledField();
        rowCountField = new DisabledField();

        VetoableSingleSelectionModel model = new VetoableSingleSelectionModel();
        model.addVetoableChangeListener(this);
        dependenciesPanel = new DependenciesPanel();
        //dependenciesPanel.setDatabaseObject(table);

        // create the tabbed pane
        tabPane = new JTabbedPane();
        tabPane.setModel(model);

        tabPane.add(Bundles.getCommon("description"), descTablePanel);
        tabPane.add(Bundles.getCommon("constraints"), constraintsPanel);
        tabPane.add(Bundles.getCommon("indexes"), indexesPanel);
        tabPane.add(Bundles.getCommon("privileges"), tablePrivilegePanel);
        tabPane.add(Bundles.getCommon("references"), referencesPanel);
        tabPane.add(Bundles.getCommon("data"), tableDataPanel);
        tabPane.add(Bundles.getCommon("SQL"), splitPane);
        tabPane.add(Bundles.getCommon("metadata"), metaDataPanel);
        tabPane.add(Bundles.getCommon("dependencies"), dependenciesPanel);
        //dependenciesPanel.load();

        tabPane.addChangeListener(this);

        // apply/cancel buttons
        applyButton = new DefaultPanelButton(Bundles.get("common.apply.button"));
        cancelButton = new DefaultPanelButton(Bundles.get("common.cancel.button"));

        applyButton.addActionListener(this);
        cancelButton.addActionListener(this);

        rowsCountButton = new DefaultPanelButton(Bundles.getCommon("get-rows-count"));
        rowsCountButton.addActionListener(this);

        // add to the base panel
        JPanel base = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        Insets ins = new Insets(10, 5, 0, 5);
        gbc.insets = ins;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbh.setDefaults(gbc).defaults();
        gbh.topGap(5);
        gbh.addLabelFieldPair(base, bundleString("table-name"), tableNameField, null);
        JLabel label = new JLabel(bundleString("external-file"));
        externalFileComponents.add(label);
        externalFileComponents.add(externalFileField);
        gbh.addLabelFieldPair(base, label, externalFileField, null);
        label = new JLabel(bundleString("adapter"));
        externalFileComponents.add(label);
        externalFileComponents.add(adapterField);
        gbh.addLabelFieldPair(base, label, adapterField, null);
        base.add(tabPane, gbh.nextRowFirstCol().fillBoth().spanY().get());
        /*base.add()
        // add the bottom components
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weighty = 0;
        gbc.weightx = 0;
        gbc.insets.top = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        base.add(rowsCountButton, gbc);
        gbc.gridx = 2;
        gbc.insets.top = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        gbc.insets.right = 0;
        base.add(rowCountField, gbc);
        gbc.gridx = 3;
        gbc.weightx = 0;
        gbc.insets.top = 0;
        gbc.insets.right = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        base.add(applyButton, gbc);
        gbc.gridx = 4;
        gbc.weightx = 0;
        gbc.insets.left = 0;
        base.add(cancelButton, gbc);*/

        // set up and add the focus listener for the tables
        FocusListener tableFocusListener = new FocusListener() {
            public void focusLost(FocusEvent e) {
            }

            public void focusGained(FocusEvent e) {
                focusTable = (JTable) e.getSource();
            }
        };
        descriptionTable.addFocusListener(tableFocusListener);
        //columnDataTable.addTableFocusListener(tableFocusListener);
        constraintsTable.addFocusListener(tableFocusListener);

        sbTemp = new StringBuffer(100);

        setContentPanel(base);
        setHeaderIcon(GUIUtilities.loadIcon("DatabaseTable24.png"));
        setHeaderText(bundleString("db-table"));

        // register for keyword changes
        EventMediator.registerListener(this);

        lock = new Semaphore(1);
        for (JComponent component : externalFileComponents)
            component.setVisible(false);
    }

    /**
     * Invoked when a SQL text pane gains the keyboard focus.
     */
    public void focusGained(FocusEvent e) {
        Object source = e.getSource();
        // check which editor we are in
        if (createSqlText.getEditorTextComponent() == source) {
            lastFocusEditor = createSqlText;
        } else if (alterSqlText.getEditorTextComponent() == source) {
            lastFocusEditor = alterSqlText;
        }
    }

    /**
     * Invoked when a SQL text pane loses the keyboard focus.
     * Does nothing here.
     */
    public void focusLost(FocusEvent e) {
        if (e.getSource() == alterSqlText) {
            table.setModifiedSQLText(alterSqlText.getSQLText());
        }
    }

    /**
     * Notification of a new keyword added to the list.
     */
    public void keywordsAdded(KeywordEvent e) {
        alterSqlText.setSQLKeywords(true);
        createSqlText.setSQLKeywords(true);
    }

    /**
     * Notification of a keyword removed from the list.
     */
    public void keywordsRemoved(KeywordEvent e) {
        alterSqlText.setSQLKeywords(true);
        createSqlText.setSQLKeywords(true);
    }

    public boolean canHandleEvent(ApplicationEvent event) {
        return (event instanceof DefaultKeywordEvent);
    }

    /**
     * Performs the apply/cancel changes.
     *
     * @param event - the event
     */
    public void actionPerformed(ActionEvent event) {

        if (table.isAltered()) {

            Object source = event.getSource();
            if (source == applyButton) {

                try {
                    if (tabPane.getSelectedComponent() == tableDataPanel)
                        tableDataPanel.stopEditing();
                    DatabaseObjectChangeProvider docp = new DatabaseObjectChangeProvider(table);
                    docp.applyChanges();
                    if (docp.applied)
                        setValues(table);

                } catch (DataSourceException e) {

                    GUIUtilities.displayExceptionErrorDialog(e.getMessage(), e);
                }

            } else if (source == cancelButton) {

                table.revert();
                setValues(table);
            }

        }

        if (event.getSource() == rowsCountButton) {

            table.resetRowsCount();
            updateRowCount(bundleString("quering"));
            reloadDataRowCount();
        }
    }

    public String getLayoutName() {
        return NAME;
    }

    public void refresh() {
    }

    public Printable getPrintable() {

        int tabIndex = tabPane.getSelectedIndex();

        switch (tabIndex) {

            case 0:
                return new TablePrinter(descriptionTable,
                        bundleString("description-table") + table.getName());

            case 1:
                return new TablePrinter(constraintsTable,
                        bundleString("constraints-table") + table.getName(), false);

            case 2:
                return new TablePrinter(columnIndexTable,
                        bundleString("indexes-table") + table.getName());

            case 3:
                return new TablePrinter(tablePrivilegePanel.getTable(),
                        bundleString("privileges-table") + table.getName());

            case 4:
                return referencesPanel.getPrintable();

            case 5:
                return new TablePrinter(tableDataPanel.getTable(),
                        bundleString("data-table") + table.getName());

            case 7:
                return new TablePrinter(metaDataPanel.getTable(),
                        bundleString("metadata-table") + table.getName());

            default:
                return null;

        }

    }

    public void cleanup() {

        if (worker != null) {

            worker.interrupt();
            worker = null;
        }

        tableDataPanel.closeResultSet();
        referencesPanel.cleanup();
        EventMediator.deregisterListener(this);
    }

    public void vetoableChange(PropertyChangeEvent e) throws PropertyVetoException {

        if (Integer.valueOf(e.getOldValue().toString()) == TABLE_DATA_TAB_INDEX) {

            if (tableDataPanel.hasChanges()) {

                boolean applyChanges = new DatabaseObjectChangeProvider(table).applyChanges(true);
                if (!applyChanges) {

                    throw new PropertyVetoException("User cancelled", e);
                }
            }

        }


    }

    /**
     * Handles a change tab selection.
     */
    public void stateChanged(ChangeEvent e) {

        final int index = tabPane.getSelectedIndex();
        if (index != TABLE_DATA_TAB_INDEX && tableDataPanel.isExecuting()) {

            tableDataPanel.cancelStatement();
            return;
        }

//        GUIUtils.startWorker(new Runnable() {
//
//            public void run() {

                tabIndexSelected(index);
//            }
//
//        });

    }

    private void tabIndexSelected(int index) {

        switch (index) {
            case 2:
                loadIndexes();
                break;
            case 3:
                loadPrivileges();
                break;
            case 4:
                loadReferences();
                break;
            case 5:
                tableDataPanel.loadDataForTable(table);
                break;
            case 6:
                try {
                    // check for any table defn changes
                    // and update the alter text pane
                    if (table.isAltered()) {

                        alterSqlText.setSQLText(table.getAlteredSQLText().trim());

                    } else {

                        alterSqlText.setSQLText(EMPTY);
                    }

                } catch (DataSourceException exc) {

                    controller.handleException(exc);
                    alterSqlText.setSQLText(EMPTY);
                }

                break;
            case 7:
                loadTableMetaData();
                break;
        }

    }

    /**
     * Indicates that the references tab has been previously displayed
     * for the current selection. Aims to keep any changes made to the
     * references ERD (moving tables around) stays as was previosuly set.
     */
    private boolean referencesLoaded;

    /**
     * Loads database table references.
     */
    private void loadReferences() {

        // TODO: to be refactored out when erd receives new impl

        if (referencesLoaded) {
            return;
        }

        try {

            GUIUtilities.showWaitCursor();

            List<ColumnData[]> columns = new ArrayList<ColumnData[]>();
            List<String> tableNames = new ArrayList<String>();

            List<ColumnConstraint> constraints = table.getConstraints();
            Set<String> tableNamesAdded = new HashSet<String>();

            for (ColumnConstraint constraint : constraints) {

                String tableName = constraint.getTableName();
                if (constraint.isPrimaryKey()) {

                    if (!tableNamesAdded.contains(tableName)) {

                        tableNames.add(tableName);
                        tableNamesAdded.add(tableName);
                        columns.add(controller.getColumnData(constraint.getCatalogName(),
                                constraint.getSchemaName(),
                                tableName,table.getHost().getDatabaseConnection()));
                    }

                } else if (constraint.isForeignKey()) {

                    String referencedTable = constraint.getReferencedTable();
                    if (!tableNamesAdded.contains(referencedTable)) {

                        tableNames.add(referencedTable);
                        tableNamesAdded.add(referencedTable);
                        columns.add(controller.getColumnData(constraint.getReferencedCatalog(),
                                constraint.getReferencedSchema(),
                                referencedTable,table.getHost().getDatabaseConnection()));
                    }

                    String columnName = constraint.getColumnName();
                    if (!tableNames.contains(tableName) && !columns.contains(columnName)) {

                        tableNames.add(tableName);
                        tableNamesAdded.add(tableName);
                        columns.add(controller.getColumnData(constraint.getCatalogName(),
                                constraint.getSchemaName(),
                                tableName,table.getHost().getDatabaseConnection()));
                    }


                }

            }

            List<DatabaseColumn> exportedKeys = table.getExportedKeys();
            for (DatabaseColumn column : exportedKeys) {

                String parentsName = column.getParentsName();
                if (!tableNamesAdded.contains(parentsName)) {

                    tableNames.add(parentsName);
                    tableNamesAdded.add(parentsName);
                    columns.add(controller.getColumnData(column.getCatalogName(),
                            column.getSchemaName(),
                            parentsName,table.getHost().getDatabaseConnection()));
                }

            }

            if (tableNames.isEmpty()) {

                tableNames.add(table.getName());
                columns.add(new ColumnData[0]);
            }

            referencesPanel.setTables(tableNames, columns);

        } catch (DataSourceException e) {

            controller.handleException(e);
        } finally {

            GUIUtilities.showNormalCursor();
        }

        referencesLoaded = true;
    }

    /**
     * Loads database table indexes.
     */
    private void loadTableMetaData() {

        try {

            metaDataPanel.setData(table.getColumnMetaData());

        } catch (DataSourceException e) {

            controller.handleException(e);
            metaDataPanel.setData(null);
        }
    }

    /**
     * Loads database table indexes.
     */
    private void loadIndexes() {
        try {
            // reset the data
            citm.setIndexData(table.getIndexes());

            // reset the view properties
            columnIndexTable.setCellSelectionEnabled(true);
            TableColumnModel tcm = columnIndexTable.getColumnModel();
            tcm.getColumn(0).setPreferredWidth(25);
            tcm.getColumn(0).setMaxWidth(25);
            tcm.getColumn(0).setMinWidth(25);
            tcm.getColumn(0).setCellRenderer(new KeyCellRenderer());
            tcm.getColumn(1).setPreferredWidth(130);
            tcm.getColumn(2).setPreferredWidth(130);
            tcm.getColumn(3).setPreferredWidth(130);
            tcm.getColumn(4).setPreferredWidth(90);

        } catch (DataSourceException e) {
            controller.handleException(e);
            citm.setIndexData(null);
        }

    }

    /**
     * Loads database table privileges.
     */
    private void loadPrivileges() {

        try {

            tablePrivilegePanel.setValues(table.getPrivileges());

        } catch (DataSourceException e) {

            controller.handleException(e);
            tablePrivilegePanel.setValues(new TablePrivilege[0]);
        }

    }

    private DatabaseTable table;

    public void setValues(DatabaseTable table) {

        this.table = table;
        if (table.getExternalFile() != null) {
            externalFileField.setText(table.getExternalFile());
            if (table.getAdapter() != null)
                adapterField.setText(table.getAdapter());
            for (JComponent component : externalFileComponents)
                component.setVisible(true);

        }

        reloadView();
        if (SystemProperties.getBooleanProperty("user", "browser.query.row.count")) {

            reloadDataRowCount();
        }

        stateChanged(null);
    }

    protected void reloadView() {

        try {

            if (SystemProperties.getBooleanProperty("user", "browser.query.row.count")) {

                updateRowCount(bundleString("quering"));

            } else {

                updateRowCount(bundleString("option-disabled"));
            }

            referencesLoaded = false;

            tableNameField.setText(table.getName());
            descriptionTable.setDatabaseTable(table);
            constraintsTable.setDatabaseTable(table);
            loadIndexes();
            dependenciesPanel.setDatabaseObject(table);

            alterSqlText.setSQLText(EMPTY);
            createSqlText.setSQLText("Loading data");
            SwingWorker sw = new SwingWorker() {
                @Override
                public Object construct() {
                    try {

                        createSqlText.setSQLText(createTableStatementFormatted());

                    } catch (Exception e) { // some liquibase generated issues... ??

                        String message = "Error generating SQL for table - " + e.getMessage();
                        createSqlText.setSQLText(message);
                        Log.error("Error generating SQL for table - " + e.getMessage(), e);
                    }
                    return null;
                }
            };
            sw.start();


        } catch (DataSourceException e) {

            ///controller.handleException(e);
            Log.error("Error load table:", e);

            descriptionTable.resetDatabaseTable();
            constraintsTable.resetConstraintsTable();
        }

    }

    private String createTableStatementFormatted() {

        return table.getCreateSQLText();
    }

    private boolean loadingRowCount;
    private SwingWorker worker;
    private Timer timer;

    private void reloadDataRowCount() {

        if (timer != null) {

            timer.cancel();
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {

                updateDataRowCount();
            }
        }, 600);

    }

    private void updateDataRowCount() {

        if (worker != null) {

            if (loadingRowCount) {

                Log.debug("Interrupting worker for data row count");
            }
            worker.interrupt();
        }

        worker = new SwingWorker() {
            public Object construct() {
                try {

                    loadingRowCount = true;
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Log.error("Error load data row count:", e);
                    }

                    Log.debug("Retrieving data row count for table - " + table.getName());
                    return String.valueOf(table.getDataRowCount());

                } catch (DataSourceException e) {

                    return "Error: " + e.getMessage();

                } finally {

                    loadingRowCount = false;
                }
            }

            public void finished() {

                updateRowCount(get().toString());
            }
        };
        worker.start();
    }

    /**
     * Fires a change in the selected table node and reloads
     * the current table display view.
     *
     * @param metaObject - the selected meta object
     * @param reset      - whether to reset the view, regardless of the current
     *                   view which may be the same metaObject
     */
    public void selectionChanged(BaseDatabaseObject metaObject, boolean reset) {

        if (this.metaObject == metaObject && !reset) {

            return;
        }

        this.metaObject = metaObject;
    }

    /**
     * Fires a change in the selected table node and reloads
     * the current table display view.
     *
     * @param metaObject - the selected meta object
     */
    public void selectionChanged(BaseDatabaseObject metaObject) {
        selectionChanged(metaObject, false);
    }

    public TableDataTab getTableDataPanel() {
        return tableDataPanel;
    }

    public void setBrowserPreferences() {
        tableDataPanel.setTableProperties();
    }

    public JTable getTableInFocus() {
        return focusTable;
    }

    /**
     * Returns whether the SQL panel has any text in it.
     */
    public boolean hasSQLText() {
        return !(alterSqlText.isEmpty());
    }

    /**
     * Resets the contents of the SQL panel to nothing.
     */
    public void resetSQLText() {
        alterSqlText.setSQLText(EMPTY);
    }

    /**
     * Returns the contents of the SQL text pane.
     */
    public String getSQLText() {
        return alterSqlText.getSQLText();
    }

    // -----------------------------------------------
    // --- TableConstraintFunction implementations ---
    // -----------------------------------------------

    /**
     * Deletes the selected row on the currently selected table.
     */
    public void deleteRow() {

        int tabIndex = tabPane.getSelectedIndex();

        if (tabIndex == 0) {

            descriptionTable.deleteSelectedColumn();

        } else if (tabIndex == 1) {

            constraintsTable.deleteSelectedConstraint();
        } else if (tabIndex == 2) {
            int row = columnIndexTable.getSelectedRow();
            if (row >= 0) {
                row = ((TableSorter) columnIndexTable.getModel()).modelIndex(row);
                DefaultDatabaseIndex index = ((TableColumnIndexTableModel) ((TableSorter) columnIndexTable.getModel()).getTableModel()).getIndexes().get(row);
                String query = "DROP INDEX " + index.getName();
                ExecuteQueryDialog eqd = new ExecuteQueryDialog("Dropping object", query, table.getHost().getDatabaseConnection(), true);
                eqd.display();
                if (eqd.getCommit())
                    table.reset();
                setValues(table);
            }
        }

        setSQLText();
    }

    /**
     * Inserts a row after the selected row on the currently selected table.
     */
    public void insertAfter() {

        int tabIndex = tabPane.getSelectedIndex();
        JPanel panelForDialog = null;
        BaseDialog dialog = null;
        if (tabIndex == 0) {
            dialog = new BaseDialog("Add column", true);
            panelForDialog = new InsertColumnPanel(table, dialog);
        } else if (tabIndex == 1) {
            dialog = new BaseDialog("Add constraint", true);
            panelForDialog = new EditConstraintPanel(table, dialog);
        } else if (tabIndex == 2) {
            dialog = new BaseDialog("Add Index", true);
            panelForDialog = new CreateIndexPanel(table.getHost().getDatabaseConnection(), dialog, table.getName());

        }

        dialog.addDisplayComponent(panelForDialog);
        dialog.display();
        table.reset();
        reloadView();

    }

    public void setSQLText() {

        sbTemp.setLength(0);
        alterSqlText.setSQLText("");
    }

    public void setSQLText(String values, int type) {

        sbTemp.setLength(0);
        
        /*
        if (type == TableModifier.COLUMN_VALUES) {
            sbTemp.append(values).
                   append(conPanel.getSQLText());
        }
        /*
        else if (type == TableModifier.CONSTRAINT_VALUES) {
            sbTemp.append(columnDataTable.getSQLText()).
                   append(values);
        }
        */
        alterSqlText.setSQLText(sbTemp.toString());
    }

    public String getTableName() {
        return tableNameField.getText();
    }

    public Vector<String> getHostedSchemasVector() {
        return controller.getHostedSchemas();
    }

    public Vector<String> getSchemaTables(String schemaName) {
        return controller.getTables(schemaName);
    }

    public Vector<String> getColumnNamesVector(String tableName, String schemaName) {
        return controller.getColumnNamesVector(tableName, schemaName);
    }

    public void insertBefore() {
    }

    public void moveColumnUp() {
    }

    public void moveColumnDown() {
    }

    public ColumnData[] getTableColumnData() {
        return null;
        //return columnDataTable.getTableColumnData();
    }

    public Vector<ColumnData> getTableColumnDataVector() {
        return null;
        //return columnDataTable.getTableColumnDataVector();
    }

    @Override
    public DatabaseConnection getSelectedConnection() {
        return table.getHost().getDatabaseConnection();
    }

    /**
     * Tab index of the SQL text panes
     */
    private static final int SQL_PANE_INDEX = 6;

    /**
     * Returns the focused TextEditor panel where the selected
     * tab is the SQL text pane.
     */
    protected TextEditor getFocusedTextEditor() {
        if (tabPane.getSelectedIndex() == SQL_PANE_INDEX) {
            if (lastFocusEditor != null) {
                return lastFocusEditor;
            }
        }
        return null;
    }

    private void updateRowCount(final String text) {
        if(lock.tryAcquire()) {
            GUIUtils.invokeLater(new Runnable() {
                public void run() {
                    synchronized (rowCountField) {
                        rowCountField.setText(text);
                        lock.release();
                    }
                }
            });
        }
    }

    private void createButtonsEditingColumnPanel() {
        buttonsEditingColumnPanel = new JPanel(new GridBagLayout());
        PanelToolBar bar = new PanelToolBar();
        RolloverButton addRolloverButton = new RolloverButton();
        addRolloverButton.setIcon(GUIUtilities.loadIcon("ColumnInsert16.png"));
        addRolloverButton.setToolTipText("Insert column");
        addRolloverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                insertAfter();
            }
        });
        bar.add(addRolloverButton);
        RolloverButton deleteRolloverButton = new RolloverButton();
        deleteRolloverButton.setIcon(GUIUtilities.loadIcon("ColumnDelete16.png"));
        deleteRolloverButton.setToolTipText("Delete column");
        deleteRolloverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteRow();
            }
        });
        bar.add(deleteRolloverButton);
        RolloverButton commitRolloverButton = new RolloverButton();
        commitRolloverButton.setIcon(GUIUtilities.loadIcon("Commit16.png"));
        commitRolloverButton.setToolTipText("Commit");
        commitRolloverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {


                try {
                    DatabaseObjectChangeProvider docp = new DatabaseObjectChangeProvider(table);
                    if (docp.applyDefinitionChanges())
                        setValues(table);

                } catch (DataSourceException e) {
                    GUIUtilities.displayExceptionErrorDialog(e.getMessage(), e);
                }
            }
        });
        bar.add(commitRolloverButton);
        RolloverButton rollbackRolloverButton = new RolloverButton();
        rollbackRolloverButton.setIcon(GUIUtilities.loadIcon("Rollback16.png"));
        rollbackRolloverButton.setToolTipText("Rollback");
        rollbackRolloverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                table.revert();
                setValues(table);
            }
        });
        bar.add(rollbackRolloverButton);
        GridBagConstraints gbc3 = new GridBagConstraints(4, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
        buttonsEditingColumnPanel.add(bar, gbc3);
    }

    private void createButtonsEditingConstraintPanel() {
        buttonsEditingConstraintPanel = new JPanel(new GridBagLayout());
        PanelToolBar bar = new PanelToolBar();
        RolloverButton addRolloverButton = new RolloverButton();
        addRolloverButton.setIcon(GUIUtilities.loadIcon("ColumnInsert16.png"));
        addRolloverButton.setToolTipText("Insert constraint");
        addRolloverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                insertAfter();
            }
        });
        bar.add(addRolloverButton);
        RolloverButton deleteRolloverButton = new RolloverButton();
        deleteRolloverButton.setIcon(GUIUtilities.loadIcon("ColumnDelete16.png"));
        deleteRolloverButton.setToolTipText("Delete column");
        deleteRolloverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteRow();
            }
        });
        bar.add(deleteRolloverButton);
        RolloverButton commitRolloverButton = new RolloverButton();
        commitRolloverButton.setIcon(GUIUtilities.loadIcon("Commit16.png"));
        commitRolloverButton.setToolTipText("Commit");
        commitRolloverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {


                try {
                    DatabaseObjectChangeProvider docp = new DatabaseObjectChangeProvider(table);
                    if (docp.applyDefinitionChanges())
                        setValues(table);

                } catch (DataSourceException e) {
                    GUIUtilities.displayExceptionErrorDialog(e.getMessage(), e);
                }
            }
        });
        bar.add(commitRolloverButton);
        RolloverButton rollbackRolloverButton = new RolloverButton();
        rollbackRolloverButton.setIcon(GUIUtilities.loadIcon("Rollback16.png"));
        rollbackRolloverButton.setToolTipText("Rollback");
        rollbackRolloverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                table.revert();
                setValues(table);
            }
        });
        bar.add(rollbackRolloverButton);
        GridBagConstraints gbc3 = new GridBagConstraints(4, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
        buttonsEditingConstraintPanel.add(bar, gbc3);
    }

    private void createButtonsEditingIndexesPanel() {
        buttonsEditingIndexesPanel = new JPanel(new GridBagLayout());
        PanelToolBar bar = new PanelToolBar();
        RolloverButton addRolloverButton = new RolloverButton();
        addRolloverButton.setIcon(GUIUtilities.loadIcon("ColumnInsert16.png"));
        addRolloverButton.setToolTipText("Insert Index");
        addRolloverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                insertAfter();
            }
        });
        bar.add(addRolloverButton);
        RolloverButton deleteRolloverButton = new RolloverButton();
        deleteRolloverButton.setIcon(GUIUtilities.loadIcon("ColumnDelete16.png"));
        deleteRolloverButton.setToolTipText("Delete Index");
        deleteRolloverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                deleteRow();
            }
        });
        bar.add(deleteRolloverButton);
        GridBagConstraints gbc3 = new GridBagConstraints(4, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
        buttonsEditingIndexesPanel.add(bar, gbc3);
    }

}


