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

import fr.jmmc.jmcs.util.jaxb.JAXBFactory;
import fr.jmmc.jmcs.util.jaxb.JAXBUtils;
import fr.jmmc.smprsc.data.list.model.Category;
import fr.jmmc.smprsc.data.list.model.Family;
import fr.jmmc.smprsc.data.list.model.SampStubList;
import fr.jmmc.smprsc.data.stub.StubMetaData;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import fr.jmmc.jmcs.util.CollectionUtils;
import fr.jmmc.jmcs.util.ResourceUtils;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * smprsc access singleton.
 *
 * @author Sylvain LAFRASSE
 */
public class StubRegistry {

    /** Logger - get from given class name */
    private static final Logger _logger = LoggerFactory.getLogger(StubRegistry.class.getName());
    /** Internal singleton instance holder */
    private static StubRegistry _singleton = new StubRegistry();
    /** package name for JAXB generated code */
    private final static String SAMP_STUB_LIST_JAXB_PACKAGE = "fr.jmmc.smprsc.data.list.model";
    /** SAMP stub application files path */
    public static final String SAMP_STUB_LIST_FILE_PATH = "fr/jmmc/smprsc/registry/__index__.xml";
    /** internal JAXB Factory */
    private final JAXBFactory jf;
    /** Known application names list */
    Set<String> _knownApplicationNames;
    /** Category's application names cache */
    Map<Category, List<String>> _categoryApplicationNames;
    /** Category's visible application names cache */
    Map<Category, List<String>> _categoryVisibleApplicationNames;

    /**
     * Private constructor called at static initialization.
     *
     * @throws IllegalStateException if IO, JAXBException occurs during SampStubList retrieval
     */
    private StubRegistry() {
        // Start JAXB
        jf = JAXBFactory.getInstance(SAMP_STUB_LIST_JAXB_PACKAGE);

        // Try to load __index__.xml resource
        final URL fileURL = ResourceUtils.getResource(SAMP_STUB_LIST_FILE_PATH);
        final SampStubList sampStubList;
        try {
            sampStubList = (SampStubList) JAXBUtils.loadObject(fileURL, jf);
        } catch (IOException ioe) {
            throw new IllegalStateException("Load failure on " + fileURL, ioe);
        }

        // Members creation
        _knownApplicationNames = new HashSet<String>(32);
        _categoryApplicationNames = new EnumMap<Category, List<String>>(Category.class);
        _categoryVisibleApplicationNames = new EnumMap<Category, List<String>>(Category.class);

        // Cache all application names for each category
        for (Family family : sampStubList.getFamilies()) {

            // Get the list of application name for the current category
            final Category currentCategory = family.getCategory();
            final List<String> fullCategoryApplicationNameList = family.getApplications();
            _categoryApplicationNames.put(currentCategory, fullCategoryApplicationNameList);

            // Build the list of visible applications for the current category
            List<String> visibleCategoryApplications = new ArrayList<String>();
            for (String applicationName : fullCategoryApplicationNameList) {

                _knownApplicationNames.add(applicationName);

                if (StubMetaData.getEmbeddedApplicationIcon(applicationName) != null) {
                    visibleCategoryApplications.add(applicationName);
                }
            }

            if (visibleCategoryApplications.size() > 0) {
                _categoryVisibleApplicationNames.put(currentCategory, visibleCategoryApplications);
            }
        }
    }

    /**
     * @return true if the given application name is known, false otherwise
     */
    public static boolean isApplicationKnown(String applicationName) {
        final boolean isApplicationKnown = _singleton._knownApplicationNames.contains(applicationName);
        return isApplicationKnown;
    }

    /**
     * @return the list of SAMP stub application names for the given category, null otherwise.
     */
    public static List<String> getCategoryApplicationNames(Category category) {
        return _singleton._categoryApplicationNames.get(category);
    }

    /**
     * @return the list of SAMP stub visible application names for the given category, null otherwise.
     */
    public static List<String> getCategoryVisibleApplicationNames(Category category) {
        return _singleton._categoryVisibleApplicationNames.get(category);
    }

    /**
     * Main entry point
     *
     * @param args command line arguments (open file ...)
     */
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static void main(final String[] args) {

        List<String> list = null;

        for (Category category : Category.values()) {

            System.out.println("-------------------------------------------------------");
            System.out.println("category = " + category.value());
            System.out.println("-------------------------------------------------------");

            list = StubRegistry.getCategoryVisibleApplicationNames(category);
            System.out.println("Visible apps : " + CollectionUtils.toString(list, ", ", "{", "}"));

            list = StubRegistry.getCategoryApplicationNames(category);
            System.out.println("Application paths : " + CollectionUtils.toString(list, ", ", "{", "}"));

            System.out.println("");
        }

        System.out.println("-------------------------------------------------------");
        String[] applicationNames = {"SearchCal", "Aladin", "toto"};
        for (String string : applicationNames) {
            System.out.println("Application '" + string + "' is" + (isApplicationKnown(string) ? " " : " NOT ") + "known.");
        }
        System.out.println("-------------------------------------------------------");
    }
}
/*___oOo___*/
