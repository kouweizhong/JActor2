import org.agilewiki.jactor2.core.requests.SOp;
import org.agilewiki.jactor2.core.reactors.Reactor;
import org.agilewiki.jactor2.core.requests.impl.RequestImpl;

public class PongerLoop extends SOp<Void> {
    private final Ponger ponger;
    private final long count;

    public PongerLoop(
            final Ponger _ponger, 
            final long _count) {
        super("pongerLoop", _ponger.getReactor());
        ponger = _ponger;
        count = _count;
    }
    
    @Override
    public Void processSyncOperation(final RequestImpl _requestImpl) throws Exception {
        long i = 0;
        Reactor reactor = getTargetReactor();
        while (i < count) {
            i++;
            ponger.ping(reactor);
        }
        return null;
    }
}
