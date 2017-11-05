package gjum.minecraft.forge.morechunks;

public class ExpectedDisconnect extends DisconnectReason {

    public ExpectedDisconnect(String description) {
        super(description);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExpectedDisconnect that = (ExpectedDisconnect) o;

        return description != null ? description.equals(that.description) : that.description == null;
    }

    @Override
    public String toString() {
        return "ExpectedDisconnect{" + description + '}';
    }
}
