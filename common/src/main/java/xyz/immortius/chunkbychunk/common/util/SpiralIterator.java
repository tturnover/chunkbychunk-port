package xyz.immortius.chunkbychunk.common.util;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.Mth;

/**
 * A spiral iterator in 2 dimensions. Spirals clockwise, starting in the positive x direction
 */
public class SpiralIterator {

    private static final int[][] SCAN_DIRECTION_OFFSET = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
    private static final int[] SCAN_DISTANCE_INCREASE = {0, 1, 0, 1};

    private int startX;
    private int startY;
    private int currentX;
    private int currentY;
    private int direction = 0;
    private int lineLength = 1;
    private int lineRemaining = 1;

    /**
     * Spirial iterator starting from (0, 0)
     */
    public SpiralIterator() {
        this(0,0);
    }

    /**
     * @param startX The initial x coord
     * @param startY The initial y coord
     */
    public SpiralIterator(int startX, int startY) {
        this.startX = startX;
        this.startY = startY;
        currentX = startX;
        currentY = startY;
    }

    /**
     * @return The current x position
     */
    public int getX() {
        return currentX;
    }

    /**
     * @return The current y position
     */
    public int getY() {
        return currentY;
    }

    /**
     * Resets the iterator to position (0,0)
     */
    public void reset() {
        reset(0,0);
    }

    /**
     * @return How many layers out is the spiral
     */
    public int layerDistance() {
        return Math.max(Mth.abs(currentX - startX), Mth.abs(currentY - startY));
    }

    /**
     * Resets the iterator to the given position
     * @param x
     * @param y
     */
    public void reset(int x, int y) {
        currentX = x;
        currentY = y;
        direction = 0;
        lineLength = 1;
        lineRemaining = 1;
    }

    /**
     * Iterates to the next coordinate
     */
    public void next() {
        currentX += SCAN_DIRECTION_OFFSET[direction][0];
        currentY += SCAN_DIRECTION_OFFSET[direction][1];
        lineRemaining--;
        if (lineRemaining == 0) {
            lineLength += SCAN_DISTANCE_INCREASE[direction];
            lineRemaining = lineLength;
            direction = (direction + 1) % SCAN_DIRECTION_OFFSET.length;
        }
    }


    public void load(ValueInput input) {
        currentX = input.getIntOr("X", 0);
        currentY = input.getIntOr("Y", 0);
        direction = input.getIntOr("Direction", 0);
        lineLength = input.getIntOr("LineLength", 0);
        lineRemaining = input.getIntOr("LineRemaining", 0);
    }

    public void save(ValueOutput output) {
        output.putInt("X", currentX);
        output.putInt("Y", currentY);
        output.putInt("Direction", direction);
        output.putInt("LineLength", lineLength);
        output.putInt("LineRemaining", lineRemaining);
    }
}
