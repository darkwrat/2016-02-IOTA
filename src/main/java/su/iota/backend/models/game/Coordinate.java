package su.iota.backend.models.game;

import org.jetbrains.annotations.NotNull;

public class Coordinate {

    private final int offX;
    private final int offY;

    public static final int BASE_X = 34 / 2;
    public static final int BASE_Y = 34 / 2;

    public Coordinate(int offX, int offY) {
        this.offX = offX;
        this.offY = offY;
    }

    public int getOffX() {
        return offX;
    }

    public int getOffY() {
        return offY;
    }

    public int getX() {
        return BASE_X + offX;
    }

    public int getY() {
        return BASE_Y + offY;
    }

    @NotNull
    public Coordinate plus(@NotNull Coordinate other) {
        return new Coordinate(offX + other.offX, offY + other.offY);
    }

    @NotNull
    public Coordinate minus(@NotNull Coordinate other) {
        return new Coordinate(offX - other.offX, offY - other.offY);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Coordinate other = (Coordinate) obj;
        return offX == other.offX && offY == other.offY;
    }

    @Override
    public int hashCode() {
        int result = offX;
        result = 31 * result + offY;
        return result;
    }

}