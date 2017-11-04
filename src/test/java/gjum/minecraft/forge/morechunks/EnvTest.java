package gjum.minecraft.forge.morechunks;

import junit.framework.TestCase;
import org.apache.logging.log4j.Level;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class EnvTest extends TestCase {
    public void testCurrentTimeMillis() throws Exception {
        Env env = new Env();
        long before = System.currentTimeMillis();
        long measured = env.currentTimeMillis();
        long after = System.currentTimeMillis();
        assertTrue(before <= measured);
        assertTrue(measured <= after);
    }

    public void testLog() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream out = System.out;
        System.setOut(ps);

        Env env = new Env();
        env.log(Level.INFO, "Hi there");
        assertEquals("INFO [EnvTest.testLog@26] Hi there\n", baos.toString());

        System.setOut(out);
        ps.close();
        baos.close();
    }
}
