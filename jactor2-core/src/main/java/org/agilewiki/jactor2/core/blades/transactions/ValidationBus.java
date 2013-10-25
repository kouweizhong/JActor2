package org.agilewiki.jactor2.core.blades.transactions;

import org.agilewiki.jactor2.core.blades.requestBus.RequestBus;
import org.agilewiki.jactor2.core.blades.requestBus.Subscription;
import org.agilewiki.jactor2.core.messages.AsyncRequest;
import org.agilewiki.jactor2.core.messages.AsyncResponseProcessor;
import org.agilewiki.jactor2.core.reactors.NonBlockingReactor;

import java.util.Iterator;

public class ValidationBus<IMMUTABLE_CHANGES> extends RequestBus<IMMUTABLE_CHANGES, String> {
    public ValidationBus(NonBlockingReactor _reactor) throws Exception {
        super(_reactor);
    }

    public AsyncRequest<String> sendAReq(final IMMUTABLE_CHANGES _changes) {
        return new AsyncBladeRequest<String>() {
            AsyncResponseProcessor<String> dis = this;
            int count;
            int i;

            AsyncResponseProcessor<String> notificationResponseProcessor =
                    new AsyncResponseProcessor<String>() {
                        @Override
                        public void processAsyncResponse(final String _error) throws Exception {
                            i++;
                            if (_error != null) {
                                dis.processAsyncResponse(_error);
                                return;
                            }
                            if (count == i)
                                dis.processAsyncResponse(null);
                        }
                    };

            @Override
            protected void processAsyncRequest() throws Exception {
                count = subscriptions.size();
                if (count == 0) {
                    dis.processAsyncResponse(null);
                    return;
                }
                Iterator<Subscription<IMMUTABLE_CHANGES, String>> it = subscriptions.iterator();
                while (it.hasNext()) {
                    Subscription<IMMUTABLE_CHANGES, String> subscription = it.next();
                    send(subscription.notificationAReq(_changes), notificationResponseProcessor);
                }
            }
        };
    }
}
