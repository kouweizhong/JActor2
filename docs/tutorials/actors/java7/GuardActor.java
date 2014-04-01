import java.util.concurrent.atomic.AtomicBoolean;

class GuardActor {
    private AtomicBoolean busy = new AtomicBoolean();
    private volatile boolean replyExpected;
    
    protected void start(boolean isReply) {
        while (true) {
            while ((replyExpected && !isReply) || !busy.compareAndSet(false, true))
                Thread.yield();
            if (!replyExpected && isReply) {
                busy.set(false);
                throw new UnsupportedOperationException("Reply received when none expected");
            }
            if (replyExpected == isReply)
                return;
            busy.set(false);
            Thread.yield();
        }
    }
    
    protected void finish(boolean expectingReply) {
        replyExpected = expectingReply;
        busy.set(false);
    }
}
