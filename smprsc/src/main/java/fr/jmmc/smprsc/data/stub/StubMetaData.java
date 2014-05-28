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

import fr.jmmc.jmcs.data.preference.Preferences;
import fr.jmmc.jmcs.data.preference.PreferencesException;
import fr.jmmc.jmcs.gui.util.SwingUtils;
import fr.jmmc.jmcs.util.jaxb.JAXBFactory;
import fr.jmmc.jmcs.util.jaxb.JAXBUtils;
import fr.jmmc.jmcs.util.jaxb.XmlBindException;
import fr.jmmc.jmcs.network.http.Http;
import fr.jmmc.jmcs.network.http.PostQueryProcessor;
import fr.jmmc.jmcs.network.interop.SampMetaData;
import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.jmcs.util.ResourceUtils;
import fr.jmmc.smprsc.data.stub.model.SampStub;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.ImageIcon;
import org.apache.commons.httpclient.methods.PostMethod;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Subscriptions;
import fr.jmmc.jmcs.util.concurrent.ThreadExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Real SAMP application meta data older, that can report to JMMC central registry if not referenced yet.
 *
 * @author Sylvain LAFRASSE.
 */
public class StubMetaData {

    // Constants
    /** Package name for JAXB generated code */
    private final static String STUB_DATA_MODEL_JAXB_PATH = "fr.jmmc.smprsc.data.stub.model";
    /** Base URL of the JMMC SAMP application meta data repository */
    private final static String REGISTRY_BASE_URL = "http://jmmc.fr/~smprun/stubs/";//"http://jmmc.fr/~lafrasse/stubs/";
    /** JMMC SAMP application meta data repository submission form name */
    private final static String REGISTRY_SUBMISSION_FORM_NAME = "push.php";
    /** JMMC SAMP application meta data repository directory containing all stubs definition files */
    private static final String SAMP_STUB_REGISTRY_DIRECTORY = "registry/";
    /** Resource directory containing all SAMP application files */
    private final static String SAMP_STUB_RESOURCE_DIRECTORY = "fr/jmmc/smprsc/registry/";
    /** File extension of the JMMC SAMP application meta data file format */
    private final static String SAMP_STUB_FILE_EXTENSION = ".xml";
    /** Application icon files extension */
    private final static String SAMP_STUB_ICON_FILE_EXTENSION = ".png";
    // Statics
    /** Logger */
    private final static Logger _logger = LoggerFactory.getLogger(StubMetaData.class.getName());
    /** JAXB initialization */
    private final static JAXBFactory _jaxbFactory = JAXBFactory.getInstance(STUB_DATA_MODEL_JAXB_PATH);
    /** Loaded SampStub cache */
    private final static Map<String, SampStub> _cachedSampStubs = new HashMap<String, SampStub>();
    // Members
    /** SAMP application meta data container */
    private final SampStub _data = new SampStub();
    /** Real application exact name */
    private final String _applicationName;
    /** Cleaned application exact name */
    private final String _applicationId;
    /** Real application SAMP meta data */
    private final Metadata _sampMetaData;
    /** Real application SAMP mTypes */
    private final Subscriptions _sampSubscriptions;

    /**
     * Constructor.
     *
     * @param metadata SAMP Meta data
     * @param subscriptions SAMP mTypes
     */
    public StubMetaData(Metadata metadata, Subscriptions subscriptions) {

        _logger.debug("Serializing SAMP application meta-data.");

        _applicationName = metadata.getName();
        _applicationId = FileUtils.cleanupFileName(_applicationName);
        _data.setUid(_applicationId);
        _sampMetaData = metadata; // Should clone it instead, but clone() is not implemented in jSAMP
        _sampSubscriptions = subscriptions;
    }

    public static SampStub retrieveSampStubForApplication(String applicationName) {

        String applicationId = FileUtils.cleanupFileName(applicationName);
        SampStub sampStub = _cachedSampStubs.get(applicationId);
        if (sampStub == null) {

            sampStub = loadSampStubForApplication(applicationId);
            _cachedSampStubs.put(applicationId, sampStub);
        }

        return sampStub;
    }

