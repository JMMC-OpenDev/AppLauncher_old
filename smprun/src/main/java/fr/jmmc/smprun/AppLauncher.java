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

import fr.jmmc.jmcs.App;
import fr.jmmc.jmcs.Bootstrapper;
import fr.jmmc.jmcs.data.preference.PreferencesException;
import fr.jmmc.jmcs.gui.FeedbackReport;
import fr.jmmc.jmcs.gui.PreferencesView;
import fr.jmmc.jmcs.gui.action.RegisteredAction;
import fr.jmmc.jmcs.gui.action.ShowReleaseNotesAction;
import fr.jmmc.jmcs.gui.component.BooleanPreferencesView;
import fr.jmmc.jmcs.gui.component.MessagePane;
import fr.jmmc.jmcs.gui.component.ResizableTextViewFactory;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.gui.util.WindowUtils;
import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.jmcs.network.interop.SampManager;
import fr.jmmc.jmcs.service.JnlpStarter;
import fr.jmmc.jmcs.util.concurrent.ThreadExecutors;
import fr.jmmc.jmcs.util.runner.LocalLauncher;
import fr.jmmc.smprsc.data.RegistryManager;
import fr.jmmc.smprun.preference.ApplicationListSelectionView;
import fr.jmmc.smprun.preference.PreferenceKey;
import fr.jmmc.smprun.preference.Preferences;
import fr.jmmc.smprun.stub.ClientStub;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;
import javax.swing.JPanel;
import org.astrogrid.samp.client.SampException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AppLauncher main class.
 *
 * @author Sylvain LAFRASSE, Laurent BOURGES.
 */
public class AppLauncher extends App {

    /** welcome message */
    private final static String WELCOME_MESSAGE = "<HTML><HEAD></HEAD><BODY>"
            + "<CENTER><H2>Welcome to AppLauncher !!!</H2></CENTER>"
            + "<BR>"
            + "And thank you for your confidence in the JMMC automatic SAMP application launcher.<BR>"
            + "<BR>"
            + "- First, an auto-test procedure will proceed (after you clicked OK) to confirm everything is fine for AppLauncher to work well;<BR>"
            + "- You can customize (among other things) which applications are shown in the Dock using the preferences window;<BR>"
            + "- Further documentation is available directly from the Help menu, so don't hesitate to have a look;<BR>"
            + "- You can easily provide (greatly appreciated) feedback and bug reports to us from the dedicated entry in the Help menu.<BR>"
            + "<BR>"
            + "<B>We hope you will appreciate using AppLauncher as much as we had fun making it !</B>";
    /** AppLauncherTester stub name */
    private final static String APP_LAUNCHER_TESTER = "AppLauncherTester";
    /** AppLauncherTester auto test timeout in milliseconds */
    private final static long APPLAUNCHER_TESTER_TIMEOUT = 2 * 60 * 1000L;
    /** Logger */
    protected static final Logger _logger = LoggerFactory.getLogger(AppLauncher.class.getName());
    /** Launch JNLP/SAMP Auto-Test action (menu) */
    public LaunchJnlpSampAutoTestAction _launchJnlpSampAutoTestAction = null;
    /** Launch Java WebStart Viewer action (menu) */
    public LaunchJavaWebStartViewerAction _launchJavaWebStartViewerAction = null;
    /** Show Registry Release Notes action (menu) */
    public ShowReleaseNotesAction _launchRegistryReleaseNotes = null;
    /** optional dock window */
    private static DockWindow _dockWindow = null;
    /** preferences instance */
    private Preferences _preferences = null;

    /**
     * Launch the AppLauncher application.
     *
     * Create all objects needed by AppLauncher and plug event responding loop (Listener/Listenable,
     * Observer/Observable) in.
     *
     * @param args command-line options.
     */
    public AppLauncher(final String[] args) {
        super(args);
        // For debugging purpose only, to dismiss splashscreen
        //super(args, false, true, false);
    }

    @Override
    protected void initServices() {
        _preferences = Preferences.getInstance();
        // Initialize job runner
        LocalLauncher.startUp();
    }

