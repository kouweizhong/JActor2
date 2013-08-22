package org.agilewiki.jactor2.util.durable.incDes;

import junit.framework.TestCase;
import org.agilewiki.jactor2.core.threading.ModuleContext;
import org.agilewiki.jactor2.core.processing.MessageProcessor;
import org.agilewiki.jactor2.core.processing.NonBlockingMessageProcessor;
import org.agilewiki.jactor2.util.durable.Durables;
import org.agilewiki.jactor2.util.durable.FactoryLocator;

public class UnionTest extends TestCase {
    public void test() throws Exception {
        ModuleContext moduleContext = Durables.createModuleContext();
        try {
            FactoryLocator factoryLocator = Durables.getFactoryLocator(moduleContext);
            Durables.registerUnionFactory(factoryLocator, "siUnion", JAString.FACTORY_NAME, "siUnion");
            Union siu1 = (Union) Durables.newSerializable(moduleContext, "siUnion");
            assertNull(siu1.getValue());
            MessageProcessor messageProcessor = new NonBlockingMessageProcessor(moduleContext);
            Union siu2 = (Union) siu1.copy(messageProcessor);
            assertNull(siu2.getValue());
            siu2.setValue(JAString.FACTORY_NAME);
            JAString sj2 = (JAString) siu2.getValue();
            assertNotNull(sj2);
            Union siu3 = (Union) siu2.copy(messageProcessor);
            JAString sj3 = (JAString) siu3.getValue();
            assertNotNull(sj3);
        } finally {
            moduleContext.close();
        }
    }
}
