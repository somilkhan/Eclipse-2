package io.github.somilkhan.instaeclipse2.utils.root;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

/** Records scripts; answers from a function so tests never start a process. */
final class FakeShell implements ShellRunner {
    final List<String> scripts = new ArrayList<>();
    final Deque<Result> queued = new ArrayDeque<>();
    Function<String, Result> answer = s -> ok("");

    static Result ok(String out) { return new Result(0, out, "", false, false, null); }
    static Result fail(int code, String err) { return new Result(code, "", err, false, false, null); }
    static Result noSu() { return new Result(-1, "", "", true, false, "IOException: No such file"); }
    static Result timeout() { return new Result(-1, "", "", false, true, null); }
    static Result granted() { return ok("0\n" + RootCommands.CHECK_MARKER + "\n"); }

    @Override
    public Result run(String script, long timeoutMs) {
        scripts.add(script);
        if (!queued.isEmpty()) return queued.poll();
        return answer.apply(script);
    }
}
