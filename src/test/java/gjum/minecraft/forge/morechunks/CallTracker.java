package gjum.minecraft.forge.morechunks;

import java.util.ArrayList;
import java.util.List;

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

    class CallSnap {
        final Call call;
        final Object[] args;

        private CallSnap(Call call, Object... args) {
            this.call = call;
            this.args = args;
        }
    }
}
