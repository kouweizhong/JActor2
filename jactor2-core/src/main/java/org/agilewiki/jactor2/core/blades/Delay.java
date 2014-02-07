package org.agilewiki.jactor2.core.blades;

import org.agilewiki.jactor2.core.blades.BlockingBladeBase;
import org.agilewiki.jactor2.core.reactors.BlockingReactor;
import org.agilewiki.jactor2.core.requests.SyncRequest;

/**
 * Simulates I/O.
 */
public class Delay extends BlockingBladeBase {
    /**
     * Create a Delay blade and a Blocking reactor whose parent is the internal reactor of Plant.
     */
    public Delay() throws Exception {
    }

    /**
     * Create a Delay blade.
     *
     * @param _reactor The blade's facility.
     */
    public Delay(final BlockingReactor _reactor) throws Exception {
        super(_reactor);
    }

    /**
     * Returns a delay request.
     *
     * @param _delay The duration of the delay in milliseconds.
     * @return The delay request.
     */
    public SyncRequest<Void> sleepSReq(final long _delay) {
        return new SyncBladeRequest<Void>() {
            @Override
            public Void processSyncRequest() throws Exception {
                Thread.sleep(_delay);
                return null;
            }
        };
    }
}
