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

import fr.jmmc.jmcs.data.preference.PreferencesException;
import fr.jmmc.jmcs.util.FileUtils;
import fr.jmmc.smprsc.data.list.ApplicationListSelectionPanel;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sylvain LAFRASSE
 */
public class ApplicationListSelectionView extends ApplicationListSelectionPanel implements Observer {

    /** Logger - get from given class name */
    private static final Logger _logger = LoggerFactory.getLogger(ApplicationListSelectionPanel.class.getName());
    private final Preferences _preferences;

    public ApplicationListSelectionView() {
        super();
        _preferences = Preferences.getInstance();
    }

    @Override
    public void init() {

        super.init();

        update(null, null);
        _preferences.addObserver(this);
    }

    @Override
    public void update(Observable observable, Object parameter) {

        List<String> selectedApplicationList = _preferences.getSelectedApplicationNames();
        _logger.debug("Preferenced list of selected applications updated : {}", selectedApplicationList);

        if (selectedApplicationList != null) {
            setCheckedApplicationNames(selectedApplicationList);
        }
    }

    /**
     * @return A panel describing the application of given name.
     * @param applicationName application name to describe.
     */
    public JScrollPane retrieveDescriptionPanelForApplication(String applicationName) {
        fillApplicationDescriptionPane(applicationName);
        return _descriptionScrollPane;
    }

    @Override
    protected void checkedApplicationChanged(List<String> checkedApplicationList) {
        _logger.debug("New list of SELECTED applications received : {}", checkedApplicationList);
        saveStringListPreference(PreferenceKey.SELECTED_APPLICATION_LIST, checkedApplicationList);
    }

    @Override
    protected boolean isApplicationBetaJnlpUrlInUse(String applicationName) {
        return _preferences.isApplicationReleaseBeta(applicationName);
    }

    @Override
    protected void betaApplicationChanged(List<String> betaApplicationList) {
        _logger.debug("New list of BETA applications received : '{}'.", betaApplicationList);
        saveStringListPreference(PreferenceKey.BETA_APPLICATION_LIST, betaApplicationList);
    }

    @Override
    protected void applicationCliPathChanged(String applicationName, String cliPath) {
        _logger.debug("New CLI path for application '{}' received : '{}'.", applicationName, cliPath);
        _preferences.setApplicationCliPath(applicationName, cliPath);
    }

    @Override
    protected String applicationCliPath(String applicationName) {

        final String cliPath = _preferences.getApplicationCliPath(applicationName);
        _logger.debug("Retrieved CLI path for application '{}' :  '{}'.", applicationName, cliPath);

        if (cliPath == null) {
            return "";
        }
        return cliPath;
    }

    private static void cleanApplicationNameList(final List<String> applicationNameList) {
        for (int i = 0; i < applicationNameList.size(); i++) {
            String applicationName = applicationNameList.get(i);
            final String applicationId = FileUtils.cleanupFileName(applicationName);
            applicationNameList.set(i, applicationId);
        }
    }

    private void saveStringListPreference(PreferenceKey preference, List<String> stringList) {

        if (stringList == null) {
            return;
        }

        cleanApplicationNameList(stringList);

        try {
            _preferences.setPreference(preference, stringList);
        } catch (PreferencesException ex) {
            _logger.error("PreferencesException :", ex);
        }
    }
}
