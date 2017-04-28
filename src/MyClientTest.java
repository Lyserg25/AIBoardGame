import lenz.htw.bogapr.Move;
import org.junit.jupiter.api.Test;

import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Wayne on 14.04.2017.
 */
class MyClientTest {

    @Test
    public void testConcatenate() {
        MyClient client = new MyClient(null, "test", null);
        client.myPlayerNr = 0;
        client.initFieldBounds();
        Stack[][] currentField = client.initField();

        for (int i = 0; i < 50; i++) {
            long start = System.currentTimeMillis();
            Move move = client.calculateMove(currentField);
            System.out.println(move.toString() + " time: " + (System.currentTimeMillis() - start));
        }
    }

}