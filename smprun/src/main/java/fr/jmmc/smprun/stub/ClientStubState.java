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

/**
 * Enumeration of all the internal states a stub can go through.
 * 
 * @author Sylvain LAFRASSE, Laurent BOURGES
 */
public enum ClientStubState {

    /** undefined state */
    UNDEFINED(0, ""),
    /** initialization step */
    INITIALIZING(1, ""),
    /** hub connection step */
    CONNECTING(2, ""),
    /** mType registration step */
    REGISTERING(3, ""),
    /** listening for registration of the real application */
    LISTENING(4, ""),
    /** Processing an incoming message */
    PROCESSING(5, "Received a new message"),
    /** Launching the application */
    LAUNCHING(6, "Downloading the application"),
    // TODO: add STARTING state

    /** Waiting for the application to be started and connected to SAMP hub */
    SEEKING(7, "Starting the application"),
    /** Sending the intercepted message to the application */
    FORWARDING(8, "Forwarding the message"),
    /** Disconnecting from hub */
    DISCONNECTING(9, "Cleaning up"),
    /** end state means disabled */
    DIYING(10, "Done"),
    /** failure state means an error occurred when starting the application */
    FAILING(11, "Failed to start the application");
    /* members */
    /** the numerical order of the internal progress (steps equal to zero don't trigger GUI updates) */
    private final int _step;
    /** the user displayable text to explain the internal state */
    private final String _message;

    /**
     * Constructor
     * @param step the numerical order of the internal progress (steps equal to zero don't trigger GUI updates)
     * @param message the user displayable text to explain the internal state
     */
    ClientStubState(final int step, final String message) {
        _step = step;
        _message = message;
    }

    /**
     * @return the internal numerical progression.
     */
    public int step() {
        return _step;
    }

    /**
     * Return true if this state is after the given state
     * @param state state to compare with
     * @return true if this state is after the given state 
     */
    public boolean after(final ClientStubState state) {
        return _step > state.step();
    }

    /**
     * Return true if this state is before the given state
     * @param state state to compare with
     * @return true if this state is before the given state 
     */
    public boolean before(final ClientStubState state) {
        return _step < state.step();
    }

    /**
     * @return the user displayable text to explain the internal state
     */
    public String message() {
        return _message;
    }

    /**
     * For unit testing purpose only.
     * @param args
     */
    public static void main(String[] args) {
        for (ClientStubState s : ClientStubState.values()) {
            System.out.println("State '" + s + "' = [" + s.step() + ", '" + s.message() + "'].");
        }
    }
}