/*******************************************************************************
 *          AppLauncher project ( http://www.jmmc.fr/applauncher )
 *******************************************************************************
 * Copyright (c) 2014, CNRS. All rights reserved.
 *
 * This file is part of AppLauncher.
 *
 * AppLauncher is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the
 * Free Software Foundation, version 3.
 *
 * AppLauncher is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * AppLauncher. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.jmmc.smprsc.data.list;

import com.jidesoft.swing.CheckBoxTree;
import com.jidesoft.swing.CheckBoxTreeSelectionModel;
import fr.jmmc.jmcs.data.MimeType;
import fr.jmmc.jmcs.gui.PreferencesView;
import fr.jmmc.jmcs.network.interop.SampMetaData;
import fr.jmmc.jmcs.service.BrowserLauncher;
import fr.jmmc.jmcs.util.CollectionUtils;
import fr.jmmc.jmcs.util.ImageUtils;
import fr.jmmc.smprsc.data.list.model.Category;
import fr.jmmc.smprsc.data.stub.StubMetaData;
import fr.jmmc.smprsc.data.stub.model.Metadata;
import fr.jmmc.smprsc.data.stub.model.SampStub;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sylvain LAFRASSE.
 */
public class ApplicationListSelectionPanel extends JPanel {

    /** Logger - get from given class name */
    private static final Logger _logger = LoggerFactory.getLogger(ApplicationListSelectionPanel.class.getName());
    // CONSTANTS
    private static final int PANEL_HEIGHT = PreferencesView.FRAME_HEIGHT;
    // Cached application data
    private final HashMap<String, ImageIcon> _cachedApplicationIcons = new HashMap<String, ImageIcon>();
    private final HashMap<String, String> _cachedApplicationDescriptions = new HashMap<String, String>();
    private final HashMap<String, Boolean> _cachedBetaCheckBoxStates = new HashMap<String, Boolean>();
    private final HashMap<String, String> _cachedApplicationCliPathes = new HashMap<String, String>();
    private String _currentlySelectedApplicationName = null;
    private boolean _programaticCheckingUnderway = true;
    // Tree stuff
    private static final int TREE_WIDTH = 200;
    private DefaultMutableTreeNode _treeDataModel;
    private static final List<String> ALL = null;
    private static final String ROOT_NODE_NAME = "Root";
    private CheckBoxTree _checkBoxTree;
    private static final int ICON_SIZE = 16;
    // Description stuff
    private static final int EDITOR_PANE_WIDTH = PreferencesView.FRAME_WIDTH - TREE_WIDTH;
    private JEditorPane _descriptionEditorPane;
    protected JScrollPane _descriptionScrollPane;
    private JCheckBox _betaCheckBox;
    private JTextField _cliPathTextField;
    private JLabel _cliPathLabel;

    public ApplicationListSelectionPanel() {
        super();
    }

