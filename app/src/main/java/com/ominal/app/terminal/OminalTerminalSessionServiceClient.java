package com.ominal.app.terminal;

import android.app.Service;

import androidx.annotation.NonNull;

import com.ominal.app.OminalService;
import com.ominal.shared.runtime.shell.command.runner.terminal.OminalSession;
import com.ominal.shared.runtime.terminal.OminalTerminalSessionClientBase;
import com.ominal.terminal.TerminalSession;
import com.ominal.terminal.TerminalSessionClient;

/** The {@link TerminalSessionClient} implementation that may require a {@link Service} for its interface methods. */
public class OminalTerminalSessionServiceClient extends OminalTerminalSessionClientBase {

    private static final String LOG_TAG = "OminalTerminalSessionServiceClient";

    private final OminalService mService;

    public OminalTerminalSessionServiceClient(OminalService service) {
        this.mService = service;
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession terminalSession, int pid) {
        OminalSession ominalSession = mService.getOminalSessionForTerminalSession(terminalSession);
        if (ominalSession != null)
            ominalSession.getExecutionCommand().mPid = pid;
    }

}
