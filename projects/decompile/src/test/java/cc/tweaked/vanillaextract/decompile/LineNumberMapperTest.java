package cc.tweaked.vanillaextract.decompile;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class LineNumberMapperTest {
    /**
     * Map at 0 and the beyond the maximum line.
     */
    @Test
    public void Map_min_and_max() {
        var lineMapper = new LineNumberMapper(null, new int[]{
            5, 10,
            6, 11,
            8, 12
        });

        assertEquals(0, lineMapper.remapLine(0));
        assertEquals(12, lineMapper.remapLine(10));
    }

    /**
     * Match exact values.
     */
    @Test
    public void Map_exact() {
        var lineMapper = new LineNumberMapper(null, new int[]{
            5, 10,
            6, 11,
            8, 12
        });

        assertEquals(10, lineMapper.remapLine(5));
        assertEquals(11, lineMapper.remapLine(6));
        assertEquals(12, lineMapper.remapLine(8));
    }

    /**
     * Mapping something below finds the next largest line.
     */
    @Test
    public void Map_chooses_next_line() {
        var lineMapper = new LineNumberMapper(null, new int[]{
            5, 10,
            6, 11,
            8, 12
        });

        assertEquals(10, lineMapper.remapLine(3));
        assertEquals(10, lineMapper.remapLine(4));
        assertEquals(12, lineMapper.remapLine(7));
    }
}
