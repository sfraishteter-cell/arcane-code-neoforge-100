package dev.sergey.arcanecode.spell;

import java.util.List;

public final class PatternNormalizer {
    private PatternNormalizer() {}

    public static String key(int[] directions) {
        if (directions.length == 0) return "";
        int offset = directions[0];
        StringBuilder builder = new StringBuilder();
        for (int direction : directions) builder.append(Math.floorMod(direction - offset, 6));
        return builder.toString();
    }

    public static String key(List<Integer> directions) {
        int[] array = directions.stream().mapToInt(Integer::intValue).toArray();
        return key(array);
    }
}