    @Override
    protected void setupGui() {

        _launchJnlpSampAutoTestAction = new LaunchJnlpSampAutoTestAction(getClass().getName(), "_launchJnlpSampAutoTestAction");
        _launchJavaWebStartViewerAction = new LaunchJavaWebStartViewerAction(getClass().getName(), "_launchJavaWebStartViewerAction");
        _launchRegistryReleaseNotes = new ShowReleaseNotesAction("_launchRegistryReleaseNotes", "AppLauncher Registry", RegistryManager.getInstance().getDescription());

        // Start first the SampManager (connect to an existing hub or launch a new one)
        // and check if it is connected to one Hub:
        if (!SampManager.isConnected()) {
            throw new IllegalStateException("Unable to connect to an existing hub or start an internal SAMP hub !");
        }

        // First initialize the Client descriptions:
        HubPopulator.start();

        // Prepare dock window
        _dockWindow = DockWindow.getInstance();

        if (_dockWindow != null) {
            App.setFrame(_dockWindow);

            preparePreferencesWindow();
        }

        // @TODO : Handle JMMC app mimetypes to open our apps !!!
    }

    private void preparePreferencesWindow() {

        // Retrieve application preference panes and attach them to their view
        LinkedHashMap<String, JPanel> panels = new LinkedHashMap<String, JPanel>();

        // Create application selection pane
        ApplicationListSelectionView applicationListSelectionView = new ApplicationListSelectionView();
        applicationListSelectionView.init();
        panels.put("Application Selection", applicationListSelectionView);

        // Create general settings pane
        LinkedHashMap<Object, String> generalSettingsMap = new LinkedHashMap<Object, String>();
        generalSettingsMap.put(PreferenceKey.SHOW_EXIT_WARNING, "Show warning before shuting down SAMP hub while quitting");
        generalSettingsMap.put(PreferenceKey.START_SELECTED_STUBS, "Restrict automatic tierce app startup to your selection");
        generalSettingsMap.put(PreferenceKey.DISCARD_BROADCASTS_FLAG, "Silently skip SAMP broadcast messages");
        generalSettingsMap.put(PreferenceKey.SILENTLY_REPORT_FLAG, "Silently report unknown applications to JMMC");
        BooleanPreferencesView generalSettingsView = new BooleanPreferencesView(_preferences, generalSettingsMap, BooleanPreferencesView.SAVE_AND_RESTART_MESSAGE);
        generalSettingsView.init();
        panels.put("General Settings", generalSettingsView);

        // Finalize prefence window
        PreferencesView preferencesView = new PreferencesView(App.getFrame(), _preferences, panels);
        preferencesView.init();
    }

    /**
     * Create SAMP Message handlers
     */
    @Override
    protected void declareInteroperability() {
        // Initialize the Hub monitor which starts client stubs if necessary
        HubMonitor.getInstance();
    }

    /**
     * Execute application body = make the application frame visible
     */
    @Override
    protected void execute() {

        WindowUtils.centerOnMainScreen(_dockWindow);

        // If JNLP/SAMP startup test went fine
        SwingUtils.invokeLaterEDT(new Runnable() {
            /**
             * Show the application frame using EDT
             */
            @Override
            public void run() {
                _logger.debug("Setting AppLauncher GUI up.");

                // Show Dock window if not hidden
                getFrame().setVisible(_dockWindow != null);
            }
        });

        // Show Welcome pane and perform JNLP/SAMP auto-test on first AppLauncher launch
        performFirstRunTasks();
    }

    @Override
    public boolean shouldSilentlyKillSampHubOnQuit() {

        final boolean shouldSilentlyKillSampHubOnQuit = !_preferences.getPreferenceAsBoolean(PreferenceKey.SHOW_EXIT_WARNING);
        return shouldSilentlyKillSampHubOnQuit;
    }

    /**
     * Hook to handle operations when exiting application.
     *
     * @see App#exit(int)
     */
    @Override
    protected void cleanup() {

        // Stop job runner first to prevent new job submission
        LocalLauncher.shutdown();

        // Properly disconnect connected clients
        HubPopulator.disconnectAllStubs();

        _launchJnlpSampAutoTestAction = null;
        _launchJavaWebStartViewerAction = null;
        _dockWindow = null;
        _preferences = null;
    }

    /**
     * Show the application frame and bring it to front only if the DockWindow is visible
     */
    public static void showFrameToFront() {
        if (_dockWindow != null) {
            App.showFrameToFront();
        }
    }

