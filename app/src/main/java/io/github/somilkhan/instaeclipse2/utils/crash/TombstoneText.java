package io.github.somilkhan.instaeclipse2.utils.crash;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns the API 31+ protobuf tombstone ({@code ApplicationExitInfo.getTraceInputStream()} for a
 * native crash) into readable text: signal, abort message, causes and the crashing thread's
 * backtrace. Field numbers follow AOSP {@code system/core/debuggerd/proto/tombstone.proto}.
 * Any parse problem returns what was decoded so far; the raw file is saved next to the report.
 */
final class TombstoneText {
    private TombstoneText() { }

    // Tombstone
    private static final int T_BUILD_FINGERPRINT = 2, T_TIMESTAMP = 4, T_PID = 5, T_TID = 6,
            T_COMMAND_LINE = 9, T_SIGNAL = 10, T_ABORT = 14, T_CAUSES = 15, T_THREADS = 16;
    // Signal
    private static final int S_NUMBER = 1, S_NAME = 2, S_CODE = 3, S_CODE_NAME = 4,
            S_HAS_FAULT = 8, S_FAULT_ADDR = 9;
    // Cause
    private static final int C_HUMAN = 1;
    // Thread
    private static final int TH_ID = 1, TH_NAME = 2, TH_BACKTRACE = 4, TH_NOTE = 7;
    // BacktraceFrame
    private static final int F_REL_PC = 1, F_FUNCTION = 4, F_OFFSET = 5, F_FILE = 6, F_BUILD_ID = 8;

    static String decode(byte[] data) {
        StringBuilder out = new StringBuilder(8192);
        out.append("── Native crash (decoded tombstone) ──\n");
        try {
            Reader r = new Reader(data, 0, data.length);
            long tid = -1;
            String abort = null, timestamp = null, fingerprint = null;
            List<String> cmd = new ArrayList<>(), causes = new ArrayList<>();
            byte[] signal = null;
            List<byte[]> threads = new ArrayList<>();
            while (r.more()) {
                int key = (int) r.varint(), field = key >>> 3, type = key & 7;
                switch (field) {
                    case T_BUILD_FINGERPRINT -> fingerprint = r.string(type);
                    case T_TIMESTAMP -> timestamp = r.string(type);
                    case T_TID -> tid = r.number(type);
                    case T_COMMAND_LINE -> cmd.add(r.string(type));
                    case T_SIGNAL -> signal = r.bytes(type);
                    case T_ABORT -> abort = r.string(type);
                    case T_CAUSES -> {
                        Reader c = new Reader(r.bytes(type));
                        while (c.more()) {
                            int k = (int) c.varint();
                            if ((k >>> 3) == C_HUMAN) causes.add(c.string(k & 7)); else c.skip(k & 7);
                        }
                    }
                    case T_THREADS -> threads.add(r.bytes(type)); // map entry: key=1 tid, value=2 Thread
                    default -> { if (field == T_PID) r.number(type); else r.skip(type); }
                }
            }
            if (timestamp != null) out.append("Timestamp: ").append(timestamp).append('\n');
            if (!cmd.isEmpty()) out.append("Cmdline: ").append(String.join(" ", cmd)).append('\n');
            if (fingerprint != null) out.append("Fingerprint: ").append(fingerprint).append('\n');
            if (signal != null) out.append(signal(signal)).append('\n');
            if (abort != null && !abort.isEmpty()) out.append("Abort message: ").append(abort).append('\n');
            for (String c : causes) out.append("Cause: ").append(c).append('\n');
            out.append('\n');
            boolean printed = false;
            for (byte[] entry : threads) {
                byte[] thread = null;
                long id = -1;
                Reader e = new Reader(entry);
                while (e.more()) {
                    int k = (int) e.varint();
                    if ((k >>> 3) == 1) id = e.number(k & 7);
                    else if ((k >>> 3) == 2) thread = e.bytes(k & 7);
                    else e.skip(k & 7);
                }
                if (thread != null && id == tid) { out.append(thread(thread, true)); printed = true; }
            }
            if (!printed && !threads.isEmpty()) out.append("(crashing thread not found; tid=").append(tid).append(")\n");
        } catch (Throwable t) {
            out.append("\n(decode stopped: ").append(t.getClass().getSimpleName()).append(")\n");
        }
        return out.toString();
    }

