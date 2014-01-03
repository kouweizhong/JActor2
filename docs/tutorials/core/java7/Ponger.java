import org.agilewiki.jactor2.core.blades.NonBlockingBladeBase;
import org.agilewiki.jactor2.core.requests.SyncRequest;
import org.agilewiki.jactor2.core.reactors.NonBlockingReactor;
import org.agilewiki.jactor2.core.reactors.Reactor;

public class Ponger extends NonBlockingBladeBase {
    private long count = 0;
    
    public Ponger(final NonBlockingReactor _reactor) throws Exception {
        initialize(_reactor);
    }

    private long ping() {
        count += 1;
        return count;
    }

    //Directly callable
    public long ping(final Reactor _sourceReactor) {
        if (getReactor() != _sourceReactor)
            throw new UnsupportedOperationException(
                "for thread safety the caller must use the same reactor");
        return ping();
    }

    public SyncRequest<Long> pingSReq() {
        return new SyncBladeRequest() {
            @Override
            public Long processSyncRequest() throws Exception {
                return ping();
            }
        };
    }
}
