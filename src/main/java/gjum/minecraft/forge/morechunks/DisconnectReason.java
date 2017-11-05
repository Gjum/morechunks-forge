package gjum.minecraft.forge.morechunks;

public class DisconnectReason {
    public final String description;

    public DisconnectReason(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DisconnectReason that = (DisconnectReason) o;

        return description != null ? description.equals(that.description) : that.description == null;
    }

    @Override
    public int hashCode() {
        return description != null ? description.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "DisconnectReason{" + description + '}';
    }
}
