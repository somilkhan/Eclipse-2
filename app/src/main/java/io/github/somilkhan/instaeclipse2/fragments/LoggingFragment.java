package io.github.somilkhan.instaeclipse2.fragments;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import io.github.somilkhan.instaeclipse2.BuildConfig;
import io.github.somilkhan.instaeclipse2.R;
import io.github.somilkhan.instaeclipse2.utils.core.CommonUtils;
import io.github.somilkhan.instaeclipse2.utils.log.LogExport;
import io.github.somilkhan.instaeclipse2.utils.log.LogMirrorProvider;
import io.github.somilkhan.instaeclipse2.utils.log.LogWindow;
import io.github.somilkhan.instaeclipse2.utils.log.Logging;

/**
 * Logs tab. Instagram's module log is read from a file in the companion's own storage that the
 * hooked Instagram process keeps appending to (see {@link LogMirrorProvider}), so it works even
 * when Instagram is backgrounded or dead. While Instagram is running we additionally ask it (via
 * broadcast) to re-sync that file; its inline reply is only used as a fallback when the file is
 * missing (e.g. an Instagram process started before this companion version was installed).
 *
 * <p>Threading: all file reads, parsing, sorting, filtering and diffing run on {@link #loadExecutor};
 * the main thread only swaps in the prepared list and notifies the adapter with ranges. While the
 * tab is visible the mirror files are polled cheaply (size/mtime) and new entries are appended
 * incrementally. The on-screen list is bounded to {@link #MAX_ENTRIES}; "Save to storage" always
 * writes the complete files.
 */
public class LoggingFragment extends Fragment {

    private static final long INSTAGRAM_REPLY_TIMEOUT_MS = 3000;
    private static final int MAX_ENTRIES = 6000;
    // Logging caps each persisted file at 4 MiB; read all of it for the view (the list itself is
    // bounded by MAX_ENTRIES). Save to storage has no cap at all.
    private static final long MAX_READ_BYTES = 8L * 1024 * 1024;
    /** Clipboard only (Binder-limited); Save to storage writes everything. */
    private static final int MAX_COPY_CHARS = 250_000;
    private static final long LIVE_POLL_MS = 1500;

    private static final int KIND_INFO = 0;
    private static final int KIND_OK = 1;
    private static final int KIND_WARN = 2;
    private static final int KIND_ERROR = 3;

    private static final int FILTER_ALL = 0;

