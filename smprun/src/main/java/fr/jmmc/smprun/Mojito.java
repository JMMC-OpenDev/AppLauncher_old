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

import ch.qos.logback.classic.Level;
import fr.jmmc.jmcs.gui.component.MessagePane;
import fr.jmmc.jmcs.logging.LoggingService;
import fr.jmmc.jmcs.network.interop.SampManager;
import fr.jmmc.jmcs.network.interop.SampMetaData;
import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.smprsc.data.list.StubRegistry;
import fr.jmmc.smprsc.data.list.model.Category;
import fr.jmmc.smprsc.data.stub.StubMetaData;
import fr.jmmc.smprsc.data.stub.model.SampStub;
import fr.jmmc.smprun.stub.ClientStub;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import org.astrogrid.samp.Client;
import org.astrogrid.samp.Metadata;
import org.astrogrid.samp.Subscriptions;

/**
 * Mojito : the AppLauncher Registry tester.
 * Start each known application, then dump its meta data to detect changes across time.
 *
 * @author Sylvain LAFRASSE.
 */
public class Mojito {

    /**
     * Main entry point.
     * @param args command line arguments
     */
    public static void main(final String[] args) throws TimeoutException {
        LoggingService.getInstance();
        LoggingService.setLoggerLevel(LoggingService.getJmmcLogger(), Level.OFF);
        LoggingService.setLoggerLevel(LoggingService.JMMC_APP_LOG, Level.OFF);
        LoggingService.setLoggerLevel(LoggingService.JMMC_STATUS_LOG, Level.OFF);
        LoggingService.setLoggerLevel("fr.jmmc.smprun", Level.OFF);

        System.out.println("-------------------------------------------------------");
        System.out.println("------ Mojito : the AppLauncher Registry tester -------");
        System.out.println("-------------------------------------------------------");

        // Build temporary output directory
        final Date today = new Date();
        final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_HH_mm_SS");
        final String date = DATE_FORMAT.format(today);
        final String tempDirPath = FileUtils.getTempDirPath();
        final File outputDirectory = new File(tempDirPath + "/AppLauncherRegistryTester_" + date);
        if (!outputDirectory.mkdir()) {
            System.exit(-1);
        }
        System.out.println("outputDirectory :");
        System.out.println(outputDirectory);

        System.out.println("-------------------------------------------------------");
        System.out.println("-------------------------------------------------------");

        for (final Category category : Category.values()) {
            final List<String> list = StubRegistry.getCategoryApplicationNames(category);
            for (final String applicationName : list) {

                System.out.println("Testing '" + applicationName + "' application:");
                final SampStub applicationStub = StubMetaData.retrieveSampStubForApplication(applicationName);
                final ClientStub clientStub = new ClientStub(applicationStub);

                System.out.println("Launching ...");
                clientStub.launchRealApplication();

                int retry = 5;
                do {

                    System.out.println("Waiting (" + retry + " retry left) ...");
                    try {
                        Thread.sleep(3000L); // milliseconds
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Mojito.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                    }

                    final List<String> clientIdsForName = SampManager.getClientIdsForName(applicationName);
                    if (clientIdsForName == null) {
                        System.out.println("WARNING : Client ID not found !!!");
                        continue;
                    }

                    for (String id : clientIdsForName) {
                        final Client client = SampManager.getClient(id);
                        final Metadata md = client.getMetadata();
                        if (md != null) {
                            final Subscriptions subscriptions = client.getSubscriptions();
                            final String clientName = md.getName();
                            final String applicationId = FileUtils.cleanupFileName(clientName);

                            final StubMetaData stubMetaData = new StubMetaData(md, subscriptions);
                            final Object clientIsAStubFlag = md.get(SampMetaData.getStubMetaDataId(clientName));
                            if (!SampMetaData.STUB_TOKEN.equals(clientIsAStubFlag)) {

                                final String applicationDescription = stubMetaData.getApplicationDescription();
                                System.out.println("MetaData[" + clientName + "] :");
                                System.out.println(applicationDescription);

                                System.out.println("Saving ...");
                                final Writer file = FileUtils.openFile(outputDirectory.getAbsolutePath() + "/" + clientName + ".xml");
                                try {
                                    file.write(applicationDescription);
                                    FileUtils.closeFile(file);
                                } catch (IOException ex) {
                                    Logger.getLogger(Mojito.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                                }

                                retry = -1;
                                break;
                            }
                        }
                    }

                    retry--;
                    if (retry == 0) {
                        final boolean retryAgain = MessagePane.showConfirmMessage("Retry again ???");
                        if (retryAgain) {
                            retry = 5;
                        }
                    }
                } while (retry > 0);

                System.out.println("Killing ...");
                clientStub.killRealApplication();
                System.out.println("-------------------------------------------------------");
            }
        }

        System.out.println("-------------------------------------------------------");
        System.out.println("-------------------------------------------------------");
        System.out.println("DONE");
    }
}