    /**
     * Try to load embedded icon for given application name.
     *
     * @param applicationName the application name of the sought icon.
     * @return the icon if found, null otherwise.
     */
    public static ImageIcon getEmbeddedApplicationIcon(String applicationName) {

        ImageIcon icon = null;

        // Forge icon resource path
        final String applicationId = FileUtils.cleanupFileName(applicationName);
        final String iconResourcePath = SAMP_STUB_RESOURCE_DIRECTORY + applicationId + SAMP_STUB_ICON_FILE_EXTENSION;

        try {
            // Try to load application icon resource
            final URL fileURL = ResourceUtils.getResource(iconResourcePath);
            if (fileURL != null) {
                icon = new ImageIcon(fileURL);
            }
        } catch (IllegalStateException ise) {
            _logger.warn("Could not find '{}' embedded icon.", applicationName);
        }

        return icon;
    }

    /**
     * @return application complete description as a String.
     */
    public String getApplicationDescription() {
        serializeMetaData(null, null); // Report without further data
        return marshallApplicationDescription();
    }

    /**
     * Upload application complete description to JMMC central repository (only if not known yet).
     *
     * @param preferenceInstance the jMCS Preference object in which silent report flag is stored
     * @param preferenceName the jMCS Preference key that point to the silent report flag value
     */
    public void reportToCentralRepository(final Preferences preferenceInstance, final String preferenceName) {

        // Make all the network stuff run in the background
        ThreadExecutors.getGenericExecutor().submit(new Runnable() {
            AtomicBoolean shouldPhoneHome = new AtomicBoolean(true);
            ApplicationReportingForm dialog = null;

            @Override
            public void run() {

                // If the current application does not exist in the central repository
                if (isNotKnownYet()) {

                    // If user wants to explicitly choose to report or not
                    final boolean silently = preferenceInstance.getPreferenceAsBoolean(preferenceName);
                    if (!silently) {
                        // Ask user if it is OK to phone application description back home
                        SwingUtils.invokeAndWaitEDT(new Runnable() {
                            /**
                             * Synchronized by EDT
                             */
                            @Override
                            public void run() {
                                _logger.debug("Showing report window for '{}' application", _applicationName);
                                dialog = new ApplicationReportingForm(_applicationName);
                                shouldPhoneHome.set(dialog.shouldSubmit());
                                final boolean shouldSilentlySubmit = dialog.shouldSilentlySubmit();
                                if (shouldSilentlySubmit != silently) {
                                    try {
                                        preferenceInstance.setPreference(preferenceName, shouldSilentlySubmit);
                                        preferenceInstance.saveToFile();
                                    } catch (PreferencesException ex) {
                                        _logger.warn("Could not save silent report state to preference : ", ex);
                                    }
                                }
                            }
                        });
                    } else {
                        _logger.info("Silently reporting '{}' unknown application", _applicationName);
                    }

                    // If the user agreed to report unknown app
                    if (shouldPhoneHome.get()) {
                        if (dialog == null) { // Silently
                            serializeMetaData(null, null); // Report without further data
                        } else { // Explicitly
                            final String userEmail = dialog.getUserEmail();
                            final String applicationURL = dialog.getApplicationURL();
                            serializeMetaData(userEmail, applicationURL);
                        }

                        final String xmlRepresentation = marshallApplicationDescription();
                        postXMLToRegistry(xmlRepresentation);
                    }
                }
            }
        });
    }

    /**
     * @return true if the 'name' application is unknown, false otherwise.
     */
    private boolean isNotKnownYet() {

        _logger.info("Querying JMMC SAMP application registry for '{}' application ...", _applicationName);
        boolean unknownApplicationFlag = false; // In order to skip later application reporting if registry querying goes wrong

        try {
            final String path = REGISTRY_BASE_URL + SAMP_STUB_REGISTRY_DIRECTORY + _applicationId + SAMP_STUB_FILE_EXTENSION;

            final URI applicationDescriptionFileURI = Http.validateURL(path);
            final String result = Http.download(applicationDescriptionFileURI, false); // Use the multi-threaded HTTP client
            _logger.debug("HTTP response : '{}'.", result);

            // Decipher whether the meta-data is alredy registered or not
            unknownApplicationFlag = (result == null) || (result.length() == 0);
            _logger.info("SAMP application '{}' {}found in JMMC registry.", _applicationName, (unknownApplicationFlag ? "not " : ""));

        } catch (IOException ioe) {
            _logger.error("Cannot get SAMP application meta-data : ", ioe);
        }

        return unknownApplicationFlag;
    }