    protected void init() {
        setLayout(new BorderLayout());

        // Setup tree pane
        _treeDataModel = populateTreeDataModel();
        _checkBoxTree = setupCheckBoxTree();
        setCheckedApplicationNames(ALL);
        add(new JScrollPane(_checkBoxTree), BorderLayout.WEST);

        // Setup description pane
        _descriptionEditorPane = setupDescriptionEditorPane();
        _descriptionScrollPane = setupDescriptionScrollPane();

        _betaCheckBox = setupBetaCheckBox();

        _cliPathTextField = setupCliPathTextField();
        JPanel cliPanel = new JPanel();
        cliPanel.setOpaque(false);
        cliPanel.add(_cliPathLabel);
        cliPanel.add(_cliPathTextField);

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setOpaque(false);
        descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.PAGE_AXIS));
        descriptionPanel.add(_descriptionScrollPane);
        descriptionPanel.add(cliPanel);
        descriptionPanel.add(_betaCheckBox);
        add(descriptionPanel, BorderLayout.CENTER);

        changeBetaCheckBoxState(null);
        changeCliPathTextFieldValue(null);
    }

    private DefaultMutableTreeNode populateTreeDataModel() {

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(ROOT_NODE_NAME);

        // For each known application category
        for (Category applicationCategory : Category.values()) {

            final String categoryName = applicationCategory.value();
            DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(categoryName);
            rootNode.add(categoryNode);
            _logger.trace("Loading applications for category '{}' :", categoryName);

            // Gets all category's visible application names
            for (String applicationName : StubRegistry.getCategoryVisibleApplicationNames(applicationCategory)) {

                // Add the application node only if it is a visible one
                ImageIcon applicationIcon = StubMetaData.getEmbeddedApplicationIcon(applicationName);
                // Load application icons once and for all
                ImageIcon resizedApplicationIcon = ImageUtils.getScaledImageIcon(applicationIcon, ICON_SIZE, ICON_SIZE);
                _cachedApplicationIcons.put(applicationName, resizedApplicationIcon);

                // Load application's SAMP meta data from JAR
                cacheApplicationMetaData(applicationName);

                // Create application node
                DefaultMutableTreeNode applicationNode = new DefaultMutableTreeNode(applicationName);
                categoryNode.add(applicationNode);
                _logger.trace("\t- found application '{}' with icon.", applicationName);
            }
        }

        return rootNode;
    }

    private void cacheApplicationMetaData(String applicationName) {

        // Load application's SAMP meta data from JAR
        _logger.trace("Loading '{}' application meta data :", applicationName);
        SampStub applicationData = StubMetaData.retrieveSampStubForApplication(applicationName);
        HashMap<String, String> metaDataMap = new HashMap<String, String>();
        for (Metadata applicationMetaData : applicationData.getMetadatas()) {
            final String metaDataKey = applicationMetaData.getKey();
            final String metaDataValue = applicationMetaData.getValue();
            metaDataMap.put(metaDataKey, metaDataValue);
            _logger.trace("\t- found meta data ['{}' -> '{}'].", metaDataKey, metaDataValue);
        }

        // HTML generation
        _logger.debug("Generating HTML description for '{}' application meta data :", applicationName);
        final StringBuilder generatedHtml = new StringBuilder(4096);
        generatedHtml.append("<HTML><HEAD></HEAD><BODY>");
        for (SampMetaData metaData : SampMetaData.values()) {

            // Label
            final String label = metaData.getLabel();
            if (label == null) {
                continue;
            }

            // Value
            String value = metaDataMap.get(metaData.id());
            if (value != null) {
                generatedHtml.append("<B>").append(label).append(":</B> ");

                final MimeType mimeType = metaData.mimeType();
                if (mimeType == MimeType.URL) {
                    generatedHtml.append("<A HREF='").append(value).append("'>").append(value).append("</A>");
                } else {
                    generatedHtml.append(value);
                }

                generatedHtml.append("<BR><BR>");
                _logger.trace("\t- found meta data for '{}' = '{}'.", label, value);
            }
        }
        generatedHtml.append("</BODY></HTML>");

        // Cache application description for later retrieve
        final String applicationDescription = generatedHtml.toString();
        _cachedApplicationDescriptions.put(applicationName, applicationDescription);

        // Retrieve and cache whether the current application has a beta JNLP URL or not
        final String betaJnlpUrl = metaDataMap.get(SampMetaData.JNLP_BETA_URL.id());
        if (betaJnlpUrl != null) {
            // If so get its prefered state
            final boolean betaIsEnabled = isApplicationBetaJnlpUrlInUse(applicationName);
            _cachedBetaCheckBoxStates.put(applicationName, betaIsEnabled);
            _logger.trace("\t- found beta JNLP URL, retrieveing its saved checkbox state '{}'.", betaIsEnabled);
        } else {
            _logger.trace("\t- not found beta JNLP URL, leaving checkbox disabled.");
        }

        // Retrieve and cache whether the current application has a CLI path or not
        final String defaultCliPath = metaDataMap.get(SampMetaData.CLI_PATH.id());
        if (defaultCliPath != null) {
            // If the user already specified a prefered path
            final String preferedCliPath = applicationCliPath(applicationName);
            if ((preferedCliPath != null) && (preferedCliPath.length() > 0)) {
                // Use his prefered value
                _cachedApplicationCliPathes.put(applicationName, preferedCliPath);
            } else { // Otherwise use default value as last resorts
                _cachedApplicationCliPathes.put(applicationName, defaultCliPath);
            }
        } else {
            _logger.trace("\t- not found application CLI path, leaving textfield disabled.");
        }

        final String cachedCliPath = _cachedApplicationCliPathes.get(applicationName);
        _logger.trace("\t- found application CLI path, retrieveing its saved value '{}'.", cachedCliPath);
    }

    private CheckBoxTree setupCheckBoxTree() {

        CheckBoxTree checkBoxTree = new CheckBoxTree(_treeDataModel) {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return new Dimension(TREE_WIDTH, PANEL_HEIGHT);
            }
        };

        checkBoxTree.setRootVisible(false); // Hide root node
        checkBoxTree.setShowsRootHandles(true); // Node handles should be displayed

        // Restrict row selection to one item at a time
        DefaultTreeSelectionModel treeSelectionModel = new DefaultTreeSelectionModel();
        treeSelectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        checkBoxTree.setSelectionModel(treeSelectionModel);
        checkBoxTree.getCheckBoxTreeSelectionModel().setSingleEventMode(true);

        checkBoxTree.setDigIn(true); // If a category is clicked, all its applications are also checked
        checkBoxTree.setClickInCheckBoxOnly(true); // Allow selection of items whithout setting them at the same time
        checkBoxTree.setCellRenderer(new ApplicationIconRenderer());

        listenToSelections(checkBoxTree);
        listenToChecks(checkBoxTree);

        return checkBoxTree;
    }

    private void listenToSelections(final CheckBoxTree tree) {

        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                _currentlySelectedApplicationName = null; // Reset selection memory
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

                // if nothing is selected
                if (node == null) {

                    _logger.trace("Nothing selected.");

                    // Clear information pane
                    fillApplicationDescriptionPane(null);

                } else if (!node.isLeaf()) { // i.e a category is selected

                    _logger.trace("Category selected.");

                    // Clear information pane
                    fillApplicationDescriptionPane(null);

                } else { // i.e an application is selected

                    // Retrieve the selected application name
                    _currentlySelectedApplicationName = node.getUserObject().toString();

                    _logger.debug("Application '{}' is selected.", _currentlySelectedApplicationName);

                    // Fill information pane accordinaly
                    fillApplicationDescriptionPane(_currentlySelectedApplicationName);
                }
            }
        });
    }

    private void listenToChecks(final CheckBoxTree tree) {

        tree.getCheckBoxTreeSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {

                if (_programaticCheckingUnderway) {
                    _logger.trace("Skipping programatically checked applications.");
                    return;
                }

                // Get the currently checked paths list
                TreePath[] checkedPaths = tree.getCheckBoxTreeSelectionModel().getSelectionPaths();
                if (checkedPaths == null) {
                    _logger.debug("Discovered that NOTHING is checked - ignoring event.");
                    return;
                }

                // Retrieve application names for each checked box
                List<String> checkedApplicationList = new ArrayList<String>();
                for (TreePath checkedPath : checkedPaths) {

                    final Object checkedBox = checkedPath.getLastPathComponent();
                    final String checkedBoxName = checkedBox.toString();
                    _logger.debug("Discovered that '{}' is checked.", checkedBoxName);

                    // If the current checked box is a leaf
                    DefaultTreeModel treeModel = new DefaultTreeModel(_treeDataModel);
                    if (treeModel.isLeaf(checkedBox)) {
                        // Simply add it to the list
                        checkedApplicationList.add(checkedBoxName);
                        continue;
                    }

                    _logger.trace("But '{}' is NOT a LEAF - Going deeper :", checkedBoxName);

                    // Get all the applications in the current 'directory' node
                    for (Category category : Category.values()) {

                        // List category applications (or all applications if ROOT node)
                        if ((checkedBoxName.equals(ROOT_NODE_NAME)) || (checkedBoxName.equals(category.value()))) {

                            // Retrieve all the current category visible applications
                            for (String applicationName : StubRegistry.getCategoryVisibleApplicationNames(category)) {

                                _logger.trace("\t- made of visible application = '{}'.", applicationName);
                                checkedApplicationList.add(applicationName);
                            }
                        }
                    }
                }

                _logger.debug("Notifying manually checked applications : {}", checkedApplicationList);
                checkedApplicationChanged(checkedApplicationList);
            }
        });
    }

    private class ApplicationIconRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            // Leave category icons alone
            if (leaf) {

                // Get application's name
                final String applicationName = value.toString();

                // Get application's icon
                final ImageIcon icon = _cachedApplicationIcons.get(applicationName);

                if (icon != null) {
                    setIcon(icon);
                }

                _logger.trace("Rendered '{}' application icon.", applicationName);
            }

            return this;
        }
    }

    private JEditorPane setupDescriptionEditorPane() {

        final JEditorPane descriptionEditorPane = new JEditorPane();

        descriptionEditorPane.setPreferredSize(new Dimension(EDITOR_PANE_WIDTH, PANEL_HEIGHT));
        descriptionEditorPane.setEditable(false);
        descriptionEditorPane.setMargin(new Insets(5, 5, 5, 5));
        descriptionEditorPane.setContentType("text/html");

        descriptionEditorPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent event) {
                // When a link is clicked
                if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {

                    // Get the clicked URL
                    final URL url = event.getURL();

                    // If it is valid
                    if (url != null) {
                        // Get it in the good format
                        final String clickedURL = url.toExternalForm();
                        // Open the url in web browser
                        BrowserLauncher.openURL(clickedURL);
                    } else { // Assume it was an anchor
                        String anchor = event.getDescription();
                        descriptionEditorPane.scrollToReference(anchor);
                    }
                }
            }
        });

        return descriptionEditorPane;
    }

    private JScrollPane setupDescriptionScrollPane() {

        JScrollPane descriptionScrollPane = new JScrollPane(_descriptionEditorPane);
        final Dimension scrollPaneDimension = new Dimension(EDITOR_PANE_WIDTH, PANEL_HEIGHT);
        descriptionScrollPane.setMaximumSize(scrollPaneDimension);
        descriptionScrollPane.setPreferredSize(scrollPaneDimension);
        descriptionScrollPane.setAlignmentX(CENTER_ALIGNMENT);

        // Ensure background color consistency
        _descriptionEditorPane.setOpaque(false);
        descriptionScrollPane.setOpaque(false);
        descriptionScrollPane.getViewport().setOpaque(false);
        descriptionScrollPane.setBorder(null);

        return descriptionScrollPane;
    }

    private JCheckBox setupBetaCheckBox() {

        final JCheckBox betaCheckBox = new JCheckBox("Use beta version");

        betaCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean betaCheckBoxIsSelected = betaCheckBox.isSelected();
                _logger.debug("Beta JNLP Checkbox has been '{}' for application '{}', saving state.",
                        (betaCheckBoxIsSelected ? "selected" : "deselected"), _currentlySelectedApplicationName);
                _cachedBetaCheckBoxStates.put(_currentlySelectedApplicationName, betaCheckBoxIsSelected);

                // Get all desired beta applications
                List<String> betaApplicationList = new ArrayList<String>();
                for (String string : _cachedBetaCheckBoxStates.keySet()) {
                    if (_cachedBetaCheckBoxStates.get(string) == true) {
                        betaApplicationList.add(string);
                    }
                }
                betaApplicationChanged(betaApplicationList);
            }
        });

        betaCheckBox.setEnabled(false);
        betaCheckBox.setAlignmentX(CENTER_ALIGNMENT);

        return betaCheckBox;
    }

    private void changeBetaCheckBoxState(Boolean state) {
        if (state == null) {
            // Reset beta check box
            _betaCheckBox.setSelected(false);
            _betaCheckBox.setEnabled(false);
        } else {
            // Enable check box and set its state
            _betaCheckBox.setSelected(state);
            _betaCheckBox.setEnabled(true);
        }
    }

    private JTextField setupCliPathTextField() {

        _cliPathLabel = new JLabel("Command-line Path : ");
        _cliPathLabel.setLabelFor(_cliPathTextField);

        final JTextField cliPathTextField = new JTextField();
        cliPathTextField.setMinimumSize(new Dimension(200, 20));
        cliPathTextField.setPreferredSize(new Dimension(200, 20));
        cliPathTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String cliPath = cliPathTextField.getText();
                _logger.debug("Application '{}' CLI path = '{}'.", _currentlySelectedApplicationName, cliPath);
                _cachedApplicationCliPathes.put(_currentlySelectedApplicationName, cliPath);
                applicationCliPathChanged(_currentlySelectedApplicationName, cliPath);
            }
        });

        cliPathTextField.setEnabled(false);
        cliPathTextField.setAlignmentX(CENTER_ALIGNMENT);

        return cliPathTextField;
    }

    private void changeCliPathTextFieldValue(String cliPath) {
        if (cliPath == null) {
            _cliPathTextField.setText("");
            _cliPathTextField.setEnabled(false);
            _cliPathLabel.setEnabled(false);
        } else {
            _cliPathTextField.setText(cliPath);
            _cliPathTextField.setEnabled(true);
            _cliPathLabel.setEnabled(true);
        }
    }

    protected void fillApplicationDescriptionPane(String applicationName) {

        // Clear the description if no application name provided
        if (applicationName == null) {
            changeBetaCheckBoxState(null);

            // Reset CLI path
            changeCliPathTextFieldValue(null);

            // Reset description pane
            _descriptionEditorPane.setText("");

            _logger.trace("Cleared description pane.");
            return;
        }

        // if the application description is not yet cached
        String applicationDescription = _cachedApplicationDescriptions.get(applicationName);
        if (applicationDescription == null) {
            _logger.error("Could not find '{}' application description.", applicationName);
            return;
        }

        // Set application description in editor pane
        _descriptionEditorPane.setText(applicationDescription);

        // Show first line of editor pane, and not its last line as by default !
        _descriptionEditorPane.setCaretPosition(0);

        final Boolean checkBoxState = _cachedBetaCheckBoxStates.get(applicationName);
        changeBetaCheckBoxState(checkBoxState);
        if (checkBoxState == null) {
            _logger.debug("No beta JLNP URL found for '{}', disabled beta checkbox.", applicationName);
        } else {
            _logger.debug("Found beta JLNP URL for '{}', enabled beta checkbox to saved state of '{}'.", applicationName, checkBoxState);
        }

        final String cliPath = _cachedApplicationCliPathes.get(applicationName);
        changeCliPathTextFieldValue(cliPath);
        if (cliPath == null) {
            _logger.debug("No CLI path found for '{}' application, disabled CLI textfield.", applicationName);
        } else {
            _logger.debug("Found CLI path '{}' for '{}' application, enabled CLI textfield.", cliPath, applicationName);
        }
    }

    /**
     * Called each time the selection of applications changed.
     * Should be override to save selected application list.
     */
    protected void checkedApplicationChanged(List<String> checkedApplicationList) {
        _logger.debug("Selected applications : {}.", CollectionUtils.toString(checkedApplicationList, ", ", "{", "}"));
    }

    /**
     * Define the current application selection.
     * @param applicationNames the list of application names to select, or null for all.
     */
    public final synchronized void setCheckedApplicationNames(List<String> applicationNames) {

        _programaticCheckingUnderway = true;

        final CheckBoxTreeSelectionModel checkBoxTreeSelectionModel = _checkBoxTree.getCheckBoxTreeSelectionModel();

        // For each tree row
        for (int currentRow = 0; currentRow < _checkBoxTree.getRowCount(); currentRow++) {

            // Select the check box for current row
            final TreePath pathForCurrentRow = _checkBoxTree.getPathForRow(currentRow);
            // Expand all rows
            _checkBoxTree.expandRow(currentRow);

            // Select all or only the desired applications
            final String currentRowApplicationName = pathForCurrentRow.getLastPathComponent().toString();
            if ((applicationNames == null) || (applicationNames.contains(currentRowApplicationName))) {
                checkBoxTreeSelectionModel.addSelectionPath(pathForCurrentRow);
                _logger.debug("Checked '{}' application.", currentRowApplicationName);
            } else {
                checkBoxTreeSelectionModel.removeSelectionPath(pathForCurrentRow);
                _logger.trace("Unchecked '{}' application.", currentRowApplicationName);
            }
        }

        _programaticCheckingUnderway = false;
    }

    /**
     * Called each time the beta state of any application changed.
     * Should be override to save beta application list.
     */
    protected void betaApplicationChanged(List<String> betaApplicationList) {
        _logger.debug("Beta applications : {}.", CollectionUtils.toString(betaApplicationList, ", ", "{", "}"));
    }

    /**
     * Called each time any application is selected, to retrieve whether it has a beta release or not.
     * By default force use of production JNLP URL.
     * 
     * Should be override to return beta application status.
     * @return true if the given application has a beta JNLP URL, false otherwise.
     */
    protected boolean isApplicationBetaJnlpUrlInUse(String applicationName) {
        return false; // By default use production JNLP URL.
    }

    /**
     * Called each time the CLI path of any application changed.
     * Should be override to save application CLI path list.
     */
    protected void applicationCliPathChanged(String applicationName, String cliPath) {
        _logger.debug("applications '{}' CLI path : '{}'.", applicationName, cliPath);
    }

    /**
     * Called each time any application is selected, to retrieve whether it has a CLI path or not.
     * By default force returns none.
     * 
     * Should be override to return application CLI path.
     * @return the application CLI path, or null if none otherwise.
     */
    protected String applicationCliPath(String applicationName) {
        return null; // By default no CLI path.
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        final ApplicationListSelectionPanel applicationListSelectionPanel = new ApplicationListSelectionPanel();
        frame.add(applicationListSelectionPanel);
        frame.pack();
        frame.setVisible(true);

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException ex) {
            _logger.error("", ex);
        }

        List<String> specificApplicationNames = new ArrayList<String>();
        specificApplicationNames.add("Aspro2");
        specificApplicationNames.add("SearchCal");
        specificApplicationNames.add("LITpro");
        specificApplicationNames.add("topcat");
        applicationListSelectionPanel.setCheckedApplicationNames(specificApplicationNames);
    }
}