    private static String signal(byte[] b) {
        Reader r = new Reader(b);
        long number = -1, code = -1, fault = 0;
        String name = null, codeName = null;
        boolean hasFault = false;
        while (r.more()) {
            int k = (int) r.varint(), f = k >>> 3, t = k & 7;
            switch (f) {
                case S_NUMBER -> number = r.number(t);
                case S_NAME -> name = r.string(t);
                case S_CODE -> code = r.number(t);
                case S_CODE_NAME -> codeName = r.string(t);
                case S_HAS_FAULT -> hasFault = r.number(t) != 0;
                case S_FAULT_ADDR -> fault = r.number(t);
                default -> r.skip(t);
            }
        }
        return "Signal: " + number + " (" + name + "), code " + code + " (" + codeName + ")"
                + (hasFault ? ", fault addr 0x" + Long.toHexString(fault) : "");
    }

    private static String thread(byte[] b, boolean crashing) {
        Reader r = new Reader(b);
        StringBuilder frames = new StringBuilder();
        long id = -1;
        String name = null;
        List<String> notes = new ArrayList<>();
        int index = 0;
        while (r.more()) {
            int k = (int) r.varint(), f = k >>> 3, t = k & 7;
            switch (f) {
                case TH_ID -> id = r.number(t);
                case TH_NAME -> name = r.string(t);
                case TH_NOTE -> notes.add(r.string(t));
                case TH_BACKTRACE -> { if (index < 64) frames.append(frame(r.bytes(t), index)); else r.skip(t); index++; }
                default -> r.skip(t);
            }
        }
        StringBuilder out = new StringBuilder();
        out.append(crashing ? "Crashing thread: " : "Thread: ").append(id).append(" \"").append(name).append("\"\n");
        for (String n : notes) out.append("  note: ").append(n).append('\n');
        out.append("Backtrace:\n").append(frames);
        if (index > 64) out.append("  … ").append(index - 64).append(" more frames\n");
        return out.toString();
    }

    private static String frame(byte[] b, int index) {
        Reader r = new Reader(b);
        long pc = 0, offset = 0;
        String fn = null, file = null, build = null;
        while (r.more()) {
            int k = (int) r.varint(), f = k >>> 3, t = k & 7;
            switch (f) {
                case F_REL_PC -> pc = r.number(t);
                case F_FUNCTION -> fn = r.string(t);
                case F_OFFSET -> offset = r.number(t);
                case F_FILE -> file = r.string(t);
                case F_BUILD_ID -> build = r.string(t);
                default -> r.skip(t);
            }
        }
        StringBuilder s = new StringBuilder(String.format(java.util.Locale.ROOT, "  #%02d pc %016x  %s", index, pc, file));
        if (fn != null && !fn.isEmpty()) s.append(" (").append(fn).append('+').append(offset).append(')');
        if (build != null && !build.isEmpty()) s.append(" (BuildId: ").append(build).append(')');
        return s.append('\n').toString();
    }

    /** Minimal protobuf wire reader. */
    private static final class Reader {
        final byte[] d; int p; final int end;
        Reader(byte[] d) { this(d, 0, d.length); }
        Reader(byte[] d, int from, int to) { this.d = d; p = from; end = to; }
        boolean more() { return p < end; }
        long varint() {
            long v = 0; int shift = 0;
            while (true) {
                if (p >= end || shift > 63) throw new IllegalStateException("varint");
                byte b = d[p++];
                v |= (long) (b & 0x7f) << shift;
                if ((b & 0x80) == 0) return v;
                shift += 7;
            }
        }
        long number(int type) {
            if (type == 0) return varint();
            if (type == 1) { long v = 0; for (int i = 0; i < 8; i++) v |= (long) (d[p++] & 0xff) << (8 * i); return v; }
            if (type == 5) { long v = 0; for (int i = 0; i < 4; i++) v |= (long) (d[p++] & 0xff) << (8 * i); return v; }
            skip(type); return 0;
        }
        byte[] bytes(int type) {
            if (type != 2) { skip(type); return new byte[0]; }
            int len = (int) varint();
            if (len < 0 || p + len > end) throw new IllegalStateException("length");
            byte[] out = new byte[len];
            System.arraycopy(d, p, out, 0, len);
            p += len;
            return out;
        }
        String string(int type) { return new String(bytes(type), StandardCharsets.UTF_8); }
        void skip(int type) {
            switch (type) {
                case 0 -> varint();
                case 1 -> p += 8;
                case 2 -> { int len = (int) varint(); if (len < 0) throw new IllegalStateException("length"); p += len; }
                case 5 -> p += 4;
                default -> throw new IllegalStateException("wire type " + type);
            }
            if (p > end) throw new IllegalStateException("overrun");
        }
    }
}
