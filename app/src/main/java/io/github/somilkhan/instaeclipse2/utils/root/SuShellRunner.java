package io.github.somilkhan.instaeclipse2.utils.root;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Starts {@code su} with no arguments and writes the script to its stdin, so the script is never
 * re-split by an {@code su -c} implementation. Both output pipes are drained on their own threads
 * (a full pipe would block the script), each capped at {@link #MAX_OUTPUT_BYTES}. On timeout the
 * process is killed. Must not be called on the main thread.
 */
public final class SuShellRunner implements ShellRunner {

    /** A bit above the largest read (the 8 MiB log tail). */
    static final int MAX_OUTPUT_BYTES = 9 * 1024 * 1024;
    private static final int MAX_STDERR_BYTES = 64 * 1024;

    @Override
    public Result run(String script, long timeoutMs) {
        Process p;
        try {
            p = new ProcessBuilder("su").start();
        } catch (IOException e) {
            return new Result(-1, "", "", true, false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        Drain out = new Drain(p.getInputStream(), MAX_OUTPUT_BYTES, "ie-su-out");
        Drain err = new Drain(p.getErrorStream(), MAX_STDERR_BYTES, "ie-su-err");
        out.start();
        err.start();
        String writeError = null;
        try (OutputStream in = p.getOutputStream()) {
            in.write((script + "\nexit\n").getBytes(StandardCharsets.UTF_8));
            in.flush();
        } catch (IOException e) {
            // su exited before reading (denied right away); the exit code below says why.
            writeError = "stdin: " + e.getMessage();
        }
        try {
            if (!p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                out.join(1000);
                err.join(1000);
                return new Result(-1, out.text(), err.text(), false, true, writeError);
            }
            out.join(timeoutMs);
            err.join(1000);
            String error = writeError;
            if (out.truncated) error = (error == null ? "" : error + ", ") + "stdout truncated at " + MAX_OUTPUT_BYTES + " bytes";
            return new Result(p.exitValue(), out.text(), err.text(), false, false, error);
        } catch (InterruptedException e) {
            p.destroyForcibly();
            Thread.currentThread().interrupt();
            return new Result(-1, out.text(), err.text(), false, true, "interrupted");
        } finally {
            p.destroy();
        }
    }

    private static final class Drain extends Thread {
        private final InputStream in;
        private final int cap;
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        volatile boolean truncated;

        Drain(InputStream in, int cap, String name) {
            super(name);
            setDaemon(true);
            this.in = in;
            this.cap = cap;
        }

        @Override
        public void run() {
            byte[] b = new byte[64 * 1024];
            try (InputStream s = in) {
                int n;
                while ((n = s.read(b)) != -1) {
                    synchronized (buf) {
                        int room = cap - buf.size();
                        if (room > 0) buf.write(b, 0, Math.min(room, n));
                        if (n > room) truncated = true; // keep reading so the script never blocks
                    }
                }
            } catch (IOException ignored) {
                // Pipe closed when the process was killed on timeout; the caller reports that.
            }
        }

        String text() {
            synchronized (buf) {
                return new String(buf.toByteArray(), StandardCharsets.UTF_8);
            }
        }
    }
}
