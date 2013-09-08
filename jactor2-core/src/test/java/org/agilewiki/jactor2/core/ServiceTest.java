package org.agilewiki.jactor2.core;

import junit.framework.TestCase;
import org.agilewiki.jactor2.core.messaging.AsyncRequest;
import org.agilewiki.jactor2.core.messaging.AsyncResponseProcessor;
import org.agilewiki.jactor2.core.messaging.ExceptionHandler;
import org.agilewiki.jactor2.core.messaging.ServiceClosedException;
import org.agilewiki.jactor2.core.processing.MessageProcessor;
import org.agilewiki.jactor2.core.processing.NonBlockingMessageProcessor;
import org.agilewiki.jactor2.core.threading.ModuleContext;

public class ServiceTest extends TestCase {

    public void test() throws Exception {
        ModuleContext testContext = new ModuleContext();
        ModuleContext clientContext = new ModuleContext();
        final ModuleContext serverContext = new ModuleContext();
        try {
            MessageProcessor testMessageProcessor = new NonBlockingMessageProcessor(testContext);
            Server server = new Server(new NonBlockingMessageProcessor(serverContext));
            final Client client = new Client(new NonBlockingMessageProcessor(clientContext), server);
            new AsyncRequest<Void>(testMessageProcessor) {
                AsyncRequest<Void> dis = this;

                @Override
                public void processRequest() throws Exception {
                    client.crossReq().send(getMessageProcessor(), new AsyncResponseProcessor<Boolean>() {
                        @Override
                        public void processAsyncResponse(Boolean response) throws Exception {
                            assertFalse(response);
                            dis.processAsyncResponse(null);
                        }
                    });
                    serverContext.close();
                }
            }.call();
        } finally {
            testContext.close();
            clientContext.close();
            serverContext.close();
        }
    }
}

class Client extends ActorBase {

    Server server;

    Client(MessageProcessor messageProcessor, Server _server) throws Exception {
        initialize(messageProcessor);
        server = _server;
    }

    AsyncRequest<Boolean> crossReq() {
        return new AsyncRequest<Boolean>(getMessageProcessor()) {
            AsyncRequest<Boolean> dis = this;

            @Override
            public void processRequest() throws Exception {
                getMessageProcessor().setExceptionHandler(new ExceptionHandler() {
                    @Override
                    public void processException(Throwable throwable) throws Throwable {
                        if (!(throwable instanceof ServiceClosedException)) {
                            throw throwable;
                        }
                        processAsyncResponse(false);
                    }
                });
                server.hangReq().send(getMessageProcessor(), new AsyncResponseProcessor<Void>() {
                    @Override
                    public void processAsyncResponse(Void response) throws Exception {
                        dis.processAsyncResponse(true);
                    }
                });
            }
        };
    }
}

class Server extends ActorBase {
    Server(MessageProcessor messageProcessor) throws Exception {
        initialize(messageProcessor);
    }

    AsyncRequest<Void> hangReq() {
        return new AsyncRequest<Void>(getMessageProcessor()) {
            @Override
            public void processRequest() throws Exception {
            }
        };
    }
}
