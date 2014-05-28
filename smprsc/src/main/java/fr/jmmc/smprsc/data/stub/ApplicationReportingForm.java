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
package fr.jmmc.smprsc.data.stub;

import fr.jmmc.jmcs.data.preference.CommonPreferences;
import fr.jmmc.jmcs.gui.util.WindowUtils;
import java.awt.Font;
import javax.swing.AbstractAction;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search Panel
 *
 * @author Sylvain LAFRASSE, Guillaume MELLA.
 */
public class ApplicationReportingForm extends javax.swing.JDialog {

    /** Logger */
    private static final Logger _logger = LoggerFactory.getLogger(ApplicationReportingForm.class.getName());
    // Data stuff
    /** Hold the name of the application to report */
    private final String _applicationName;
    /** User answer (true for submit, false for cancel) */
    private boolean _shouldSubmit;
    /** User answer (true for submit, false for cancel) */
    private boolean _shouldSilentlySubmit;
    /** Hold user email address */
    private String _userEmail;
    /** Hold JNLP URL address */
    private String _applicationURL;
    // Action stuff
    /** Submit action */
    private SubmitAction _submitAction;
    /** Find Next action */
    private CancelAction _cancelAction;

    /** Creates new form ApplicationReportingForm */
    public ApplicationReportingForm(String applicationName) {

        // Remember application name
        _applicationName = applicationName;

        resetState();
        setupActions();
        initComponents();
        setupMainExplanationLabel();
        finishFrameSetup();
    }

    private void resetState() {
        _shouldSilentlySubmit = false;
        _shouldSubmit = false;
        _userEmail = null;
        _applicationURL = null;
    }

    private void setupMainExplanationLabel() {
        _mainExplanationLabel.setContentType(new HTMLEditorKit().getContentType());
        // Add a CSS rule to force body tags to use the default label font instead of the value in javax.swing.text.html.default.csss
        Font font = UIManager.getFont("Label.font");
        String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument) _mainExplanationLabel.getDocument()).getStyleSheet().addRule(bodyRule);
        String message = "<html><<body>"
                + "<center>AppLauncher discovered the '<b>" + _applicationName + "</b>' application it did not know yet !</center><br>"
                + "Do you wish to contribute making AppLauncher better, and send '" + _applicationName + "' application<br>"
                + "description to the JMMC ?<br><br>"
                + "<small><i>No other personnal information than those optionaly provided below will be sent along.</i></small><br>"
                + "</body></html>";
        _mainExplanationLabel.setText(message);
    }

    private void finishFrameSetup() {
        // Automatically fulfill the email field with default shared user email (as in FeedbackReport), if any
        _userEmail = CommonPreferences.getInstance().getPreference(CommonPreferences.FEEDBACK_REPORT_USER_EMAIL);
        _contactEmailField.setText(_userEmail);

        getRootPane().setDefaultButton(_submitButton);

        WindowUtils.centerOnMainScreen(this);
        WindowUtils.setClosingKeyboardShortcuts(this);

        // Show the dialog and wait for user inputs
        pack();
        setVisible(true);
    }

    /** Create required actions */
    private void setupActions() {
        _submitAction = new SubmitAction();
        _cancelAction = new CancelAction();
    }

    protected class SubmitAction extends AbstractAction {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;

        SubmitAction() {
            super();
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            _logger.info("Reported SAMP application meta-data to JMMC registry.");

            _shouldSilentlySubmit = _silentlySubmitCheckBox.isSelected();
            _shouldSubmit = true;
            _userEmail = _contactEmailField.getText();
            _applicationURL = _jnlpUrlField.getText();

            _logger.debug("Hiding dialog box on Submit button.");
            setVisible(false);
        }
    }

    protected class CancelAction extends AbstractAction {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;

        CancelAction() {
            super();
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            _logger.info("Cancelled SAMP application meta-data reporting.");

            // Reset state
            resetState();

            _logger.debug("Hiding dialog box on Submit button.");
            setVisible(false);
        }
    }

    /**
     * @return true if the user choose to submit, false otherwise (cancel)
     */
    public boolean shouldSilentlySubmit() {
        return _shouldSilentlySubmit;
    }

    /**
     * @return true if the user choose to submit, false otherwise (cancel)
     */
    public boolean shouldSubmit() {
        return _shouldSubmit;
    }

    /**
     * @return the user email address if any, null otherwise.
     */
    public String getUserEmail() {
        return _userEmail;
    }

    /**
     * @return the JNLP URL address if any, null otherwise.
     */
    public String getApplicationURL() {
        return _applicationURL;
    }

    public static void main(String[] args) {

        // Create dialog and wait for user response
        ApplicationReportingForm form = new ApplicationReportingForm("UnknownApp v0.0");

        // Output user values
        System.out.println("User answered:");
        System.out.println(" _shouldSilentlySubmit = '" + form.shouldSilentlySubmit() + "'.");
        System.out.println(" _shouldSubmit         = '" + form.shouldSubmit() + "'.");
        System.out.println(" _userEmail            = '" + form.getUserEmail() + "'.");
        System.out.println(" _applicationURL       = '" + form.getApplicationURL() + "'.");

        System.exit(0);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        _mainExplanationLabel = new javax.swing.JEditorPane();
        _jnlpUrlLabel = new javax.swing.JLabel();
        _contactEmailLabel = new javax.swing.JLabel();
        _jnlpUrlField = new javax.swing.JTextField();
        _contactEmailField = new javax.swing.JTextField();
        _silentlySubmitCheckBox = new javax.swing.JCheckBox();
        _cancelButton = new javax.swing.JButton();
        _submitButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Report New SAMP Application to JMMC Registry ?");
        setAlwaysOnTop(true);
        setModal(true);
        setResizable(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());

        _mainExplanationLabel.setEditable(false);
        _mainExplanationLabel.setBackground(jPanel1.getBackground());
        _mainExplanationLabel.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        jPanel1.add(_mainExplanationLabel, gridBagConstraints);

        _jnlpUrlLabel.setText("Application URL:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel1.add(_jnlpUrlLabel, gridBagConstraints);

        _contactEmailLabel.setText("Contact eMail:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel1.add(_contactEmailLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(_jnlpUrlField, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(_contactEmailField, gridBagConstraints);

        _silentlySubmitCheckBox.setText("Silently submit forthcoming unknown applications ?");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(_silentlySubmitCheckBox, gridBagConstraints);

        _cancelButton.setAction(_cancelAction);
        _cancelButton.setText("Cancel");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        jPanel1.add(_cancelButton, gridBagConstraints);

        _submitButton.setAction(_submitAction);
        _submitButton.setText("Submit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        jPanel1.add(_submitButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        getContentPane().add(jPanel1, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton _cancelButton;
    private javax.swing.JTextField _contactEmailField;
    private javax.swing.JLabel _contactEmailLabel;
    private javax.swing.JTextField _jnlpUrlField;
    private javax.swing.JLabel _jnlpUrlLabel;
    private javax.swing.JEditorPane _mainExplanationLabel;
    private javax.swing.JCheckBox _silentlySubmitCheckBox;
    private javax.swing.JButton _submitButton;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
}
