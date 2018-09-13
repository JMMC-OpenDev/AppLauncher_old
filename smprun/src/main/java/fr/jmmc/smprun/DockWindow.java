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
package fr.jmmc.smprun;

import fr.jmmc.jmcs.gui.component.StatusBar;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.gui.util.WindowUtils;
import fr.jmmc.jmcs.gui.util.ResourceImage;
import fr.jmmc.jmcs.util.ImageUtils;
import fr.jmmc.smprsc.data.list.StubRegistry;
import fr.jmmc.smprsc.data.list.model.Category;
import fr.jmmc.smprun.preference.ApplicationListSelectionView;
import fr.jmmc.smprun.preference.Preferences;
import fr.jmmc.smprun.stub.ClientStub;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main window. This class is at one central point and play the mediator role.

 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public class DockWindow extends JFrame implements Observer {

    // Constants
    private static final String BETA_SIGN = " ß";
    /** default serial UID for Serializable interface */
    private static final long serialVersionUID = 1;
    /** Logger */
    private static final Logger _logger = LoggerFactory.getLogger(DockWindow.class.getName());
    /** DockWindow singleton */
    private static DockWindow _instance = null;
    /** window dimensions */
    private static final Dimension _windowDimension = new Dimension(640, 120);
    /* members */
    /** button / client map */
    private final HashMap<JButton, ClientStub> _clientButtons = new HashMap<JButton, ClientStub>(8);
    /** client / button map */
    private final HashMap<ClientStub, JButton> _buttonClients = new HashMap<ClientStub, JButton>(8);
    /** User-chosen application name list */
    private final Preferences _preferences;
    /** Unique application button action listener */
    private final ActionListener _buttonActionListener;
    /** to display application descriptions */
    private final ApplicationListSelectionView _applicationDescriptionFactory;

    /**
     * Return the DockWindow singleton 
     * @return DockWindow singleton
     */
    public static DockWindow getInstance() {
        if (_instance == null) {
            _instance = new DockWindow();
            _instance.init();
        }
        return _instance;
    }

    /**
     * Constructor.
     */
    private DockWindow() {

        super("AppLauncher");

        _preferences = Preferences.getInstance();

        _buttonActionListener = new ActionListener() {
            /**
             * Start client application when its icon is clicked
             */
            @Override
            public void actionPerformed(final ActionEvent e) {

                if (e.getSource() instanceof JButton) {
                    final JButton button = (JButton) e.getSource();

                    final ClientStub stub = _clientButtons.get(button);

                    // Start application in background:
                    if (stub != null) {
                        stub.launchRealApplication();
                    }
                }
            }
        };

        _applicationDescriptionFactory = new ApplicationListSelectionView();

        prepareFrame();
    }

    private void init() {

        update(null, null);

        _preferences.addObserver(this);

        _applicationDescriptionFactory.init();

        // Show the user the app is ready to be used
        StatusBar.show("application ready.");
    }

    /**
     * Prepare the frame
     */
    private void prepareFrame() {
        setResizable(false);
        setMinimumSize(_windowDimension);
        setMaximumSize(_windowDimension);
    }

    @Override
    public void update(final Observable observable, Object param) {

        // Using invokeAndWait to be in sync with this thread :
        // note: invokeAndWaitEDT throws an IllegalStateException if any exception occurs
        SwingUtils.invokeAndWaitEDT(new Runnable() {
            /**
             * Initializes the swing components with their actions in EDT
             */
            @Override
            public void run() {

                // If the selected application list changed (preference uptdate)
                if (observable == _preferences) {
                    _logger.debug("Removing all previous components on preference update.");

                    // Remove button listerner before clearing map.
                    for (JButton button : _buttonClients.values()) {
                        button.removeActionListener(_buttonActionListener);
                    }

                    _clientButtons.clear();
                    _buttonClients.clear();

                    // Empty the frame
                    getContentPane().removeAll();
                }

                // Fill the frame
                preparePane();
            }
        });
    }

    /**
     * Prepare the content pane
     */
    private void preparePane() {

        final Container mainPane = getContentPane();
        mainPane.setLayout(new BorderLayout());

        final JPanel verticalListPane = new JPanel();
        verticalListPane.setLayout(new BoxLayout(verticalListPane, BoxLayout.Y_AXIS));

        JLabel familyLabel;
        JScrollPane iconPane;
        for (Category clientFamily : Category.values()) {

            iconPane = buildScrollPane(clientFamily);
            if (iconPane == null) {
                continue;
            }

            familyLabel = new JLabel("<HTML><B>" + clientFamily.value() + "</B></HTML>");
            verticalListPane.add(familyLabel);
            iconPane.setAlignmentX(0.01f);
            verticalListPane.add(iconPane);
            verticalListPane.add(new JSeparator());
        }

        mainPane.add(verticalListPane, BorderLayout.CENTER);
        mainPane.add(StatusBar.getInstance(), BorderLayout.SOUTH);

        pack();
    }

    /**
     * Create one scroll pane per client family
     * @param family client family
     * @return built scroll pane, or null if nothing to display (e.g daemon category)
     */
    private JScrollPane buildScrollPane(final Category family) {

        final JPanel horizontalRowPane = new JPanel();
        horizontalRowPane.setLayout(new BoxLayout(horizontalRowPane, BoxLayout.X_AXIS));

        // Get the list of visible applications for current category
        final List<String> visibleClientNames = StubRegistry.getCategoryVisibleApplicationNames(family);
        if (visibleClientNames == null) {
            return null;
        }

        boolean categoryIsEmpty = true;

        // Try to create GUI stuff for each visible and selected application
        for (final String visibleClientName : visibleClientNames) {

            // If the current client has not been selected by the user
            if (!_preferences.isApplicationNameSelected(visibleClientName)) {
                continue; // Skip its creation
            }

            // Retrieve corresponding stub (if any)
            final ClientStub clientStub = HubPopulator.retrieveClientStub(visibleClientName);
            if (clientStub == null) {
                _logger.error("Could not get '{}' stub.", visibleClientName);
                continue;
            }

            // If the current stub should remain invisble
            final JButton button = buildApplicationButton(clientStub);
            if (button == null) {
                continue; // Skip GUI stuff creation
            }
            categoryIsEmpty = false;
            button.addActionListener(_buttonActionListener);
            horizontalRowPane.add(button);
            _clientButtons.put(button, clientStub);
            _buttonClients.put(clientStub, button);


            Component infoButton = buildInfoButtonForApplication(visibleClientName);
            horizontalRowPane.add(infoButton);

            horizontalRowPane.add(Box.createRigidArea(new Dimension(10, 0)));
        }

        if (categoryIsEmpty) {
            return null;
        }

        horizontalRowPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        final JScrollPane scrollPane = new JScrollPane(horizontalRowPane);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        scrollPane.setPreferredSize(_windowDimension);
        scrollPane.setMinimumSize(_windowDimension);
        scrollPane.setMaximumSize(_windowDimension);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        JViewport view = scrollPane.getViewport();
        view.add(horizontalRowPane);

        return scrollPane;
    }

    /**
     * Create the button representing one client stub (application)
     * @param client client stub instance
     * @return created button, or null if not visible.
     */
    private JButton buildApplicationButton(final ClientStub client) {

        final String clientName = client.getApplicationName();
        ImageIcon clientIcon = client.getApplicationIcon();
        if (clientIcon == null) {
            return null;
        }

        // Resize the icon up to 64*64 pixels
        final int iconHeight = clientIcon.getIconHeight();
        final int iconWidth = clientIcon.getIconWidth();
        clientIcon = ImageUtils.getScaledImageIcon(clientIcon, 64, 64);
        final int newHeight = clientIcon.getIconHeight();
        final int newWidth = clientIcon.getIconWidth();

        // Horizontally center the icon, and bottom-aligned them all vertically
        final int squareSize = 68;
        final int borderSize = 2;
        final int midHorizontalMargin = (squareSize - newWidth) / 2;
        final int topVerticalMargin = squareSize - borderSize - newHeight; // Space to fill above if the icon is smaller than 64 pixels
        final Border border = new EmptyBorder(topVerticalMargin, midHorizontalMargin, borderSize, midHorizontalMargin);

        // Horizontally center application name (with optional beta sign if needed) below its icon
        final JButton button = new JButton(clientIcon);
        button.setName(clientName); // FEST mapping
        String buttonLabel = clientName;
        if (_preferences.isApplicationReleaseBeta(clientName)) {
            buttonLabel += BETA_SIGN;
            button.setForeground(Color.RED);
        }
        button.setText(buttonLabel);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setBorder(border);
        //Add tooltip if any description available
        final String tooltip = client.getDescription();
        if (tooltip != null) {
            button.setToolTipText(tooltip);
        }

        return button;
    }

    /**
     * Callback to re-enable the button representing the client stub
     * @param client client stub to re-enable
     * @param enabled button state
     */
    public void setClientButtonEnabled(final ClientStub client, final boolean enabled) {
        final JButton button = _buttonClients.get(client);
        if (button != null) {
            SwingUtils.invokeEDT(new Runnable() {
                @Override
                public void run() {
                    button.setEnabled(enabled);
                }
            });
        }
    }

    private Component buildInfoButtonForApplication(final String applicationName) {

        JLabel infoButton = new JLabel();

        ImageIcon icon = ResourceImage.INFO_ICON.icon();
        icon = ImageUtils.getScaledImageIcon(icon, 13, 13);
        infoButton.setIcon(icon);

        ImageIcon disabled_icon = ResourceImage.DISABLED_INFO_ICON.icon();
        disabled_icon = ImageUtils.getScaledImageIcon(disabled_icon, 13, 13);
        infoButton.setDisabledIcon(disabled_icon);
        infoButton.setEnabled(false);

        infoButton.setBorder(BorderFactory.createEmptyBorder(68, 1, 0, 0));

        // Enable icon on mouse proximity
        infoButton.addMouseListener(new MouseListener() {
            // Show application description when clicked
            @Override
            public void mouseClicked(MouseEvent e) {
                JFrame frame = new JFrame("Description - " + applicationName);
                frame.add(_applicationDescriptionFactory.retrieveDescriptionPanelForApplication(applicationName));
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.pack();
                WindowUtils.centerOnMainScreen(frame);
                WindowUtils.setClosingKeyboardShortcuts(frame);
                frame.setVisible(true);
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                enableIcon(e, true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                enableIcon(e, false);
            }

            private void enableIcon(MouseEvent e, boolean state) {
                JLabel infoButton = (JLabel) e.getSource();
                if (infoButton != null) {
                    infoButton.setEnabled(state);
                }
            }
        });

        return infoButton;
    }
}
/*___oOo___*/