    private static final Pattern TS_PREFIX = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} .*", Pattern.DOTALL);
    private static final int TS_LEN = 23; // "yyyy-MM-dd HH:mm:ss.SSS"

    private static final class Entry {
        final String ts;
        final String full;
        final String display;
        final int kind;

        Entry(String ts, String full, String display, int kind) {
            this.ts = ts;
            this.full = full;
            this.display = display;
            this.kind = kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry)) return false;
            Entry e = (Entry) o;
            return kind == e.kind && ts.equals(e.ts) && full.equals(e.full);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ts, full, kind);
        }
    }

    /** Everything the main thread needs to render one update; built on the load executor. */
    private static final class Prepared {
        List<Entry> all;
        List<Entry> shown;
        LogWindow.Delta<Entry> delta; // non-null only for an incremental append with FILTER_ALL
        int total, ok, warn, err;
    }

    private RecyclerView listView;
    private View emptyView;
    private TextView emptyTitle;
    private TextView emptyBody;
    private View emptyRefresh;
    private TextView countView;
    private TextView statusView;
    private View progressView;
    private Chip chipAll, chipHooked, chipErrors, chipWarnings;

    private final LogAdapter adapter = new LogAdapter();
    private List<Entry> allEntries = new ArrayList<>();
    private int filterKind = FILTER_ALL;
    private boolean loadedOnce;

    private final Map<String, String> liveReplies = new HashMap<>();
    /** Logs read with root from Instagram's data dir, for apps whose mirror file is missing. */
    private final Map<String, String> rootLogs = new HashMap<>();
    private boolean waitingForInstagram;
    private boolean noTargetInstalled;
    private Runnable pendingTimeout;

    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService exportExecutor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<String> saveLauncher;
    private boolean saving;
    private int totalEntries;
    private volatile String liveSignature;
    private boolean resumed;
    private final Runnable livePoll = this::pollLive;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger loadGeneration = new AtomicInteger(0);

    /** Nonce of the last ACTION_REQUEST_LOGS; replies without it are ignored (IpcSecurity). */
    private String logsNonce;
    private final java.util.List<io.github.somilkhan.instaeclipse2.utils.ipc.ModuleReplies.Pending> logReplies = new java.util.ArrayList<>();

    private final BroadcastReceiver logReplyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!CommonUtils.ACTION_LOGS_REPLY.equals(intent.getAction()) || listView == null) return;
            if (!io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.nonceMatches(logsNonce, intent)) return;
            String src = intent.getStringExtra(CommonUtils.EXTRA_LOG_SOURCE);
            if (src == null) src = CommonUtils.IG_PACKAGE_NAME;
            String body = intent.getStringExtra(CommonUtils.EXTRA_LOG_TEXT);
            liveReplies.put(src, body == null ? "" : body);
            cancelInstagramTimeout();
            waitingForInstagram = false;
            reload();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        saveLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument(LogExport.MIME_TYPE), this::saveTo);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_logging, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = view.findViewById(R.id.logs_list);
        emptyView = view.findViewById(R.id.logs_empty);
        emptyTitle = view.findViewById(R.id.logs_empty_title);
        emptyBody = view.findViewById(R.id.logs_empty_body);
        emptyRefresh = view.findViewById(R.id.logs_empty_refresh);
        countView = view.findViewById(R.id.logs_count);
        statusView = view.findViewById(R.id.logs_status);
        progressView = view.findViewById(R.id.logs_progress);
        chipAll = view.findViewById(R.id.logs_chip_all);
        chipHooked = view.findViewById(R.id.logs_chip_hooked);
        chipErrors = view.findViewById(R.id.logs_chip_errors);
        chipWarnings = view.findViewById(R.id.logs_chip_warnings);

        adapter.colorInfo = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurfaceVariant);
        adapter.colorError = MaterialColors.getColor(view, androidx.appcompat.R.attr.colorError);
        adapter.colorWarn = ContextCompat.getColor(requireContext(), R.color.ie_log_warn);
        adapter.colorOk = ContextCompat.getColor(requireContext(), R.color.ie_log_ok);

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        listView.setLayoutManager(lm);
        listView.setAdapter(adapter);
        listView.setItemAnimator(null);

        ChipGroup group = view.findViewById(R.id.logs_filter_group);
        group.setOnCheckedStateChangeListener((g, ids) -> {
            int id = ids.isEmpty() ? R.id.logs_chip_all : ids.get(0);
            if (id == R.id.logs_chip_hooked) filterKind = KIND_OK;
            else if (id == R.id.logs_chip_errors) filterKind = KIND_ERROR;
            else if (id == R.id.logs_chip_warnings) filterKind = KIND_WARN;
            else filterKind = FILTER_ALL;
            applyFilter();
        });

        view.findViewById(R.id.logs_refresh).setOnClickListener(v -> loadLogs());
        emptyRefresh.setOnClickListener(v -> loadLogs());
        view.findViewById(R.id.logs_copy).setOnClickListener(v -> copyLogs());
        view.findViewById(R.id.logs_save).setOnClickListener(v -> saveLogs());
        view.findViewById(R.id.logs_clear).setOnClickListener(v -> clearLogs());
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();
        resumed = true;
        loadLogs();
        scheduleLivePoll();
    }

    @Override
    public void onPause() {
        super.onPause();
        resumed = false;
        mainHandler.removeCallbacks(livePoll);
    }

    @Override
    public void onStop() {
        super.onStop();
        cancelInstagramTimeout();
        waitingForInstagram = false;
        cancelLogReplies();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelInstagramTimeout();
        listView = null;
        emptyView = null;
        countView = null;
        statusView = null;
        progressView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        loadExecutor.shutdownNow();
        // A running export finishes writing; the executor just takes no new work.
        exportExecutor.shutdown();
    }

    // ---------------------------------------------------------------- loading

    /** Shows the saved log right away, then asks running Instagram processes to re-sync it. */
    private void loadLogs() {
        Context ctx = getContext();
        if (ctx == null || listView == null) return;
        liveReplies.clear();
        reload();

        List<String> targets = findInstalledTargets(ctx);
        noTargetInstalled = targets.isEmpty();
        if (noTargetInstalled) return;
        readMissingWithRoot(ctx.getApplicationContext());
        cancelLogReplies();
        logsNonce = io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.newNonce();
        for (String pkg : targets) {
            Intent request = new Intent(CommonUtils.ACTION_REQUEST_LOGS);
            request.setPackage(pkg);
            request.putExtra(io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.EXTRA_NONCE, logsNonce);
            io.github.somilkhan.instaeclipse2.utils.ipc.ModuleReplies.Pending pending =
                    io.github.somilkhan.instaeclipse2.utils.core.IpcSecurity.requestReply(ctx, request, CommonUtils.ACTION_LOGS_REPLY, logReplyReceiver);
            if (pending != null) logReplies.add(pending);
        }
        // Nothing was sent (no trusted host): don't show a 3 s spinner waiting for nobody.
        if (logReplies.isEmpty()) {
            updateStatus();
            return;
        }
        waitingForInstagram = true;
        setProgress(true);
        cancelInstagramTimeout();
        pendingTimeout = () -> {
            pendingTimeout = null;
            waitingForInstagram = false;
            setProgress(false);
            updateStatus();
        };
        mainHandler.postDelayed(pendingTimeout, INSTAGRAM_REPLY_TIMEOUT_MS);
    }

    /**
     * Root fallback (opt-in): when Instagram can't write the mirror file (started before the
     * companion was installed or updated, or provider blocked), read its own log file instead.
     * Only for signer-trusted hosts; never runs su when "Use root" is off.
     */
    private void readMissingWithRoot(Context app) {
        if (io.github.somilkhan.instaeclipse2.utils.root.RootAccess.session(app).plannedPath()
                != io.github.somilkhan.instaeclipse2.utils.root.RootCommands.Path.ROOT) {
            rootLogs.clear();
            return;
        }
        List<String> missing = new ArrayList<>();
        for (String pkg : io.github.somilkhan.instaeclipse2.utils.root.RootUi.installedTargets(app, true)) {
            File f = new File(LogMirrorProvider.mirrorDir(app), pkg + ".log");
            if (!f.isFile() || f.length() == 0) missing.add(pkg);
        }
        if (missing.isEmpty()) {
            rootLogs.clear();
            return;
        }
        io.github.somilkhan.instaeclipse2.utils.root.RootActions actions = io.github.somilkhan.instaeclipse2.utils.root.RootUi.actions(app);
        io.github.somilkhan.instaeclipse2.utils.root.RootAccess.runAsync(() -> {
            Map<String, String> got = new HashMap<>();
            for (String pkg : missing) {
                String text = actions.readModuleLog(pkg);
                if (text != null && !text.isEmpty()) got.put(pkg, text);
            }
            return got;
        }, got -> {
            if (listView == null) return;
            rootLogs.clear();
            if (got != null) rootLogs.putAll(got);
            if (!rootLogs.isEmpty()) reload();
        });
    }

    private void cancelLogReplies() {
        for (io.github.somilkhan.instaeclipse2.utils.ipc.ModuleReplies.Pending reply : logReplies) reply.cancel();
        logReplies.clear();
    }

    private void reload() {
        Context ctx = getContext();
        if (ctx == null) return;
        Context app = ctx.getApplicationContext();
        int gen = loadGeneration.incrementAndGet();
        Map<String, String> replies = new HashMap<>(rootLogs);
        replies.putAll(liveReplies); // a live answer is newer than the file read with root
        String companionTag = getString(R.string.logs_source_companion);
        if (!loadedOnce) setProgress(true);
        List<Entry> current = allEntries; // treated as immutable; replaced, never mutated
        int filter = filterKind;
        try {
            loadExecutor.execute(() -> {
                liveSignature = signature(app);
                List<Entry> fresh = new ArrayList<>();
                int total = readAllEntries(app, replies, companionTag, fresh);
                Prepared p = prepare(current, fresh, total, filter);
                mainHandler.post(() -> {
                    if (listView == null || gen != loadGeneration.get()) return;
                    loadedOnce = true;
                    if (!waitingForInstagram) setProgress(false);
                    render(p, filter);
                    updateStatus();
                });
            });
        } catch (Throwable ignored) {
            // executor shut down (fragment destroyed)
        }
    }

    /** Off main thread: diff against what is on screen, then filter and count. */
    private static Prepared prepare(List<Entry> current, List<Entry> fresh, int total, int filter) {
        Prepared p = new Prepared();
        p.total = total;
        LogWindow.Delta<Entry> d = LogWindow.diff(current, fresh, MAX_ENTRIES);
        if (d.incremental) {
            List<Entry> all = new ArrayList<>(current.size() + d.items.size());
            all.addAll(current.subList(d.dropFromFront, current.size()));
            all.addAll(d.items);
            p.all = all;
            if (filter == FILTER_ALL) p.delta = d;
        } else {
            p.all = d.items;
        }
        for (Entry e : p.all) {
            if (e.kind == KIND_OK) p.ok++;
            else if (e.kind == KIND_WARN) p.warn++;
            else if (e.kind == KIND_ERROR) p.err++;
        }
        p.shown = filterEntries(p.all, filter);
        return p;
    }

    private static List<Entry> filterEntries(List<Entry> all, int filter) {
        if (filter == FILTER_ALL) return all;
        List<Entry> shown = new ArrayList<>();
        for (Entry e : all) if (e.kind == filter) shown.add(e);
        return shown;
    }

    /** Cheap change detector for the live view: file sizes/mtimes plus the companion's revision. */
    private static String signature(Context ctx) {
        StringBuilder sb = new StringBuilder();
        File[] files = LogMirrorProvider.mirrorDir(ctx).listFiles((d, n) -> n.endsWith(".log"));
        if (files != null) {
            java.util.Arrays.sort(files);
            for (File f : files) sb.append(f.getName()).append(':').append(f.length()).append(':').append(f.lastModified()).append(';');
        }
        sb.append("app:").append(Logging.revision());
        return sb.toString();
    }

    private void scheduleLivePoll() {
        mainHandler.removeCallbacks(livePoll);
        if (resumed) mainHandler.postDelayed(livePoll, LIVE_POLL_MS);
    }

    private void pollLive() {
        Context ctx = getContext();
        if (ctx == null || listView == null || !resumed) return;
        Context app = ctx.getApplicationContext();
        String known = liveSignature;
        try {
            loadExecutor.execute(() -> {
                String now = signature(app);
                mainHandler.post(() -> {
                    if (listView == null) return;
                    if (known != null && !now.equals(known)) reload();
                    scheduleLivePoll();
                });
            });
        } catch (Throwable ignored) {
            // executor shut down
        }
    }

    /** Fills {@code out} with the newest MAX_ENTRIES entries; returns the total before the cap. */
    private static int readAllEntries(Context ctx, Map<String, String> replies, String companionTag, List<Entry> result) {
        List<Entry> out = new ArrayList<>();
        Map<String, Boolean> seen = new HashMap<>();

        File[] files = LogMirrorProvider.mirrorDir(ctx).listFiles((d, n) -> n.endsWith(".log"));
        if (files != null) {
            for (File f : files) {
                String pkg = f.getName().substring(0, f.getName().length() - 4);
                List<String> lines = readTail(f);
                if (lines.isEmpty()) {
                    String reply = replies.get(pkg);
                    if (reply != null) lines = splitLines(reply);
                }
                if (!lines.isEmpty()) seen.put(pkg, true);
                parseInto(out, lines, sourcePrefix(pkg));
            }
        }
        // Fallback: a running Instagram that couldn't open the mirror still answers inline.
        for (Map.Entry<String, String> r : replies.entrySet()) {
            if (seen.containsKey(r.getKey())) continue;
            parseInto(out, splitLines(r.getValue()), sourcePrefix(r.getKey()));
        }
        parseInto(out, splitLines(Logging.getSnapshot()), companionTag + " ");

        // Stable merge by timestamp so app + Instagram entries interleave correctly.
        Collections.sort(out, (a, b) -> a.ts.compareTo(b.ts));
        result.addAll(LogWindow.tail(out, MAX_ENTRIES));
        return out.size();
    }

    private static String sourcePrefix(String pkg) {
        return CommonUtils.IG_PACKAGE_NAME.equals(pkg) ? "" : "[" + pkg + "] ";
    }

    private static List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        for (String l : text.split("\n")) if (!l.isEmpty()) lines.add(l);
        return lines;
    }

    /**
     * Instagram rewrites its mirror (truncate + write) on every flush. A read that races that
     * rewrite sees a short or empty file, which looked like "cut off" logs; re-read until the
     * size is stable across the read.
     */
    private static List<String> readTail(File f) {
        List<String> lines = new ArrayList<>();
        for (int attempt = 0; attempt < 3; attempt++) {
            long before = f.length();
            lines = readTailOnce(f);
            boolean stable = f.length() == before;
            if (stable && !lines.isEmpty()) break;
            if (!f.exists()) break;
            try { Thread.sleep(120); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        return lines;
    }

    private static List<String> readTailOnce(File f) {
        ArrayDeque<String> lines = new ArrayDeque<>();
        if (!f.isFile() || f.length() == 0) return new ArrayList<>();
        try (FileInputStream in = new FileInputStream(f)) {
            long len = f.length();
            boolean skipFirst = false;
            if (len > MAX_READ_BYTES) {
                long skip = len - MAX_READ_BYTES;
                while (skip > 0) {
                    long s = in.skip(skip);
                    if (s <= 0) break;
                    skip -= s;
                }
                skipFirst = true; // likely a partial line
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                if (skipFirst) {
                    skipFirst = false;
                    continue;
                }
                if (line.isEmpty()) continue;
                lines.addLast(line);
                if (lines.size() > MAX_ENTRIES * 4) lines.pollFirst();
            }
        } catch (Throwable ignored) {}
        return new ArrayList<>(lines);
    }

    /** Groups raw lines into entries: a timestamped line starts one, others (stack traces) continue it. */
    private static void parseInto(List<Entry> out, List<String> lines, String prefix) {
        String curTs = null;
        StringBuilder cur = null;
        String lastTs = "";
        for (String line : lines) {
            boolean starts = line.length() > TS_LEN && TS_PREFIX.matcher(line).matches();
            if (starts || cur == null) {
                if (cur != null) out.add(makeEntry(curTs, cur.toString(), prefix));
                curTs = starts ? line.substring(0, TS_LEN) : lastTs;
                lastTs = curTs;
                cur = new StringBuilder(line);
            } else {
                cur.append('\n').append(line);
            }
        }
        if (cur != null) out.add(makeEntry(curTs, cur.toString(), prefix));
    }

    private static Entry makeEntry(String ts, String full, String prefix) {
        String display;
        if (full.length() > TS_LEN && full.startsWith(ts) && !ts.isEmpty()) {
            // Drop the date for display; keep HH:mm:ss.SSS.
            display = full.substring(11, TS_LEN) + "  " + prefix + full.substring(TS_LEN + 1);
        } else {
            display = prefix + full;
        }
        return new Entry(ts == null ? "" : ts, prefix.isEmpty() ? full : insertPrefix(full, ts, prefix), display, classify(full));
    }

    private static String insertPrefix(String full, String ts, String prefix) {
        if (ts != null && !ts.isEmpty() && full.startsWith(ts) && full.length() > TS_LEN) {
            return ts + " " + prefix + full.substring(TS_LEN + 1);
        }
        return prefix + full;
    }

    static int classify(String s) {
        String l = s.toLowerCase(Locale.ROOT);
        if (s.contains("❌") || l.contains("exception") || l.contains("error") || l.contains("failed")
                || l.contains("fatal") || l.contains("crash")) return KIND_ERROR;
        if (s.contains("⚠") || l.contains("warn") || l.contains("not found") || l.contains("timeout")
                || l.contains("timed out") || l.contains("skipp") || l.contains("fallback")) return KIND_WARN;
        if (s.contains("✅") || s.contains("✔") || l.contains("hooked") || l.contains("success")
                || l.contains("installed") || l.contains(" ok ") || l.endsWith(" ok")) return KIND_OK;
        return KIND_INFO;
    }

    // ---------------------------------------------------------------- rendering

    /** Filter chip changed: re-filter off the main thread. */
    private void applyFilter() {
        if (listView == null) return;
        List<Entry> current = allEntries;
        int filter = filterKind;
        int total = totalEntries;
        int gen = loadGeneration.incrementAndGet();
        try {
            loadExecutor.execute(() -> {
                Prepared p = prepare(Collections.emptyList(), current, total, filter);
                mainHandler.post(() -> {
                    if (listView == null || gen != loadGeneration.get()) return;
                    render(p, filter);
                });
            });
        } catch (Throwable ignored) {
            // executor shut down
        }
    }

    /** Main thread: swap in a prepared update. Only O(1) adapter work for live appends. */
    private void render(Prepared p, int filter) {
        if (listView == null) return;
        if (filter != filterKind) { // chip changed while this update was being prepared
            allEntries = p.all;
            totalEntries = p.total;
            applyFilter();
            return;
        }
        boolean atBottom = !listView.canScrollVertically(1);
        allEntries = p.all;
        totalEntries = p.total;
        chipAll.setText(getString(R.string.logs_filter_count, getString(R.string.logs_filter_all), p.all.size()));
        chipHooked.setText(getString(R.string.logs_filter_count, getString(R.string.logs_filter_hooked), p.ok));
        chipErrors.setText(getString(R.string.logs_filter_count, getString(R.string.logs_filter_errors), p.err));
        chipWarnings.setText(getString(R.string.logs_filter_count, getString(R.string.logs_filter_warnings), p.warn));

        boolean wasEmpty = adapter.items.isEmpty();
        if (p.delta != null && !wasEmpty) {
            if (!p.delta.isEmpty()) adapter.append(p.shown, p.delta.dropFromFront, p.delta.items.size());
        } else {
            adapter.setItems(p.shown);
        }
        countView.setText(getString(R.string.logs_count_format, p.shown.size(), p.total));

        boolean empty = p.shown.isEmpty();
        emptyView.setVisibility(empty && loadedOnce ? View.VISIBLE : View.GONE);
        listView.setVisibility(empty ? View.INVISIBLE : View.VISIBLE);
        if (empty) {
            boolean filtered = !p.all.isEmpty();
            emptyTitle.setText(filtered ? R.string.logs_empty_filter_title : R.string.logs_empty_title);
            emptyBody.setText(filtered ? R.string.logs_empty_filter_body : R.string.logs_empty_body);
            emptyRefresh.setVisibility(filtered ? View.GONE : View.VISIBLE);
        } else if (atBottom || wasEmpty || p.delta == null) {
            // Follow the tail unless the user scrolled up to read something.
            listView.scrollToPosition(p.shown.size() - 1);
        }
    }

    private void updateStatus() {
        if (statusView == null) return;
        int res;
        if (noTargetInstalled) res = R.string.logs_status_none;
        else if (!liveReplies.isEmpty()) res = R.string.logs_status_live;
        else if (!rootLogs.isEmpty() && !waitingForInstagram) res = R.string.logs_status_root;
        else if (!waitingForInstagram) res = R.string.logs_status_saved;
        else {
            statusView.setVisibility(View.GONE);
            return;
        }
        statusView.setText(res);
        statusView.setVisibility(View.VISIBLE);
    }

    private void setProgress(boolean on) {
        if (progressView != null) progressView.setVisibility(on ? View.VISIBLE : View.GONE);
    }

    private void cancelInstagramTimeout() {
        if (pendingTimeout != null) {
            mainHandler.removeCallbacks(pendingTimeout);
            pendingTimeout = null;
        }
    }

    private static List<String> findInstalledTargets(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        List<String> out = new ArrayList<>();
        for (String pkg : CommonUtils.SUPPORTED_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                out.add(pkg);
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return out;
    }

    // ---------------------------------------------------------------- actions

    /** Text of the currently filtered entries (full timestamps), capped for the clipboard. */
    private String copyText() {
        List<Entry> items = adapter.items;
        int start = items.size();
        long chars = 0;
        while (start > 0) {
            int len = items.get(start - 1).full.length() + 1;
            if (chars + len > MAX_COPY_CHARS) break;
            chars += len;
            start--;
        }
        StringBuilder sb = new StringBuilder((int) chars);
        for (int i = start; i < items.size(); i++) sb.append(items.get(i).full).append('\n');
        return sb.toString();
    }

    private void copyLogs() {
        Context ctx = getContext();
        if (ctx == null) return;
        String text = copyText();
        if (text.isEmpty()) {
            io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(ctx, R.string.logs_nothing_to_copy, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        clipboard.setPrimaryClip(ClipData.newPlainText("InstaEclipse logs", text));
        io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(ctx, R.string.logs_copied, Toast.LENGTH_SHORT).show();
    }

    // ---------------------------------------------------------------- save to storage

    /** Opens the system "create document" picker; the full log is written once a file is chosen. */
    private void saveLogs() {
        Context ctx = getContext();
        if (ctx == null || saveLauncher == null) return;
        if (saving) {
            io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(ctx, R.string.logs_saving, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            saveLauncher.launch(LogExport.fileName(new Date(), TimeZone.getDefault()));
        } catch (Throwable t) {
            io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(ctx, R.string.logs_save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveTo(@Nullable Uri uri) {
        Context ctx = getContext();
        if (uri == null || ctx == null) return; // picker cancelled
        Context app = ctx.getApplicationContext();
        Map<String, String> replies = new HashMap<>(liveReplies);
        Map<String, String> fromRoot = new HashMap<>(rootLogs);
        String companionTitle = getString(R.string.logs_export_companion_section);
        saving = true;
        setProgress(true);
        io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(ctx, R.string.logs_saving, Toast.LENGTH_SHORT).show();
        try {
            exportExecutor.execute(() -> {
                boolean ok = writeFullLog(app, uri, replies, fromRoot, companionTitle);
                mainHandler.post(() -> {
                    saving = false;
                    if (!waitingForInstagram) setProgress(false);
                    io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(app,
                            ok ? R.string.logs_saved : R.string.logs_save_failed, Toast.LENGTH_SHORT).show();
                });
            });
        } catch (Throwable t) {
            saving = false;
            setProgress(false);
            io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(ctx, R.string.logs_save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /** Background thread. Streams every log source completely: no entry or size cap. */
    private static boolean writeFullLog(Context app, Uri uri, Map<String, String> replies,
                                        Map<String, String> fromRoot, String companionTitle) {
        try (OutputStream raw = app.getContentResolver().openOutputStream(uri, "wt")) {
            if (raw == null) return false;
            OutputStream out = new java.io.BufferedOutputStream(raw, 64 * 1024);
            LogExport.writeText(out, LogExport.header(deviceInfo(app), new Date(), TimeZone.getDefault()));
            Map<String, Boolean> written = new HashMap<>();
            File[] files = LogMirrorProvider.mirrorDir(app).listFiles((d, n) -> n.endsWith(".log"));
            if (files != null) {
                java.util.Arrays.sort(files);
                for (File f : files) {
                    String pkg = f.getName().substring(0, f.getName().length() - 4);
                    if (f.length() == 0 && replies.get(pkg) == null) continue;
                    LogExport.writeText(out, LogExport.sectionTitle("Instagram log [" + pkg + "]"));
                    long n = 0;
                    try (InputStream in = new FileInputStream(f)) {
                        n = LogExport.copyAll(in, out);
                    }
                    if (n == 0) LogExport.writeText(out, replies.get(pkg));
                    written.put(pkg, true);
                }
            }
            for (Map.Entry<String, String> r : replies.entrySet()) {
                if (written.containsKey(r.getKey()) || r.getValue() == null || r.getValue().isEmpty()) continue;
                LogExport.writeText(out, LogExport.sectionTitle("Instagram log [" + r.getKey() + "] (live reply)"));
                LogExport.writeText(out, r.getValue());
                written.put(r.getKey(), true);
            }
            for (Map.Entry<String, String> r : fromRoot.entrySet()) {
                if (written.containsKey(r.getKey()) || r.getValue() == null || r.getValue().isEmpty()) continue;
                LogExport.writeText(out, LogExport.sectionTitle("Instagram log [" + r.getKey() + "] (read with root)"));
                LogExport.writeText(out, r.getValue());
            }
            LogExport.writeText(out, LogExport.sectionTitle(companionTitle));
            LogExport.writeText(out, Logging.getSnapshot());
            out.flush();
            return true;
        } catch (Throwable t) {
            android.util.Log.w("InstaEclipse", "[Logs] save to storage failed", t);
            return false;
        }
    }

    private static LogExport.DeviceInfo deviceInfo(Context ctx) {
        LogExport.DeviceInfo info = new LogExport.DeviceInfo();
        info.appVersionName = BuildConfig.VERSION_NAME;
        info.appVersionCode = BuildConfig.VERSION_CODE;
        info.buildType = BuildConfig.BUILD_TYPE;
        info.checkedInstagram = io.github.somilkhan.instaeclipse2.utils.core.IgCompat.evidenceSummary();
        info.instagram = new ArrayList<>();
        PackageManager pm = ctx.getPackageManager();
        for (String pkg : CommonUtils.SUPPORTED_PACKAGES) {
            try {
                PackageInfo pi = pm.getPackageInfo(pkg, 0);
                info.instagram.add(new LogExport.TargetApp(pkg, pi.versionName, pi.getLongVersionCode()));
            } catch (Throwable ignored) {}
        }
        info.androidRelease = Build.VERSION.RELEASE;
        info.sdkInt = Build.VERSION.SDK_INT;
        info.manufacturer = Build.MANUFACTURER;
        info.model = Build.MODEL;
        info.device = Build.DEVICE;
        return info;
    }

    private void clearLogs() {
        Context ctx = getContext();
        if (ctx == null) return;
        // Only to signer-trusted hosts, like every other companion -> Instagram command (this
        // used to go to any installed package with a supported name).
        CommonUtils.broadcastToInstagram(ctx, new Intent(CommonUtils.ACTION_CLEAR_LOGS));
        Logging.clear();
        liveReplies.clear();
        rootLogs.clear();
        Context app = ctx.getApplicationContext();
        try {
            loadExecutor.execute(() -> {
                // Truncate (not delete): a running Instagram keeps an O_APPEND fd on these files.
                File[] files = LogMirrorProvider.mirrorDir(app).listFiles();
                if (files != null) {
                    for (File f : files) {
                        try (FileOutputStream fos = new FileOutputStream(f, false)) {
                            fos.getFD();
                        } catch (Throwable ignored) {}
                    }
                }
            });
        } catch (Throwable ignored) {}
        reload();
        io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(ctx, R.string.logs_cleared, Toast.LENGTH_SHORT).show();
    }

    // ---------------------------------------------------------------- adapter

    private final class LogAdapter extends RecyclerView.Adapter<LogAdapter.VH> {
        List<Entry> items = new ArrayList<>();
        int colorInfo, colorOk, colorWarn, colorError;

        @SuppressWarnings("NotifyDataSetChanged")
        void setItems(List<Entry> list) {
            items = list;
            notifyDataSetChanged();
        }

        /** Live update: {@code list} is the old list minus {@code dropped} head items plus {@code added}. */
        void append(List<Entry> list, int dropped, int added) {
            items = list;
            if (dropped > 0) notifyItemRangeRemoved(0, dropped);
            if (added > 0) notifyItemRangeInserted(list.size() - added, added);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            android.util.DisplayMetrics dm = parent.getResources().getDisplayMetrics();
            int padV = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, dm));
            int padH = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, dm));
            tv.setPadding(padH, padV, padH, padV);
            tv.setLineSpacing(0, 1.15f);
            // Log lines are code: keep them left-to-right even in Arabic / Hebrew.
            tv.setTextDirection(View.TEXT_DIRECTION_LTR);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Entry e = items.get(position);
            holder.text.setText(e.display);
            int c;
            switch (e.kind) {
                case KIND_ERROR: c = colorError; break;
                case KIND_WARN: c = colorWarn; break;
                case KIND_OK: c = colorOk; break;
                default: c = colorInfo;
            }
            holder.text.setTextColor(c);
            holder.text.setOnLongClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("InstaEclipse log entry", e.full));
                    io.github.somilkhan.instaeclipse2.utils.toast.IeToast.makeText(v.getContext(), R.string.logs_entry_copied, Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class VH extends RecyclerView.ViewHolder {
            final TextView text;

            VH(TextView tv) {
                super(tv);
                text = tv;
            }
        }
    }
}
