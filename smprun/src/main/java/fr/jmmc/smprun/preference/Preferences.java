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
package fr.jmmc.smprun.preference;

import fr.jmmc.jmcs.data.preference.MissingPreferenceException;
import fr.jmmc.jmcs.data.preference.PreferencesException;
import fr.jmmc.jmcs.util.FileUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage AppLauncher user's default values.
 *
 * @author Sylvain LAFRASSE
 */
public class Preferences extends fr.jmmc.jmcs.data.preference.Preferences {

    /**
     * Logger
     */
    private static final Logger _logger = LoggerFactory.getLogger(Preferences.class.getName());
    /**
     * Singleton instance
     */
    private static Preferences _instance = null;
    /**
     * Default selected application list
     */
    private static final List<String> _defaultSelectedApplicationList = Arrays.asList("Aspro2", "SearchCal", "LITpro", "OIFitsExplorer", "OImaging",
            "topcat", "Aladin", "ETC-42", "Cassis", "splat", "VOSpec",
            "AMDA", "OiDB", "Simbad", "VizieR", "VOSED");
    /**
     * Constant to detect that no application is deselected
     */
    public List<String> ALL_APPLICATIONS_SELECTED = null;

    /**
     * @return the singleton instance.
     */
    public static final synchronized Preferences getInstance() {
        // DO NOT MODIFY !!!
        if (_instance == null) {
            _instance = new Preferences();
        }

        return _instance;

        // DO NOT MODIFY !!!
    }

    @Override
    protected String getPreferenceFilename() {
        return "fr.jmmc.applauncher.properties";
    }

    @Override
    protected int getPreferencesVersionNumber() {
        return 1;
    }

    @Override
    protected void setDefaultPreferences() throws PreferencesException {
        // By default always consider it is the first time ever AppLauncher is started
        setDefaultPreference(PreferenceKey.FIRST_START_FLAG, true);
        // By default always start all stubs
        setDefaultPreference(PreferenceKey.START_SELECTED_STUBS, false);
        // By default always show exit warning
        setDefaultPreference(PreferenceKey.SHOW_EXIT_WARNING, true);
        // By default always ask user's permission before reorting an unknown application
        setDefaultPreference(PreferenceKey.SILENTLY_REPORT_FLAG, false);
        // By default always show JMMC and ESSENTIALS applications
        setDefaultPreference(PreferenceKey.SELECTED_APPLICATION_LIST, _defaultSelectedApplicationList);
        // By default no application should be used as beta
        setDefaultPreference(PreferenceKey.BETA_APPLICATION_LIST, new ArrayList<String>());
        // By default do not skip broadcasted messages
        setDefaultPreference(PreferenceKey.DISCARD_BROADCASTS_FLAG, false);
    }

    public List<String> getSelectedApplicationNames() {

        return getStringListPreference(PreferenceKey.SELECTED_APPLICATION_LIST);
    }

    private List<String> getStringListPreference(PreferenceKey preference) {
        List<String> stringList = ALL_APPLICATIONS_SELECTED;

        try {
            stringList = getPreferenceAsStringList(preference);
        } catch (MissingPreferenceException ex) {
            _logger.error("MissingPreferenceException :", ex);
        } catch (PreferencesException ex) {
            _logger.error("PreferencesException :", ex);
        }

        return stringList;
    }

    public boolean isApplicationNameSelected(String applicationName) {
        List<String> selectedApplicationNameList = getSelectedApplicationNames();
        final String applicationId = FileUtils.cleanupFileName(applicationName);

        if ((selectedApplicationNameList == ALL_APPLICATIONS_SELECTED) || (selectedApplicationNameList.contains(applicationId))) {
            return true;
        }

        return false;
    }

    public boolean isApplicationReleaseBeta(String applicationName) {

        List<String> betaApplicationNameList = ALL_APPLICATIONS_SELECTED;

        try {
            betaApplicationNameList = getPreferenceAsStringList(PreferenceKey.BETA_APPLICATION_LIST);
        } catch (MissingPreferenceException ex) {
            _logger.error("MissingPreferenceException :", ex);
        } catch (PreferencesException ex) {
            _logger.error("PreferencesException :", ex);
        }
        final String applicationId = FileUtils.cleanupFileName(applicationName);
        if ((betaApplicationNameList == ALL_APPLICATIONS_SELECTED) || (betaApplicationNameList.contains(applicationId))) {
            return true;
        }
        return false;
    }

    public String getApplicationCliPath(String applicationName) {
        final String applicationId = FileUtils.cleanupFileName(applicationName);
        final String cliPath = getPreference(PreferenceKey.APPLICATION_CLI_PATH_PREFIX + applicationId, true); // Does not thrown exception on missing value
        return cliPath;
    }

    public void setApplicationCliPath(String applicationName, String cliPath) {
        final String applicationId = FileUtils.cleanupFileName(applicationName);
        try {
            setPreference(PreferenceKey.APPLICATION_CLI_PATH_PREFIX + applicationId, cliPath);
        } catch (PreferencesException ex) {
            _logger.error("Could not set '{}' application command-line path to '{}' : ", applicationName, cliPath, ex);
        }
    }

    public static void main(String[] args) {

        final Preferences prefs = Preferences.getInstance();

        String currentPrefs = prefs.dumpCurrentProperties();
        System.out.println("---------------\n" + "Current Preferences Dump :\n" + currentPrefs + "\n---------------");

        try {
            List<String> list = prefs.getPreferenceAsStringList(PreferenceKey.SELECTED_APPLICATION_LIST);
            System.out.println("Selected Application List : " + list + "\n---------------");
        } catch (MissingPreferenceException ex) {
            System.out.println("MissingPreferenceException = " + ex);
        } catch (PreferencesException ex) {
            System.out.println("PreferencesException = " + ex);
        }

        try {
            List<String> list = prefs.getPreferenceAsStringList(PreferenceKey.BETA_APPLICATION_LIST);
            System.out.println("Beta Application List : " + list + "\n---------------");
        } catch (MissingPreferenceException ex) {
            System.out.println("MissingPreferenceException = " + ex);
        } catch (PreferencesException ex) {
            System.out.println("PreferencesException = " + ex);
        }
    }
}
