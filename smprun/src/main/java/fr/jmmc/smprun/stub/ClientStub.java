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
package fr.jmmc.smprun.stub;

import fr.jmmc.jmcs.data.preference.PreferencesException;
import fr.jmmc.jmcs.gui.component.DismissableMessagePane;
import fr.jmmc.jmcs.gui.component.MessagePane;
import fr.jmmc.jmcs.gui.component.StatusBar;
import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.jmcs.network.interop.SampManager;
import fr.jmmc.jmcs.network.interop.SampMetaData;
import fr.jmmc.jmcs.service.BrowserLauncher;
import fr.jmmc.jmcs.service.JnlpStarter;
import fr.jmmc.jmcs.util.CommandLineUtils;
import fr.jmmc.jmcs.util.StringUtils;
import fr.jmmc.jmcs.util.concurrent.ThreadExecutors;
import fr.jmmc.jmcs.util.runner.JobListener;
import fr.jmmc.jmcs.util.runner.LocalLauncher;
import fr.jmmc.jmcs.util.runner.RootContext;
import fr.jmmc.jmcs.util.runner.RunContext;
import fr.jmmc.jmcs.util.runner.RunState;
import fr.jmmc.jmcs.util.runner.process.ProcessContext;
import fr.jmmc.smprsc.data.stub.StubMetaData;
import fr.jmmc.smprsc.data.stub.model.SampStub;
import fr.jmmc.smprsc.data.stub.model.Type;
import fr.jmmc.smprun.DockWindow;
import fr.jmmc.smprun.preference.PreferenceKey;
import fr.jmmc.smprun.preference.Preferences;
import java.util.*;
import java.util.concurrent.TimeoutException;
import javax.swing.ImageIcon;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Subscriptions;
import org.astrogrid.samp.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers a fake application to the hub, and later dispatch any received message to the freshly started recipient.
 *
 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public final class ClientStub extends Observable implements JobListener {

    /** Class logger */
    private static final Logger _logger = LoggerFactory.getLogger(ClientStub.class.getName());
    /** AppLauncher shared preferences */
    private final Preferences _preferences;
    /* members : app meta data object */
    /**
     * Store desired stub application meta data
     */
    private final Metadata _description;
    /**
     * Convenient proxy to meta data dedicated field)
     */
    private final String _applicationName;
    /**
     * Store desired stub SAMP capabilities
     */
    private final SampCapability[] _mTypes;
    /**
     * sleep delay in milliseconds before sending the SAMP message (application startup workaround)
     */
    private final long _sleepDelayBeforeNotify;
    /**
     * application execution type
     */
    private final Type _executionType;
    /**
     * log prefix
     */
    private final String _logPrefix;
    /* state objects */
    /**
     * internal lock object for synchronization
     */
    private final Object _lock = new Object();
    /**
     * client stub state
     */
    private ClientStubState _status;
    /**
     * job context identifier representing the executed application to be able to kill / cancel its execution
     */
    private volatile Long _jobContextId = null;
    /**
     * Messages queued, to forward once recipient appeared
     */
    private volatile Queue<Message> _messages = new LinkedList<Message>();
    /* SAMP objects */
    /**
     * Hub connector
     */
    private final HubConnector _connector;
    /**
     * Potential message handler
     */
    private AbstractMessageHandler[] _mHandlers = null;

    /**
     * Constructor.
     *
     * @param data XML values
     */
    public ClientStub(final SampStub data) {

        _preferences = Preferences.getInstance();

        // Retrieve each serialized SAMP meta data (remove accent to be samp compliant)
        _description = new Metadata();
        for (fr.jmmc.smprsc.data.stub.model.Metadata metadata : data.getMetadatas()) {
            _description.put(metadata.getKey(), StringUtils.removeAccents(metadata.getValue()));
        }

        // Retrieve real application name, JNLP URL and startup delay
        _applicationName = _description.getName();
        _logPrefix = "Stub['" + _applicationName + "'] : ";
        _sleepDelayBeforeNotify = data.getLag().intValue();
        _executionType = data.getType();

        // Add a custom flag to all our created STUB for later skipping while looking for real recipients
        _description.put(SampMetaData.getStubMetaDataId(_applicationName), SampMetaData.STUB_TOKEN);

        // Retrieve and filter each serialized SAMP mType
        List<SampCapability> capabilityList = new ArrayList<SampCapability>();
        for (String capability : data.getSubscriptions()) {

            // Validate read mType against our known mTypes
            SampCapability sampCapability = SampCapability.fromMType(capability);

            // Do not register SAMP internal or likely broadcasted capabilities
            if (sampCapability.isFlagged()) {
                _logger.debug("{}Looks like '{}' SAMP capability is either broadcastable or of internal use ... skipping.", _logPrefix, capability);
                continue;
            }
            // Do not register UNKNOWN capabilities
            if (sampCapability == SampCapability.UNKNOWN) {
                _logger.warn("{}Could not retrieve '{}' unknown SAMP capability ... skipping.", _logPrefix, capability);
                continue;
            }

            _logger.info("{}Found '{}' SAMP capability.", _logPrefix, capability);

            capabilityList.add(sampCapability);
        }
        _mTypes = capabilityList.toArray(new SampCapability[0]);

        setState(ClientStubState.UNDEFINED);

        // Get a hook on the SAMP machinery
        final ClientProfile profile = DefaultClientProfile.getProfile();
        _connector = new HubConnector(profile);
    }

    /**
     * @return the name of the emulated application.
     */
    public String getApplicationName() {
        return _applicationName;
    }

    /**
     * @return the description of the emulated application, null otherwise.
     */
    public String getDescription() {
        return _description.getDescriptionText();
    }

    /**
     * @return application execution type
     */
    public Type getExecutionType() {
        return _executionType;
    }

    /**
     * Return the name of the emulated application
     *
     * @return the name of the emulated application
     */
    @Override
    public String toString() {
        return getApplicationName();
    }

    /**
     * @return the URL of the icon if any (null otherwise)
     */
    public ImageIcon getApplicationIcon() {

        // @TODO : Use a generic app icon as placeholder when none available... BUT AppLauncherTester is kept invisible because of this...
        // Try to load embedded one in smprrsc
        return StubMetaData.getEmbeddedApplicationIcon(_applicationName);
    }

    /**
     * @return the good JNLP URL (whether production or beta one according to user preferences current state), null
     * otherwise.
     */
    public String getFinalJnlpUrl() {

        SampMetaData sampFinalJnlpId = SampMetaData.JNLP_URL;
        if (_preferences.isApplicationReleaseBeta(_applicationName)) {
            sampFinalJnlpId = SampMetaData.JNLP_BETA_URL;
        }

        return _description.getString(sampFinalJnlpId.id());
    }

    /**
     * @return the web application URL, null otherwise.
     */
    public String getWebApplicationUrl() {

        SampMetaData sampWebApppId = SampMetaData.WEBAPP_URL;
        return _description.getString(sampWebApppId.id());
    }

    /**
     * @return the command-line application path, null otherwise.
     */
    public String getApplicationCliPath() {

        final String preferedCliPath = _preferences.getApplicationCliPath(_applicationName);
        if ((preferedCliPath != null) && (preferedCliPath.length() > 0)) {
            return preferedCliPath;
        }

        SampMetaData sampCliPathId = SampMetaData.CLI_PATH;
        final String defaultCliPath = _description.getString(sampCliPathId.id());
        if ((defaultCliPath != null) && (defaultCliPath.length() > 0)) {
            return defaultCliPath;
        }

        // Ask user for CLI path in a pop up
        final String title = "Please give '" + _applicationName + "' command-line path ?";
        final String message = "AppLauncher tries to start the '" + _applicationName + "' native application.\n"
                + "As it is a user-installed application, AppLauncher does not know where to find it.\n\n"
                + "Please enter a straight command-line to launch it (arguments not supported) :";
        final String userCliPath = MessagePane.showInputMessage(message, title);
        if ((userCliPath != null) && (userCliPath.length() > 0)) { // User did not canceled the input
            _preferences.setApplicationCliPath(_applicationName, userCliPath);
            try {
                _preferences.saveToFile();
            } catch (PreferencesException ex) {
                _logger.warn("Could not write to preference file.", ex);
            }
            return userCliPath;
        }

        return null;
    }

    /**
     * @return the SAMP capabilities
     */
    public SampCapability[] getSampCapabilities() {
        return _mTypes;
    }

    /**
     * Change the application button state (enabled or disabled) on the dock window
     *
     * @param state true to enable the application button
     */
    private void setClientButtonEnabled(final boolean state) {
        // Only if DockWindow is visible
        final DockWindow dockWindow = DockWindow.getInstance();
        if (dockWindow != null) {
            dockWindow.setClientButtonEnabled(this, state);
        }
    }

    /**
     * Define the job context identifier
     *
     * @param jobContextId job context identifier to set
     */
    private void setJobContextId(final Long jobContextId) {
        _jobContextId = jobContextId;
    }

    /**
     * Reset job context...
     */
    private void resetMessageQueue() {
        _messages.clear();
    }

    /**
     * Return the internal state
     *
     * @return internal state
     */
    private ClientStubState getState() {
        synchronized (_lock) {
            return _status;
        }
    }

    /**
     * Update stub internal state progression and notifies observers
     *
     * @param status the current state
     */
    private void setState(final ClientStubState status) {
        // update status
        _status = status;

        setChanged();
        notifyObservers(_status);
    }

    /**
     * Perform initialization (connect to hub, register MTypes ...)
     */
    public void connect() {

        // Reentrance / concurrency checks
        synchronized (_lock) {
            if (_status == ClientStubState.UNDEFINED || _status == ClientStubState.DIYING) {
                setState(ClientStubState.INITIALIZING);

                // If the client is a web app (no SAMP capability at all)
                if (_mTypes.length <= 0) {
                    _logger.info("{}Web application ready.", _logPrefix);
                    return; // Skip SAMP stub connection
                }

                // Try to connect
                if (!connectToHub()) {
                    disconnect();
                }
            }
        }
    }

    /**
     * @return true only if this client stub is really connected to the hub, false otherwise
     */
    public boolean isConnected() {

        boolean connected = false;

        // Reentrance / concurrency checks
        synchronized (_lock) {
            // If the client is a web app (no SAMP capability at all)
            if (_mTypes.length <= 0) {
                connected = true; // Always fake it is connected
            } else if (_status.after(ClientStubState.INITIALIZING) && _status.before(ClientStubState.DISCONNECTING)) {
                connected = _connector.isConnected();
            }
        }
        return connected;
    }

    /**
     * Disconnect from hub
     */
    public void disconnect() {
        // Reentrance / concurrency checks
        synchronized (_lock) {
            if (_status.after(ClientStubState.INITIALIZING) && _status.before(ClientStubState.DISCONNECTING)) {

                // Kill the stub client
                setState(ClientStubState.DISCONNECTING);

                _logger.info("{}Disconnecting from hub ...", _logPrefix);

                // Disconnect from hub
                _connector.setActive(false);

                _logger.info("{}Dying ...", _logPrefix);

                setState(ClientStubState.DIYING);

                // openJDK issue: detach from started javaws process:
                cleanup(true);

                _logger.info("{}Disconnected.", _logPrefix);
            }
        }
    }

    /**
     * Launch the real application
     */
    public void launchRealApplication() {
        _logger.info("{}Launching real '{}' application ...", _logPrefix, _executionType);

        // Reentrance / concurrency checks
        synchronized (_lock) {

            // Note: when the javaws does not start correctly the application => it will never connect to SAMP; let the user retry ...
            switch (_executionType) {
                case JNLP:
                    final String finalJnlpUrl = getFinalJnlpUrl();
                    if (finalJnlpUrl != null) {
                        StatusBar.show("starting '" + _applicationName + "' recipient...");
                        _logger.info("{}Launching JNLP '{}' ...", _logPrefix, finalJnlpUrl);

                        // stub is connected i.e. monitoring SAMP messages ...
                        if (isConnected()) {
                            // only change state if this stub is running:
                            setState(ClientStubState.LAUNCHING);

                            setClientButtonEnabled(false);

                            // get the process context to be able to kill it later ...
                            setJobContextId(JnlpStarter.launch(finalJnlpUrl, this));
                        } else {
                            // just start application without callbacks:
                            JnlpStarter.launch(finalJnlpUrl);
                        }
                    } else {
                        _logger.error("{}JNLP URL not found.", _logPrefix);
                    }
                    break;

                case WEB:
                    // Get WebApp URL
                    final String webAppURL = getWebApplicationUrl();
                    if (webAppURL != null) {
                        StatusBar.show("opening '" + _applicationName + "' web application...");
                        _logger.info("{}Opening web application at URL '{}' ...", _logPrefix, webAppURL);
                        BrowserLauncher.openURL(webAppURL);
                    } else {
                        _logger.error("{}Web URL not found.", _logPrefix);
                    }
                    break;

                case CLI:
                    final String applicationCliPath = getApplicationCliPath();
                    if (applicationCliPath != null) {
                        StatusBar.show("starting '" + _applicationName + "' recipient...");
                        _logger.info("{}Launching command-line path '{}' ...", _logPrefix, applicationCliPath);
                        CommandLineUtils.exec(applicationCliPath);
                    } else {
                        _logger.error("{}Command-line path not found.", _logPrefix);
                    }
                    break;

                default:
                    _logger.error("{}Could not handle unknown '{}' execution type.", _logPrefix, _executionType);
                    break;
            }
            _logger.info("{}Launch done.", _logPrefix);
        }
    }

    /**
     *
     * @param timeout timeout in milliseconds
     * @throws TimeoutException if the stub did not succeed in time
     */
    public void waitForSuccess(final long timeout) throws TimeoutException {
        final long start = System.nanoTime();

        // Wait for this client stub to die:
        while (!getState().equals(ClientStubState.DIYING)) {
            _logger.debug("{}Waiting for stub to succeed ...", _logPrefix);
            try {
                Thread.sleep(100L);
            } catch (InterruptedException ie) {
                _logger.error("{}Interrupted while waiting for success.", _logPrefix, ie);
            }

            final long time = (System.nanoTime() - start) / 1000000; // ms

            if (time > timeout) {
                throw new TimeoutException("Stub did not succeed in time !");
            }
        }
    }

    /**
     * Cancel or kill the launch of the real application
     */
    public void killRealApplication() {
        _logger.info("{}Killing real application ...", _logPrefix);

        cleanup(false);
    }

    /**
     * Handle cleanup (process, button state and other internal state)
     *
     * @param success true for success
     */
    private void cleanup(final boolean success) {
        _logger.info("{}Cleaning up ...", _logPrefix);

        // Reentrance / concurrency checks
        synchronized (_lock) {

            if (_jobContextId != null) {
                /*
                 * Note: the cancel does not work on unix system:
                 * javaws is the parent command that launches another command java ...
                 *
                 * Process.destroy does not kill sub processes: we could use ps -ef ... | kill
                 * but it tricky again
                 */

                _logger.debug("{}cleanup(): cancelOrKillJob = {}", _logPrefix, _jobContextId);

                LocalLauncher.cancelOrKillJob(_jobContextId);
                setJobContextId(null);
            }

            // check current state to avoid reporting failure twice or incorrect state:
            final boolean doFail = !success && (_status.after(ClientStubState.PROCESSING) && _status.before(ClientStubState.DISCONNECTING));

            _logger.debug("{}cleanup(): doFail = {}", _logPrefix, doFail);

            if (doFail) {
                // Report failure
                setState(ClientStubState.FAILING);
            }

            // Handle error
            if (!_messages.isEmpty()) {
                _logger.error("{}Unable to deliver '{}' message(s) :", _logPrefix, _messages.size());
                for (Message msg : _messages) {
                    _logger.error("\t- '{}'", msg);
                }

                // @TODO : MessagePane ... => State = FAILED => Window (hide)
            }

            // Reset state
            setJobContextId(null);
            resetMessageQueue();

            if (doFail) {
                setState(ClientStubState.LISTENING);

                // Update GUI
                StatusBar.show("failed to start '" + getApplicationName() + "'.");
            } else if (success) {

                // Update GUI
                StatusBar.show("started '" + getApplicationName() + "'.");
            }
            setClientButtonEnabled(true);
        }
    }

    /**
     * Connect stub to the hub.
     *
     * @return true if successfully connected to hub, false otherwise.
     */
    private boolean connectToHub() {

        setState(ClientStubState.CONNECTING);

        _logger.info("{}Connecting to hub ...", _logPrefix);

        // Set connector up
        _connector.declareMetadata(_description);

        // Try to connect
        _connector.setActive(true);

        if (!_connector.isConnected()) {
            _logger.info("{}Could not connect to an existing hub.", _logPrefix);
            return false;
        }

        // Keep a look out for hubs if initial one shuts down
        _connector.setAutoconnect(5);

        registerStubCapabilities();

        _logger.info("{}Connected.", _logPrefix);

        return true;
    }

    /**
     * Declare STUB capabilities to the hub.
     */
    private void registerStubCapabilities() {

        setState(ClientStubState.REGISTERING);

        // Lazy initialisation
        if (_mHandlers == null) {

            _mHandlers = new AbstractMessageHandler[_mTypes.length];

            int i = 0;
            for (final SampCapability mType : _mTypes) {

                // Create handler for each stub capability
                _mHandlers[i] = new AbstractMessageHandler(mType.mType()) {
                    /**
                     * Implements message processing
                     *
                     * @param senderId public ID of sender client
                     * @param message message with MType this handler is subscribed to
                     * @throws SampException if any error occurred while message processing
                     */
                    @Override
                    public final Map<?, ?> processCall(final HubConnection connection, final String senderId, final Message message) throws SampException {

                        _logger.info("{}Received '{}' message from '{}' : '{}'.", _logPrefix, mType.mType(), senderId, message);

                        // Is the message coming from a broadcast ?
                        final String sampBroadcastID = SampMetaData.BROADCAST_MESSAGE_FLAG.id();
                        final String broadcastToken = ((String) message.getParam(sampBroadcastID));
                        if (broadcastToken != null) {

                            final Boolean isBroadcasted = broadcastToken.trim().equalsIgnoreCase("true");
                            if (isBroadcasted) {

                                // Should we discard broadcasts ?
                                final boolean shouldSilentlyDiscardBroadcast = _preferences.getPreferenceAsBoolean(PreferenceKey.DISCARD_BROADCASTS_FLAG);
                                if (!shouldSilentlyDiscardBroadcast) {
                                    try {
                                        _preferences.setPreference(PreferenceKey.DISCARD_BROADCASTS_FLAG, true);
                                    } catch (PreferencesException ex) {
                                        _logger.warn("Could not set preference :", ex);
                                    }

                                    // First time dismissable pane to inform user
                                    String broadcastMessageMType = message.getMType();
                                    SampCapability capability = SampCapability.fromMType(broadcastMessageMType);
                                    final String DISMISS_MESSAGE_FLAG = PreferenceKey.BROADCAST_WARNING_FLAG.toString();
                                    DismissableMessagePane.setPreferenceState(_preferences, DISMISS_MESSAGE_FLAG, false);
                                    DismissableMessagePane.show("AppLauncher just received a SAMP broadcast message for all '" + capability + "' applications.\n"
                                            + "We strongly discourage broadcast messages, to avoid multiple application startup simultaneously.\n"
                                            + "This message will thus be discarded internaly.\n\n"
                                            + "Please note that already running applications will still receive the message flawlessly.",
                                            _preferences, DISMISS_MESSAGE_FLAG);

                                    final boolean shouldHideBroadcastWarning = DismissableMessagePane.getPreferenceState(_preferences, DISMISS_MESSAGE_FLAG);
                                    try {
                                        _preferences.setPreference(PreferenceKey.DISCARD_BROADCASTS_FLAG, shouldHideBroadcastWarning);
                                        _preferences.saveToFile();
                                    } catch (PreferencesException ex) {
                                        _logger.warn("Could not set preference :", ex);
                                    }
                                }

                                // Always skip stub broadcast
                                _logger.warn("Broadcast message detected, discarding it.");
                                StatusBar.show("discarding broadcast messages.");
                                return null;
                            }
                        }

                        // Backup message and pending queue for later delivery
                        _messages.add(message);

                        // Start application in background:
                        launchRealApplication();

                        // Once the application will have finish started and been fully registered to the hub,
                        // HubMonitor will detect it and ask the stub to forward any pending message to the real application.
                        return null;
                    }
                };

                i++;
            }

            // Declare each message handlers to SAMP hub
            for (final AbstractMessageHandler handler : _mHandlers) {
                _connector.addMessageHandler(handler);
            }
        }

        // If the client is not a web app (no SAMP capability at all)
        if (_mTypes.length > 0) {
            // This step is required to update message handlers into the hub:
            _connector.declareSubscriptions(_connector.computeSubscriptions());

            _logger.info("{}Declared SAMP subscriptions.", _logPrefix);

            setState(ClientStubState.LISTENING);
        }
    }

    /**
     * Implements callback from HubMonitor when the real application is detected... Note: this method is called using
     * dedicated thread (may sleep for few seconds ...)
     *
     * @param recipientId recipient identifier of the real application.
     */
    public void forwardMessagesToRealRecipient(final String recipientId) {
        // Reentrance check
        synchronized (_lock) {
            if (_status.after(ClientStubState.REGISTERING) && _status.before(ClientStubState.DISCONNECTING)) {
                _logger.info("{}Forwarding message(s) to real recipient connected with id '{}'.", _logPrefix, recipientId);

                // Forward all received message to recipient (if any)
                if (!_messages.isEmpty()) {
                    setState(ClientStubState.SEEKING);

                    if (_sleepDelayBeforeNotify > 0L) {
                        _logger.info("{}Waiting {} milliseconds to let real recipient finish its startup ...", _logPrefix, _sleepDelayBeforeNotify);

                        // Wait a while for application startup to finish...
                        ThreadExecutors.sleep(_sleepDelayBeforeNotify);
                    }

                    // Check real recipient availability
                    final Subscriptions subscriptions = SampManager.getSubscriptions(recipientId);
                    if (subscriptions != null) {
                        // Try tyo forward each waiting message
                        int messageIndex = 1;
                        final int nbOfMessages = _messages.size();
                        for (Message msg : _messages) {

                            // Check that the current message really match one of the real recipient SAMP capability
                            boolean subscriptionFound = false;
                            for (Object mType : subscriptions.keySet()) {
                                if (mType.toString().equalsIgnoreCase(msg.getMType())) {
                                    subscriptionFound = true;
                                    break;
                                }
                            }
                            if (subscriptionFound) {
                                // Forward the message
                                setState(ClientStubState.FORWARDING);
                                try {
                                    _connector.getConnection().notify(recipientId, msg);
                                    _logger.info("{}Forwarded message ({} / {}).", _logPrefix, messageIndex, nbOfMessages);
                                } catch (SampException se) {
                                    _logger.error("{}SAMP notication exception:", _logPrefix, se);
                                }
                            } else {
                                _logger.info("{}Could not find '{}' mType ... skipping.", _logPrefix, msg.getMType());
                            }
                            messageIndex++;
                        }

                        resetMessageQueue();
                    }
                } else {
                    _logger.info("{}No message to forward.", _logPrefix);
                }

                // Kill the stub client
                disconnect();
            }
        }
    }

    /**
     * Perform the event from the given root context.
     *
     * @see JobListener
     *
     * @param jobContext root context.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public void performJobEvent(final RootContext jobContext) {

        ProcessContext pCtx;

        switch (jobContext.getState()) {
            case STATE_FINISHED_OK:

                // JNLP process done
                _logger.info("{}JNLP execution status: {}\n{}",
                        _logPrefix, jobContext.getState(), jobContext.getRing().getContent("Ring buffer:\n"));

                pCtx = (ProcessContext) jobContext.getChildContexts().get(0);

                _logger.info("{}DONE (with status '{}').", _logPrefix, pCtx.getExitCode());

                // Reset job context
                setJobContextId(null);
                break;

            case STATE_FINISHED_ERROR:
                // JNLP process failed
                _logger.info("{}JNLP execution status: {}\n{}",
                        _logPrefix, jobContext.getState(), jobContext.getRing().getContent("Ring buffer:\n"));

                pCtx = (ProcessContext) jobContext.getChildContexts().get(0);

                _logger.info("{}DONE (with status '{}').", _logPrefix, pCtx.getExitCode());

                // JNLP process failed: clean up:
                cleanup(false);
                break;

            case STATE_CANCELED:
            case STATE_INTERRUPTED:
            case STATE_KILLED:
                // JNLP process failed
                _logger.info("{}JNLP execution status: {}\n{}",
                        _logPrefix, jobContext.getState(), jobContext.getRing().getContent("Ring buffer:\n"));

                // JNLP process failed: clean up:
                cleanup(false);
                break;

            default: // Otherwise do nothing
        }
    }

    /**
     * Perform the event from the given run context.
     *
     * @see JobListener
     *
     * @param jobContext root context.
     * @param runCtx current run context.
     */
    @Override
    public void performTaskEvent(final RootContext jobContext, final RunContext runCtx) {
        _logger.debug("{}job : {}", _logPrefix, runCtx);
    }

    /**
     * Perform the event from the given run context.
     *
     * @see JobListener
     *
     * @param jobContext root context.
     * @param runCtx current run context.
     *
     * @return boolean: true of the processing should continue, false if the job should be terminated.
     */
    @Override
    public boolean performTaskDone(final RootContext jobContext, final RunContext runCtx) {
        return runCtx.getState() == RunState.STATE_FINISHED_OK;
    }
}
