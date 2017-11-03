package gjum.minecraft.forge.morechunks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class CallTracker<Call> {
    final List<CallSnap> calls = new ArrayList<>();

    void trackCall(Call call, Object... args) {
        calls.add(new CallSnap(call, args));
    }

    CallSnap getLastCall() {
        return calls.get(calls.size() - 1);
    }

    public boolean containsCall(Call call) {
        return calls.stream().anyMatch(snap -> snap.call == call);
    }

    public boolean containsCall(Predicate<CallSnap> p) {
        return calls.stream().anyMatch(p);
    }

    class CallSnap {
        final Call call;
        final Object[] args;

        @Override
        public String toString() {
            if (args.length == 1) {
                return "CallSnap{" + call + ", arg=" + args[0] + "}";
            }
            return "CallSnap{" + call + ", " + args.length + " args}";
        }

        private CallSnap(Call call, Object... args) {
            this.call = call;
            this.args = args;
        }
    }
}