    /**
     * Perform first run tests if needed
     */
    private void performFirstRunTasks() {

        // If it is the first time ever AppLauncher is started
        final boolean isFirstRun = _preferences.getPreferenceAsBoolean(PreferenceKey.FIRST_START_FLAG);
        if (!isFirstRun) {
            return; // Skip first run tasks
        }

        _logger.info("First time AppLauncher is starting (no preference file found).");

        SwingUtils.invokeLaterEDT(new Runnable() {
            @Override
            public void run() {

                // Show a modal Welcome pane and wait for OK/close:
                ResizableTextViewFactory.createHtmlWindow(WELCOME_MESSAGE, "Welcome to AppLauncher !!!", true);

                // Execute tests in background
                ThreadExecutors.getGenericExecutor().submit(new Runnable() {
                    /**
                     * Launch JNLP/SAMP Auto-Test using dedicated thread (2 minutes timeout)
                     */
                    @Override
                    public void run() {

                        // Run JNLP/SAMP abilities test
                        if (!checkJnlpSampAbilities()) {
                            _logger.error("Could not successfully perform JNLP/SAMP auto-test, aborting.");
                            return;
                        }

                        _logger.info("Successfully performed first run tasks.");

                        // Create preference file to skip this test for future starts
                        try {
                            _preferences.setPreference(PreferenceKey.FIRST_START_FLAG, false);
                            _preferences.saveToFile();
                        } catch (PreferencesException ex) {
                            _logger.warn("Could not write to preference file :", ex);
                        }
                    }
                });
            }
        });
    }

    /**
     * @return true if the test went fine, false otherwise
     */
    private boolean checkJnlpSampAbilities() {

        // First wait for stubs to canBeTerminatedNow startup
        HubMonitor.getInstance().waitForStubsStartup();

        boolean success = false;

        // Try to send a SampCapability.APPLAUNCHERTESTER_TRY_LAUNCH to AppLauncherTester stub to test our whole machinery
        List<String> clientIds = SampManager.getClientIdsForName(APP_LAUNCHER_TESTER);
        if (!clientIds.isEmpty()) {

            // TODO : Should only send this message to our own stub
            final String appLauncherTesterClientId = clientIds.get(0);

            // Retrieve corresponding stub (if any)
            final ClientStub testerStub = HubPopulator.retrieveClientStub(APP_LAUNCHER_TESTER);
            if (testerStub == null) {
                _logger.warn("Client stub [{}] not found.", APP_LAUNCHER_TESTER);
                return false;
            }

            // ClientStub: LISTENING
            if (!testerStub.isConnected()) {
                _logger.warn("Client stub [{}] not connected to SAMP.", APP_LAUNCHER_TESTER);
                return false;
            }

            // try to send the dedicated test message to our stub:

            final boolean jnlpVerbose = JnlpStarter.isJavaWebStartVerbose();
            try {
                // restore Javaws verbose setting:
                JnlpStarter.setJavaWebStartVerbose(true);

                final String appLauncherTesterMType = SampCapability.APPLAUNCHERTESTER_TRY_LAUNCH.mType();
                SampManager.sendMessageTo(appLauncherTesterMType, appLauncherTesterClientId, null);

                // Wait for tester stub to succeed (LAUNCH -> SEEK -> FORWARD -> DISCONNECT -> DYE)
                testerStub.waitForSuccess(APPLAUNCHER_TESTER_TIMEOUT);

                success = true;

            } catch (SampException se) {
                FeedbackReport.openDialog(se);
            } catch (TimeoutException te) {
                _logger.info("timeout: ", te);
                MessagePane.showWarning("A timeout occured while performing JNLP/SAMP auto-test."
                        + "\n\nPlease check your Java Web Start settings and accept security prompts.");
            } finally {
                // restore Javaws verbose setting:
                JnlpStarter.setJavaWebStartVerbose(jnlpVerbose);
            }
        }

        return success;
    }

    protected class LaunchJnlpSampAutoTestAction extends RegisteredAction {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;

        LaunchJnlpSampAutoTestAction(String classPath, String fieldName) {
            super(classPath, fieldName);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {

            ThreadExecutors.getGenericExecutor().submit(new Runnable() {
                /**
                 * Launch JNLP/SAMP Auto-Test using dedicated thread (2 minutes timeout)
                 */
                @Override
                public void run() {
                    checkJnlpSampAbilities();
                }
            });
        }
    }

    protected class LaunchJavaWebStartViewerAction extends RegisteredAction {

        /** default serial UID for Serializable interface */
        private static final long serialVersionUID = 1;

        LaunchJavaWebStartViewerAction(String classPath, String fieldName) {
            super(classPath, fieldName);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            JnlpStarter.launchJavaWebStartViewer();
        }
    }

    /**
     * Main entry point
     *
     * @param args command line arguments (open file ...)
     */
    public static void main(final String[] args) {

        // Start application with the command line arguments
        Bootstrapper.launchApp(new AppLauncher(args));
    }
}
/*___oOo___*/
