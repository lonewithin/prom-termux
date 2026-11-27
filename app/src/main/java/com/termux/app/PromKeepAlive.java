package com.termux.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;

import java.io.File;
import java.util.Arrays;

/**
 * PROM Terminal keep-alive: start after reboot, hold a wake lock, run
 * {@code ~/.termux/boot/*} (Termux:Boot compatible), and keep a background
 * shell that re-sources {@code ~/.bashrc} if sniper / Starlink / other hooks die.
 */
public final class PromKeepAlive {

    public static final String ACTION_KEEP_ALIVE = TermuxConstants.TERMUX_PACKAGE_NAME + ".service_keep_alive";
    public static final String EXTRA_RUN_BOOT_SCRIPTS = TermuxConstants.TERMUX_PACKAGE_NAME + ".keep_alive.run_boot_scripts";
    public static final String KEEPALIVE_SHELL_NAME = "prom-keepalive";

    /**
     * Background watchdog: wake lock, then re-source ~/.bashrc every 60s so
     * idempotent hooks (pgrep-guarded starts) come back if they were killed.
     */
    public static final String KEEPALIVE_SCRIPT =
        "termux-wake-lock >/dev/null 2>&1 || true; " +
        "sleep 2; " +
        "while true; do " +
        "  if [ -f \"$HOME/.bashrc\" ]; then . \"$HOME/.bashrc\" >/dev/null 2>&1 || true; fi; " +
        "  sleep 60; " +
        "done";

    private static final String LOG_TAG = "PromKeepAlive";

    private PromKeepAlive() {}

    /**
     * Start {@link TermuxService} in the foreground and optionally run boot scripts.
     */
    public static void start(@NonNull Context context, boolean runBootScripts) {
        Intent intent = new Intent(context, TermuxService.class);
        intent.setAction(ACTION_KEEP_ALIVE);
        if (runBootScripts)
            intent.putExtra(EXTRA_RUN_BOOT_SCRIPTS, true);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent);
            else
                context.startService(intent);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to start PROM keep-alive service", e);
        }
    }

    /**
     * Run executable files in {@code ~/.termux/boot} in the background, same
     * contract as Termux:Boot. Missing directory or empty folder is a no-op.
     */
    public static void runBootScripts(@NonNull Context context) {
        File bootDir = TermuxConstants.TERMUX_BOOT_SCRIPTS_DIR;
        File[] files = bootDir.isDirectory() ? bootDir.listFiles() : null;
        if (files == null || files.length == 0) {
            Logger.logInfo(LOG_TAG, "No files to execute at boot in " + bootDir.getAbsolutePath());
            return;
        }

        Arrays.sort(files, (f1, f2) -> f1.getName().compareTo(f2.getName()));
        StringBuilder logMessage = new StringBuilder();
        for (File file : files) {
            if (!file.isFile()) continue;
            if (!file.canRead()) //noinspection ResultOfMethodCallIgnored
                file.setReadable(true);
            if (!file.canExecute()) //noinspection ResultOfMethodCallIgnored
                file.setExecutable(true);

            if (logMessage.length() > 0) logMessage.append(", ");
            logMessage.append(file.getName());

            Uri scriptUri = new Uri.Builder()
                .scheme(TERMUX_SERVICE.URI_SCHEME_SERVICE_EXECUTE)
                .path(file.getAbsolutePath())
                .build();
            Intent executeIntent = new Intent(TERMUX_SERVICE.ACTION_SERVICE_EXECUTE, scriptUri);
            executeIntent.setClassName(TermuxConstants.TERMUX_PACKAGE_NAME, TermuxConstants.TERMUX_APP.TERMUX_SERVICE_NAME);
            executeIntent.putExtra(TERMUX_SERVICE.EXTRA_BACKGROUND, true);
            executeIntent.putExtra(TERMUX_SERVICE.EXTRA_SHELL_NAME, "boot-" + file.getName());
            executeIntent.putExtra(TERMUX_SERVICE.EXTRA_COMMAND_LABEL, "Boot: " + file.getName());
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(executeIntent);
                else
                    context.startService(executeIntent);
            } catch (Exception e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to start boot script " + file.getName(), e);
            }
        }

        if (logMessage.length() > 0)
            Logger.logInfo(LOG_TAG, "Executing boot scripts: " + logMessage);
    }
}
