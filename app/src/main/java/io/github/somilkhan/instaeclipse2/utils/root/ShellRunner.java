package io.github.somilkhan.instaeclipse2.utils.root;

/** Runs one script in a root shell. Swappable so tests never start a process. */
public interface ShellRunner {

    /** Outcome of one script. {@code exitCode} is -1 when the process could not run to the end. */
    final class Result {
        public final int exitCode;
        public final String stdout;
        public final String stderr;
        /** {@code su} could not be started at all (no binary). */
        public final boolean noSu;
        public final boolean timedOut;
        /** Process start / IO failure, for the log. */
        public final String error;

        public Result(int exitCode, String stdout, String stderr, boolean noSu, boolean timedOut, String error) {
            this.exitCode = exitCode;
            this.stdout = stdout == null ? "" : stdout;
            this.stderr = stderr == null ? "" : stderr;
            this.noSu = noSu;
            this.timedOut = timedOut;
            this.error = error;
        }

        public boolean ok() {
            return exitCode == 0 && !noSu && !timedOut;
        }

        /** One line for the log: never contains the script's output beyond a short stderr head. */
        public String summary() {
            if (noSu) return "su not available" + (error != null ? " (" + error + ")" : "");
            if (timedOut) return "timed out";
            String err = stderr.trim();
            if (err.length() > 200) err = err.substring(0, 200) + "…";
            return "exit " + exitCode + (err.isEmpty() ? "" : ", stderr: " + err)
                    + (error != null ? ", " + error : "");
        }
    }

    Result run(String script, long timeoutMs);
}
