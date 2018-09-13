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
import fr.jmmc.jmcs.network.interop.SampCapability;
import fr.jmmc.jmcs.network.interop.SampManager;
import fr.jmmc.jmcs.network.interop.SampMetaData;
import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.smprsc.data.list.StubRegistry;
import fr.jmmc.smprsc.data.stub.StubMetaData;
import fr.jmmc.smprun.preference.PreferenceKey;
import fr.jmmc.smprun.preference.Preferences;
import fr.jmmc.smprun.stub.ClientStub;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.astrogrid.samp.Client;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.gui.SubscribedClientListModel;
import fr.jmmc.jmcs.util.concurrent.ThreadExecutors;
import fr.jmmc.smprsc.data.stub.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitor hub connections (register / unregister) for MTypes corresponding to all client stubs.
 * 
 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public final class HubMonitor {

    /** Logger */
    private static final Logger _logger = LoggerFactory.getLogger(HubMonitor.class.getName());
    /** HubMonitor singleton */
    private static final HubMonitor INSTANCE = new HubMonitor();
    /* members  */
    /** mType array containing all unique MTypes handled by all applications */
    private final String[] _mTypesStrings;
    /** Registered SAMP recipients corresponding to mType array */
    private final SubscribedClientListModel _capableClients;
    /** Dedicated thread executor */
    private final ThreadExecutors _executor;
    /** List of unique client stubs needed to be started ASAP */
    private Set<ClientStub> _clientStubsToStart = new LinkedHashSet<ClientStub>();
    /** Map of sniffed real application meta-data */
    private HashMap<String, StubMetaData> _sniffedRealApplications = new HashMap<String, StubMetaData>();

    /**
     * Return the HubMonitor singleton.
     * 
     * @return HubMonitor singleton 
     */
    public static HubMonitor getInstance() {
        return INSTANCE;
    }

    /** Private constructor */
    private HubMonitor() {
        _logger.info("HubMonitor()");

        _mTypesStrings = ComputeMTypeArray();
        _capableClients = SampManager.createSubscribedClientListModel(_mTypesStrings);

        // Monitor any modification to the capable clients list
        _capableClients.addListDataListener(new ListDataListener() {
            @Override
            public void contentsChanged(final ListDataEvent e) {
                _logger.trace("ListDataListener.contentsChanged");
                handleHubEvent();
            }

            @Override
            public void intervalAdded(final ListDataEvent e) {
                _logger.trace("ListDataListener.intervalAdded");
                handleHubEvent();
            }

            @Override
            public void intervalRemoved(final ListDataEvent e) {
                _logger.trace("ListDataListener.intervalRemoved");
                // note: this event is never invoked by JSamp code (1.3) !
                handleHubEvent();
            }
        });

        // Create deidcated thread executor:
        _executor = ThreadExecutors.getSingleExecutor(getClass().getSimpleName() + "ThreadPool");

        // Analize already registered samp clients
        handleHubEvent();
    }

    public boolean isIdle() {
        return (_executor.getExecutor().getActiveCount() == 0 ? true : false);
    }

    /**
     * Compute MType array to listen to.
     * 
     * @return A String array containing all listened mTypes.
     */
    private static String[] ComputeMTypeArray() {
        final HashSet<String> mTypesSet = new HashSet<String>();

        for (SampCapability capability : SampCapability.values()) {
            if (!capability.isFlagged()) {
                mTypesSet.add(capability.mType());
            }
        }

        _logger.info("monitoring MTypes: {}", mTypesSet);

        // Get a dynamic list of SAMP clients able to respond to the specified capability.
        final String[] mTypesStrings = new String[mTypesSet.size()];
        mTypesSet.toArray(mTypesStrings);

        return mTypesStrings;
    }

    /**
     * Waits for stubs startup to complete
     */
    public void waitForStubsStartup() {
        while (isThereSomeStubsLeftToStart()) {
            _logger.debug("Waiting for stubs startup to complete...");
            try {
                Thread.sleep(100L);
            } catch (InterruptedException ie) {
                _logger.error("Interrupted while waiting for stubs startup to complete.", ie);
            }
        }
    }

    /**
     * @return true if there is still stubs to be started 
     */
    private boolean isThereSomeStubsLeftToStart() {
        return _clientStubsToStart.size() > 0;
    }

    /**
     * Process hub clients in background using the dedicated thread executor
     */
    private void handleHubEvent() {

        if (!ThreadExecutors.isRunning()) {
            return;
        }

        // First copy the content of the list model to avoid concurrency issues
        final int size = _capableClients.getSize();
        final Client[] clients = new Client[size];
        for (int i = 0; i < size; i++) {
            clients[i] = (Client) _capableClients.getElementAt(i);
        }

        _executor.submit(new Runnable() {
            /**
             * Process hub information about registered client 
             */
            @Override
            public void run() {
                loopOverHubClients(clients);
            }
        });
    }

    /**
     * Handle changes on registered SAMP recipients:
     * 
     * @param clients current hub registered clients
     */
    private void loopOverHubClients(final Client[] clients) {

        if (!ThreadExecutors.isRunning()) {
            _logger.info("Discarding call - shutdown in progress.");
            return;
        }

        _logger.debug("loopOverHubClients() - start");

        final Collection<ClientStub> clientStubList = HubPopulator.getClientStubMap().values();
        for (ClientStub stub : clientStubList) {

            final String stubName = stub.getApplicationName();
            boolean recipientFound = false;

            // Check each registered clients for the sought recipient name
            for (Client client : clients) {
                final Metadata md = client.getMetadata();
                
                if (md != null) {
                    final String clientName = md.getName();

                    if (clientName.matches(stubName)) {
                        recipientFound = true;

                        final String recipientId = client.getId();

                        // If current client is one of our STUB
                        final Object clientIsAStubFlag = md.get(SampMetaData.getStubMetaDataId(clientName));
                        if (SampMetaData.STUB_TOKEN.equals(clientIsAStubFlag)) {
                            _logger.debug("Found STUB recipient '{}' [{}]: leaving it alone.", clientName, recipientId);
                        } else {

                            if (stub.isConnected()) {
                                _logger.info("Found REAL recipient '{}' [{}]: running STUB trickery !", clientName, recipientId);

                                // Retrieve real application metadata for sniffing purpose
                                retrieveRealRecipientMetadata(client);

                                // Perform callback on client stub in background
                                handleNewRealRecipientDetection(stub, recipientId);
                            } else {
                                _logger.info("Found REAL recipient '{}' [{}]: but the STUB is already disconnected.", clientName, recipientId);
                            }
                        }

                        // Do not exit from loop as we can have two SAMP clients having the same application name: real and stub for example.
                    }
                }
            }

            // If no real nor stub recipient found for application name
            if (!recipientFound) {

                if (stub.isConnected()) {
                    // Happens for web applications without SAMP capability. If the stub is already connected, a client was necessarly found.
                    if (stub.getExecutionType() == Type.WEB) {
                        _logger.debug("Found NO recipient at all for '{}' (web application).", stubName);
                    } else {
                        // Could happen until the Samp clients are updated (one event per client) ! 
                        // If the stub is already connected, a client was necesserarly found.
                        _logger.debug("Found NO recipient at all for '{}': but the STUB is already connected.", stubName);
                    }
                } else {
                    _logger.debug("Found NO recipient at all for '{}': scheduling corresponding STUB startup.", stubName);

                    // Schedule stub for startup (by adding it to the unique set of client stubs to start asap)
                    _clientStubsToStart.add(stub);
                }
            }
        }

        if (isThereSomeStubsLeftToStart()) {
            _logger.info("Stub recipients waiting to start: {}", _clientStubsToStart);

            // Do launch one client stub at a time (hub will then send one registration event that will cause this method to be invoked again soon for those left)
            final Iterator<ClientStub> it = _clientStubsToStart.iterator();
            if (it.hasNext()) {
                final ClientStub first = it.next();

                _logger.info("Starting STUB recipient '{}'.", first);
                first.connect();

                // Remove this one from the waiting queue
                it.remove();
            }
        }

        // Check each registered clients for unknown applications
        for (Client client : clients) {
            retrieveRealRecipientMetadata(client);
        }
        _logger.debug("loopOverHubClients() - done");
        StatusBar.show("all applications started.");
    }

    /**
     * Process application registration in background using the generic thread executor
     * 
     * @param stub client stub to invoke
     * @param recipientId recipient identifier of the real application 
     */
    private void handleNewRealRecipientDetection(final ClientStub stub, final String recipientId) {

        ThreadExecutors.getGenericExecutor().submit(new Runnable() {
            /**
             * Process application registration using dedicated thread (may sleep for few seconds ...)
             */
            @Override
            public void run() {
                stub.forwardMessagesToRealRecipient(recipientId);
            }
        });
    }

    /**
     * Collect all real application meta-data.
     * 
     * @param client the real application
     */
    private void retrieveRealRecipientMetadata(final Client client) {
        final Metadata md = client.getMetadata();
        if (md != null) {
            final String clientName = md.getName();

            final String applicationId = FileUtils.cleanupFileName(clientName);

            // If the curent application is not in the registry yet
            if (!StubRegistry.isApplicationKnown(applicationId)) {
                _logger.info("Detected an unknown application '{}'.", clientName);

                // TODO : store previously dismissed apps in preference ?

                if (!_sniffedRealApplications.containsKey(clientName)) {
                    _logger.info("Sniffed new real application '{}': backed up its metadata and subscriptions.", clientName);

                    final StubMetaData stubMetaData = new StubMetaData(md, client.getSubscriptions());
                    _sniffedRealApplications.put(clientName, stubMetaData);
                    stubMetaData.reportToCentralRepository(Preferences.getInstance(), PreferenceKey.SILENTLY_REPORT_FLAG.toString());
                }
            }
        }
    }
}
