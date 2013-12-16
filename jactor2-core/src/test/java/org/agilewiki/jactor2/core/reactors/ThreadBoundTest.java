package org.agilewiki.jactor2.core.reactors;

import junit.framework.TestCase;
import org.agilewiki.jactor2.core.plant.Plant;
import org.agilewiki.jactor2.core.messages.Blade1;

/**
 * Test code.
 */
public class ThreadBoundTest extends TestCase {
    ThreadBoundReactor reactor;

    public void testa() throws Exception {
        final Plant plant = new Plant();
        try {
            reactor = new ThreadBoundReactor(plant, new Runnable() {
                @Override
                public void run() {
                    reactor.run();
                    try {
                        plant.close();
                    } catch (final Throwable x) {
                    }
                }
            });
            final Blade1 blade1 = new Blade1(reactor);
            String response = blade1.hiSReq().call();
            System.out.println(response);
            assertEquals("Hello world!", response);
        } finally {
            plant.close();
        }
    }
}
