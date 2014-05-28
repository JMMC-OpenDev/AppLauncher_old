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

import fr.jmmc.smprsc.data.list.StubRegistry;
import fr.jmmc.smprsc.data.list.model.Category;
import fr.jmmc.smprsc.data.stub.StubMetaData;
import fr.jmmc.smprsc.data.stub.model.SampStub;
import fr.jmmc.smprun.preference.PreferenceKey;
import fr.jmmc.smprun.preference.Preferences;
import fr.jmmc.smprun.stub.ClientStub;
import fr.jmmc.smprun.stub.StubMonitor;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Start all known stubs.
 * 
 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public final class HubPopulator {

    /** Class logger */
    private static final Logger _logger = LoggerFactory.getLogger(HubPopulator.class.getName());
    /** HubPopulator singleton */
    private static HubPopulator _singleton = null;
    /* members */
    /** Client family  / client stub mapping */
    private EnumMap<Category, List<ClientStub>> _familyLists = new EnumMap<Category, List<ClientStub>>(Category.class);
    /** Client stub map keyed by application name */
    HashMap<String, ClientStub> _clientStubMap = new HashMap<String, ClientStub>(32);

    /**
     * Return the HubPopulator singleton
     * @return HubPopulator singleton
     */
    public static HubPopulator start() {
        if (_singleton == null) {
            _singleton = new HubPopulator();
        }
        return _singleton;
    }

    /**
     * Constructor: create meta data for SAMP applications
     */
    private HubPopulator() {

        // For each known category
        for (Category currentCategory : Category.values()) {

            // Forge the list of stub for the current category
            List<ClientStub> currentCategoryClientList = new ArrayList<ClientStub>();

            // For each application name of the category
            List<String> applicationNames = StubRegistry.getCategoryApplicationNames(currentCategory);
            for (String applicationName : applicationNames) {

                // If the user asked through preferences not to start all stubs
                final Preferences preferences = Preferences.getInstance();
                if (preferences.getPreferenceAsBoolean(PreferenceKey.START_SELECTED_STUBS)) {

                    // If the current application stub should not be created (i.e was not selected by the iser)
                    if (!preferences.isApplicationNameSelected(applicationName)) {
                        _logger.debug("Skipping unwanted '{}' application.", applicationName);
                        continue; // Skip stub creation
                    }
                }

                _logger.debug("Loading '{}' category's stub '{}' data from resource.", currentCategory.value(), applicationName);
                final ClientStub newClientStub = createClientStub(applicationName);
                currentCategoryClientList.add(newClientStub);
            }

            _familyLists.put(currentCategory, currentCategoryClientList);
        }

        _logger.info("configuration: {}", _familyLists);
    }

    /**
     * Create a new Client Stub using given arguments and store it in collections
     * 
     * @param applicationName application name
     * @return client stub 
     */
    private ClientStub createClientStub(final String applicationName) {

        SampStub data = StubMetaData.retrieveSampStubForApplication(applicationName);

        final ClientStub client = new ClientStub(data);
        client.addObserver(new StubMonitor(applicationName));

        _clientStubMap.put(applicationName, client);

        return client;
    }

    /**
     * @return true if initialization is done, false otherwise.
     */
    public static boolean isInitialized() {
        return (_singleton == null ? false : true);
    }

    /**
     * Return the client stub map keyed by application name.
     * @return client stub map keyed by application name
     */
    public static Map<String, ClientStub> getClientStubMap() {
        return start()._clientStubMap;
    }

    /**
     * Return the client stub given its name.
     * @param name application name to match
     * @return client stub or null if not found
     */
    public static ClientStub retrieveClientStub(final String name) {
        return start()._clientStubMap.get(name);
    }

    /** Properly disconnect connected clients */
    public static void disconnectAllStubs() {
        for (ClientStub client : start()._clientStubMap.values()) {
            client.disconnect();
        }
    }
}
