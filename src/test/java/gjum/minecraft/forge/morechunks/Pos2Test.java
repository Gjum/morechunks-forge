package gjum.minecraft.forge.morechunks;

import junit.framework.TestCase;

public class Pos2Test extends TestCase {
    public void testEquals() {
        assertTrue(new Pos2(1, 2).equals(new Pos2(1, 2)));
        assertFalse(new Pos2(2, 3).equals(new Pos2(3, 2)));

        Object o = new Pos2(3, 4);
        assertTrue(new Pos2(3, 4).equals(o));

        Object[] oa = {new Pos2(3, 4)};
        assertTrue(new Pos2(3, 4).equals(oa[0]));
    }

}
