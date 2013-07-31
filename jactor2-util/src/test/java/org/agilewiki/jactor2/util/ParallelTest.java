package org.agilewiki.jactor2.util;

import junit.framework.TestCase;
import org.agilewiki.jactor2.core.context.JAContext;
import org.agilewiki.jactor2.core.mailbox.Mailbox;
import org.agilewiki.jactor2.core.messaging.Request;
import org.agilewiki.jactor2.core.messaging.Transport;

/**
 * Test code.
 */
public class ParallelTest extends TestCase {
    private static final int LOADS = 10;
    private static final long DELAY = 200;

    private Mailbox mailbox;
    private JAContext jaContext;
    private Request<Void> start;

    public void test() throws Exception {
        jaContext = new JAContext();
        mailbox = jaContext.createNonBlockingMailbox();

        start = new Request<Void>(mailbox) {
            @Override
            public void processRequest(
                    final Transport<Void> responseProcessor)
                    throws Exception {
                final ResponseCounter<Void> responseCounter = new ResponseCounter<Void>(
                        LOADS, null, responseProcessor);
                int i = 0;
                while (i < LOADS) {
                    final Delay dly = new Delay(jaContext);
                    dly.sleepReq(ParallelTest.DELAY).send(mailbox,
                            responseCounter);
                    i += 1;
                }
            }
        };

        final long t0 = System.currentTimeMillis();
        start.call();
        final long t1 = System.currentTimeMillis();
        assertTrue((t1 - t0) < DELAY + DELAY / 2);
        jaContext.close();
    }
}
