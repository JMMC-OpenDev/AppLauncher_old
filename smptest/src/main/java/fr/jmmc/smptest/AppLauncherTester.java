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
package fr.jmmc.smptest;

import fr.jmmc.jmcs.App;
import fr.jmmc.jmcs.Bootstrapper;
import fr.jmmc.jmcs.gui.component.MessagePane;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.gui.util.WindowUtils;
import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.jmcs.network.interop.SampManager;
import fr.jmmc.jmcs.network.interop.SampMessageHandler;
import fr.jmmc.jmcs.util.concurrent.ThreadExecutors;
import javax.swing.JFrame;
import org.astrogrid.samp.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AppLauncherTester main class.
 *
 * @author Sylvain LAFRASSE
 */
public class AppLauncherTester extends App {

    /** Logger */
    private static final Logger _logger = LoggerFactory.getLogger(AppLauncherTester.class.getName());

    /**
     * Launch the AppLauncherTester application.
     *
     * @param args command-line options.
     */
    public AppLauncherTester(final String[] args) {
        // Start whith no splash screen
        super(args);
    }

    @Override
    protected void initServices() {
        // No op
    }

    @Override
    protected void setupGui() {
        // Start first the SampManager (connect to an existing hub or ___internalStart a new one)
        // and check if it is connected to one Hub:
        if (!SampManager.isConnected()) {
            throw new IllegalStateException("Unable to connect to an existing hub or start an internal SAMP hub !");
        }
    }

    @Override
    protected void execute() {
        ThreadExecutors.sleep(30000l); // 30s before exiting
        Bootstrapper.stopApp(1);
    }

    /**
     * Create SAMP Message handlers
     */
    @Override
    protected void declareInteroperability() {

        // Add fake handler to allow AppLauncher JNLP startup test routine
        new SampMessageHandler(SampCapability.APPLAUNCHERTESTER_TRY_LAUNCH) {
            /**
             * Implements message processing
             *
             * @param senderId public ID of sender client
             * @param message message with MType this handler is subscribed to
             * @throws SampException if any error occurred while message processing
             */
            @Override
            protected void processMessage(final String senderId, final Message message) {
                if (_logger.isInfoEnabled()) {
                    _logger.info("Received '{}' message from '{}' : '{}'.",
                            this.handledMType(), senderId, message);
                }
                // Using invokeAndWait to be in sync with this thread :
                // note: invokeAndWaitEDT throws an IllegalStateException if any exception occurs
                SwingUtils.invokeAndWaitEDT(new Runnable() {
                    /**
                     * Initializes the SWING components with their actions in EDT
                     */
                    @Override
                    public void run() {
                        final JFrame frame = getFrame();
                        WindowUtils.centerOnMainScreen(frame);
                        frame.setVisible(false);
                        MessagePane.showMessage("AppLauncher installation and first run went fine !", "Congratulation !");
                        frame.setVisible(false);
                        Bootstrapper.stopApp(0);
                    }
                });
            }
        };
    }

    @Override
    protected void cleanup() {
        // No op
    }

    /**
     * Main entry point
     *
     * @param args command line arguments (open file ...)
     */
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static void main(final String[] args) {
        Bootstrapper.launchApp(new AppLauncherTester(args), false, true, false);
    }
}
/*___oOo___*/
