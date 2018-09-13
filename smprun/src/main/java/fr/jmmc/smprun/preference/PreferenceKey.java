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

/**
 * Enumeration of all AppLauncher preference keys.
 * @author Sylvain LAFRASSE
 */
public enum PreferenceKey {

    FIRST_START_FLAG("first.start.flag"),
    START_SELECTED_STUBS("start.selected.stubs"),
    SHOW_EXIT_WARNING("show.exit.warning"),
    SILENTLY_REPORT_FLAG("silently.report.flag"),
    BROADCAST_WARNING_FLAG("broadcast.warning.flag"),
    DISCARD_BROADCASTS_FLAG("discard.broadcasts.flag"),
    APPLICATION_CLI_PATH_PREFIX("command.line.path.for."),
    SELECTED_APPLICATION_LIST("selected.application.list"),
    BETA_APPLICATION_LIST("beta.application.list");
    /** the preferenced value identifying token */
    private final String _key;

    /**
     * Constructor
     * @param key the preferenced value identifying token
     */
    PreferenceKey(String key) {
        _key = key;
    }

    /**
     * @return the preferenced value identifying token
     */
    @Override
    public String toString() {
        return _key;
    }

    /**
     * For unit testing purpose only.
     * @param args
     */
    public static void main(String[] args) {
        for (PreferenceKey k : PreferenceKey.values()) {
            System.out.println("Key '" + k.name() + "' = ['" + k + "'].");
        }
    }
}