    /**
     * Load SampStub object for the given application name.
     *
     * @param applicationName name of application
     * @return the associated samp stub
     * @throws IllegalArgumentException if applicationName is null
     * @throws IllegalStateException if io exception occurs for data retrieval
     */
    private static SampStub loadSampStubForApplication(final String applicationName) {

        if (applicationName == null) {
            throw new IllegalArgumentException("applicationName");
        }

        String applicationId = FileUtils.cleanupFileName(applicationName);
        final String path = SAMP_STUB_RESOURCE_DIRECTORY + applicationId + SAMP_STUB_FILE_EXTENSION;

        // Note : use input stream to avoid JNLP offline bug with URL (Unknown host exception)
        final URL resourceURL = ResourceUtils.getResource(path);

        try {
            return (SampStub) JAXBUtils.loadObject(resourceURL, _jaxbFactory);
        } catch (IOException ioe) {
            throw new IllegalStateException("Load failure on " + resourceURL, ioe);
        }
    }

    /**
     * @param userEmail
     * @param applicationURL
     */
    private void serializeMetaData(String userEmail, String applicationURL) {

        fr.jmmc.smprsc.data.stub.model.Metadata tmp;

        // Add user given inputs
        if ((userEmail != null) && (userEmail.length() > 0)) {
            tmp = new fr.jmmc.smprsc.data.stub.model.Metadata("email", userEmail);
            _data.getMetadatas().add(tmp);
        }
        if ((applicationURL != null) && (applicationURL.length() > 0)) {
            tmp = new fr.jmmc.smprsc.data.stub.model.Metadata(SampMetaData.HOMEPAGE_URL.id(), applicationURL);
            _data.getMetadatas().add(tmp);
        }

        // Serialize all SAMP meta data
        for (Object key : _sampMetaData.keySet()) {
            tmp = new fr.jmmc.smprsc.data.stub.model.Metadata(key.toString(), _sampMetaData.get(key).toString());
            _data.getMetadatas().add(tmp);
        }

        // Serialize all SAMP mTypes
        for (Object subscription : _sampSubscriptions.keySet()) {
            _data.getSubscriptions().add(subscription.toString());
        }
    }

    /**
     * @param xml
     */
    private void postXMLToRegistry(final String xml) {

        // Check parameter vailidty
        if (xml == null) {
            _logger.warn("Something went wrong while serializing SAMP application '{}' meta-data ... aborting report.", _applicationName);
            return;
        }

        _logger.info("Sending JMMC SAMP application '{}' XML description to JMMC registry ...", _applicationName);

        try {
            final URI uri = Http.validateURL(REGISTRY_BASE_URL + REGISTRY_SUBMISSION_FORM_NAME);
            // use the multi threaded HTTP client
            final String result = Http.post(uri, false, new PostQueryProcessor() {
                /**
                 * Process the given post method to define its HTTP input fields
                 *
                 * @param method post method to complete
                 * @throws IOException if any IO error occurs
                 */
                @Override
                public void process(final PostMethod method) throws IOException {
                    method.addParameter("uid", _applicationId);
                    method.addParameter("xmlSampStub", xml);
                }
            });

            _logger.debug("HTTP response : '{}'.", result);

            // Parse result for failure
            if (result != null) {
                _logger.info("Sent SAMP application '{}' XML description to JMMC regitry.", _applicationName);
            } else {
                _logger.warn("SAMP application meta-data were not sent properly.");
            }

        } catch (IOException ioe) {
            _logger.error("Cannot send SAMP application meta-data : ", ioe);
        }
    }

    /**
     * Marshall current sampStup to a string representation.
     *
     * @return the string representation of marshalled sampStub
     * @throws XmlBindException if a JAXBException was caught while creating an marshaller
     * @throws
     */
    private String marshallApplicationDescription() throws XmlBindException {
        final StringWriter stringWriter = new StringWriter(4096); // 4K buffer

        JAXBUtils.saveObject(stringWriter, _data, _jaxbFactory);

        final String xml = stringWriter.toString();
        _logger.debug("Generated SAMP application '{}' XML description :\n{}", _applicationName, xml);

        return xml;
    }
}
