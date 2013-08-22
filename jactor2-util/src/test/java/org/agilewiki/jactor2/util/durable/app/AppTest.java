package org.agilewiki.jactor2.util.durable.app;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.agilewiki.jactor2.core.threading.ModuleContext;
import org.agilewiki.jactor2.util.durable.Durables;
import org.agilewiki.jactor2.util.durable.FactoryLocator;
import org.agilewiki.jactor2.util.durable.incDes.Box;

public class AppTest extends TestCase {
    public void test1() throws Exception {
        ModuleContext moduleContext = Durables.createModuleContext();
        try {
            FactoryLocator factoryLocator = Durables.getFactoryLocator(moduleContext);
            User.register(factoryLocator);
            User user1 = (User) Durables.newSerializable(moduleContext, "user");
            user1.PAName().setValue("Joe");
            user1.PAAge().setValue(42);
            user1.PALocation().setValue("Boston");

            User user2 = (User) user1.getDurable().copy(null);
            Assert.assertEquals("Joe", user2.PAName().getValue());
            assertEquals(42, (int) user2.PAAge().getValue());
            Assert.assertEquals("Boston", user2.PALocation().getValue());
        } finally {
            moduleContext.close();
        }
    }

    public void test2() throws Exception {
        ModuleContext moduleContext = Durables.createModuleContext();
        try {
            FactoryLocator factoryLocator = Durables.getFactoryLocator(moduleContext);
            User.register(factoryLocator);
            Box box1 = (Box) Durables.newSerializable(moduleContext, Box.FACTORY_NAME);
            box1.setValue("user");
            User user1 = (User) box1.getValue();
            user1.PAName().setValue("Joe");
            user1.PAAge().setValue(42);
            user1.PALocation().setValue("Boston");

            Box box2 = (Box) box1.copy(null);
            User user2 = (User) box2.getValue();
            Assert.assertEquals("Joe", user2.PAName().getValue());
            assertEquals(42, (int) user2.PAAge().getValue());
            Assert.assertEquals("Boston", user2.PALocation().getValue());
        } finally {
            moduleContext.close();
        }
    }
}
