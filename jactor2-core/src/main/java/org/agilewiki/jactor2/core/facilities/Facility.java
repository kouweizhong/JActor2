package org.agilewiki.jactor2.core.facilities;

import org.agilewiki.jactor2.core.blades.BladeBase;
import org.agilewiki.jactor2.core.messages.AsyncRequest;
import org.agilewiki.jactor2.core.messages.AsyncResponseProcessor;
import org.agilewiki.jactor2.core.messages.RequestBase;
import org.agilewiki.jactor2.core.messages.SyncRequest;
import org.agilewiki.jactor2.core.reactors.NonBlockingReactor;
import org.agilewiki.jactor2.core.reactors.Reactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadFactory;

/**
 * Provides a thread pool for
 * non-blocking and isolation targetReactor. Multiple facilities with independent life cycles
 * are also supported.
 * (A ServiceClosedException may be thrown when messages cross facilities and the target facility is closed.)
 * In addition, the facility maintains a set of AutoClosable objects that are closed
 * when the facility is closed, as well as a table of properties.
 */

public class Facility extends BladeBase implements AutoCloseable {

    public final static String NAME_PROPERTY = "core.name";

    public final static String PLANT_NAME = "Plant";

    public final static String DEPENDENCY_PROPERTY_PREFIX = "core.dependency_";

    public final static String FACILITY_PROPERTY_PREFIX = "core.facility_";

    /**
     * The facility's internal reactor for managing the auto closeable set and for closing itself.
     */
    private final InternalReactor internalReactor;

    /**
     * A hash set of AutoCloseable objects.
     * Can only be accessed via a request to the facility.
     */
    protected final Set<AutoCloseable> closeables = Collections
            .newSetFromMap(new WeakHashMap<AutoCloseable, Boolean>());

    /**
     * Set when the facility reaches end-of-life.
     * Can only be updated via a request to the facility.
     */
    private boolean shuttingDown = false;

    /**
     * When DEBUG, pendingRequests holds the active requests ordered by timestamp.
     */
    public final ConcurrentSkipListMap<Long, Set<RequestBase>> pendingRequests =
            Plant.DEBUG ? new ConcurrentSkipListMap<Long, Set<RequestBase>>() : null;

    /**
     * The logger used by targetReactor.
     */
    private final Logger messageProcessorLogger = LoggerFactory.getLogger(Reactor.class);

    /**
     * The thread pool used by Facility.
     */
    private final ThreadManager threadManager;

    /**
     * How big should the initial inbox doLocal queue size be?
     */
    private final int initialLocalMessageQueueSize;

    /**
     * How big should the initial outbox (per target Reactor) buffer size be?
     */
    private final int initialBufferSize;

    /**
     * Facility properties.
     */
    private ConcurrentSkipListMap<String, Object> properties = new ConcurrentSkipListMap<String, Object>();

    private final Set<FacilityPropertyChangeSubscriber> propertyChangeSubscribers = new HashSet<FacilityPropertyChangeSubscriber>();

    /**
     * Create a Facility.
     *
     * @param _name                         The name of the facility.
     * @param _initialLocalMessageQueueSize How big should the initial inbox doLocal queue size be?
     * @param _initialBufferSize            How big should the initial outbox (per target Reactor) buffer size be?
     * @param _threadCount                  The thread pool size.
     * @param _threadFactory                The factory used to create threads for the threadpool.
     */
    protected Facility(final String _name,
                       final int _initialLocalMessageQueueSize,
                       final int _initialBufferSize,
                       final int _threadCount,
                       final ThreadFactory _threadFactory) throws Exception {
        validateName(_name);
        threadManager = new ThreadManager(
                _threadCount, _threadFactory);
        initialLocalMessageQueueSize = _initialLocalMessageQueueSize;
        initialBufferSize = _initialBufferSize;
        internalReactor = new InternalReactor();
        initialize(internalReactor);
        firstSet(NAME_PROPERTY, _name);
    }

    protected void validateName(final String _name) throws Exception {
        if (_name == null)
            throw new IllegalArgumentException("name may not be null");
        if (_name.length() == 0)
            throw new IllegalArgumentException("name may not be empty");
        if (_name.contains(" "))
            throw new IllegalArgumentException("name may not contain spaces: " + _name);
        if (_name.equals(PLANT_NAME))
            throw new IllegalArgumentException("name may be " + PLANT_NAME);
    }

    /**
     * Returns the logger to be used by targetReactor.
     *
     * @return A logger.
     */
    public Logger getMessageProcessorLogger() {
        return messageProcessorLogger;
    }

    /**
     * Returns the initial buffer size to be used by outboxes.
     *
     * @return The initial buffer size.
     */
    public int getInitialBufferSize() {
        return initialBufferSize;
    }

    /**
     * Returns the initial doLocal message queue(s) size.
     *
     * @return The initial doLocal message queue(s) size.
     */
    public int getInitialLocalMessageQueueSize() {
        return initialLocalMessageQueueSize;
    }

    /**
     * Submit a Reactor for subsequent execution.
     *
     * @param _reactor The targetReactor to be run.
     */
    public final void submit(final Reactor _reactor)
            throws Exception {
        try {
            threadManager.execute(_reactor);
        } catch (final Exception e) {
            if (!isClosing())
                throw e;
        } catch (final Error e) {
            if (!isClosing())
                throw e;
        }
    }

    /**
     * Returns a request to add an auto closeable, to be closed when the Facility closes.
     * This request returns true if the AutoClosable was added.
     *
     * @param _closeable The autoclosable to be added to the list.
     * @return The request.
     */
    public SyncRequest<Boolean> addAutoClosableSReq(final AutoCloseable _closeable) {
        return new SyncBladeRequest<Boolean>() {
            @Override
            protected Boolean processSyncRequest() throws Exception {
                if (!isClosing()) {
                    return closeables.add(_closeable);
                } else {
                    return false;
                }
            }
        };
    }

    /**
     * Returns a request to remove an auto closeable.
     * This request returns true if the AutoClosable was removed.
     *
     * @param _closeable The autoclosable to be removed.
     * @return The request.
     */
    public SyncRequest<Boolean> removeAutoClosableSReq(final AutoCloseable _closeable) {
        return new SyncBladeRequest<Boolean>() {
            @Override
            protected Boolean processSyncRequest() throws Exception {
                if (!isClosing()) {
                    return closeables.remove(_closeable);
                }
                return false;
            }
        };
    }

    @Override
    public void close() throws Exception {
        new SyncBladeRequest<Void>() {
            @Override
            protected Void processSyncRequest() throws Exception {
                Plant plant = getPlant();
                if (plant != null && plant != Facility.this) {
                    plant.removeAutoClosableSReq(this).signal();
                    plant.putPropertyAReq(FACILITY_PROPERTY_PREFIX + getName(), null).signal();
                }
                if (shuttingDown) {
                    return null;
                }
                shuttingDown = true;
                threadManager.close();
                final Iterator<AutoCloseable> it = closeables.iterator();
                while (it.hasNext()) {
                    try {
                        it.next().close();
                    } catch (final Throwable t) {
                        t.printStackTrace();
                    }
                }
                return null;
            }
        }.signal();
    }

    /**
     * Returns true if close() has been called already.
     *
     * @return true if close() has already been called.
     */
    public final boolean isClosing() {
        return shuttingDown;
    }

    /**
     * Returns the value of a property.
     *
     * @param propertyName The property name.
     * @return The property value, or null.
     */
    public Object getProperty(final String propertyName) {
        return properties.get(propertyName);
    }

    public String getName() {
        return (String) getProperty(NAME_PROPERTY);
    }

    public Plant getPlant() {
        return (Plant) getProperty(DEPENDENCY_PROPERTY_PREFIX + PLANT_NAME);
    }

    public AsyncRequest<Object> putPropertyAReq(final String _propertyName,
                                                final Object _propertyValue) {
        return new AsyncBladeRequest<Object>() {
            final AsyncResponseProcessor<Object> dis = this;
            int count = propertyChangeSubscribers.size();

            @Override
            protected void processAsyncRequest() throws Exception {
                final Object old = _propertyValue == null ?
                        properties.remove(_propertyName) : properties.put(_propertyName, _propertyValue);
                if (count == 0) {
                    dis.processAsyncResponse(old);
                    return;
                }
                Iterator<FacilityPropertyChangeSubscriber> it = propertyChangeSubscribers.iterator();
                while (it.hasNext()) {
                    FacilityPropertyChangeSubscriber fpcs = it.next();
                    send(fpcs.propertyChangedAReq(Facility.this, _propertyName, old, _propertyValue), new AsyncResponseProcessor<Void>() {
                        @Override
                        public void processAsyncResponse(Void _response) throws Exception {
                            count--;
                            if (count == 0)
                                dis.processAsyncResponse(old);
                        }
                    });
                }
            }
        };
    }

    protected void firstSet(final String _propertyName,
                            final Object _propertyValue) throws Exception {
        if (_propertyValue == null)
            throw new IllegalArgumentException("value may not be null");
        if (properties.get(_propertyName) != null)
            throw new IllegalStateException("old value must be null");
        properties.put(_propertyName, _propertyValue);
    }

    public AsyncRequest<Void> firstSetAReq(final String _propertyName,
                                           final Object _propertyValue) {
        return new AsyncBladeRequest<Void>() {
            AsyncResponseProcessor<Void> dis = this;

            @Override
            protected void processAsyncRequest() throws Exception {
                if (_propertyValue == null)
                    throw new IllegalArgumentException("value may not be null");
                if (properties.get(_propertyName) != null)
                    throw new IllegalStateException("old value must be null");
                send(putPropertyAReq(_propertyName, _propertyValue), new AsyncResponseProcessor<Object>() {
                    @Override
                    public void processAsyncResponse(Object _response) throws Exception {
                        dis.processAsyncResponse(null);
                    }
                });
            }
        };
    }

    /**
     * Returns a copy of the property names.
     *
     * @return A copy of the property names.
     */
    public Set<String> getPropertyNames() {
        return new HashSet<String>(properties.keySet());
    }

    private ConcurrentNavigableMap<String, Object> matchingProperties(final String _prefix) throws Exception {
        return properties.subMap(_prefix + Character.MIN_VALUE, _prefix + Character.MAX_VALUE);
    }

    public SyncRequest<TreeMap<String, Object>> matchingPropertiesSReq(final String _prefix) {
        return new SyncBladeRequest<TreeMap<String, Object>>() {
            @Override
            protected TreeMap<String, Object> processSyncRequest() throws Exception {
                return new TreeMap<String, Object>(matchingProperties(_prefix));
            }
        };
    }

    public AsyncRequest<Boolean> hasDependencyAReq(final String _name) {
        return new AsyncBladeRequest<Boolean>() {
            AsyncResponseProcessor<Boolean> dis = this;
            int count;

            AsyncResponseProcessor<Boolean> prp = new AsyncResponseProcessor<Boolean>() {
                @Override
                public void processAsyncResponse(Boolean _hasDependency) throws Exception {
                    if (_hasDependency) {
                        dis.processAsyncResponse(true);
                        return;
                    }
                    count--;
                    if (count == 0)
                        dis.processAsyncResponse(false);
                }
            };

            @Override
            protected void processAsyncRequest() throws Exception {
                if (properties.containsKey(DEPENDENCY_PROPERTY_PREFIX + _name)) {
                    processAsyncResponse(true);
                    return;
                }
                ConcurrentNavigableMap<String, Object> cnm = matchingProperties(DEPENDENCY_PROPERTY_PREFIX);
                Collection<Object> values = cnm.values();
                if (values.size() == 0) {
                    processAsyncResponse(false);
                    return;
                }
                Iterator<Object> it = values.iterator();
                while (it.hasNext()) {
                    Facility dependency = (Facility) it.next();
                    count++;
                    send(dependency.hasDependencyAReq(_name), prp);
                }
            }
        };
    }

    public AsyncRequest<Void> dependencyAReq(final Facility _dependency) {
        return new AsyncBladeRequest<Void>() {

            AsyncResponseProcessor<Void> dis = this;

            @Override
            protected void processAsyncRequest() throws Exception {
                final String myName = getName();
                if (myName == null)
                    throw new IllegalStateException("assign a name before adding a dependency");
                final String name = _dependency.getName();
                if (name == null)
                    throw new IllegalArgumentException("the dependency has no name");
                final String propertyName = DEPENDENCY_PROPERTY_PREFIX + name;
                if (properties.containsKey(propertyName))
                    throw new IllegalStateException("the dependency was already present");
                send(_dependency.hasDependencyAReq(myName), new AsyncResponseProcessor<Boolean>() {
                    @Override
                    public void processAsyncResponse(Boolean _hasDependency) throws Exception {
                        if (_hasDependency)
                            throw new IllegalArgumentException("this would create a cyclic dependency");
                        send(firstSetAReq(propertyName, _dependency), new AsyncResponseProcessor<Void>() {
                            @Override
                            public void processAsyncResponse(Void _response) throws Exception {
                                send(_dependency.addAutoClosableSReq(Facility.this), new AsyncResponseProcessor<Boolean>() {
                                    @Override
                                    public void processAsyncResponse(Boolean _response) throws Exception {
                                        dis.processAsyncResponse(null);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        };
    }

    public SyncRequest<Map<String, Object>> subscribePropertyChangesSReq(
            final FacilityPropertyChangeSubscriber _subscriber) {
        return new SyncBladeRequest<Map<String, Object>>() {
            @Override
            protected Map<String, Object> processSyncRequest()
                    throws Exception {
                if (propertyChangeSubscribers.add(_subscriber))
                    return new HashMap<String, Object>(properties);
                return null;
            }
        };
    }

    public SyncRequest<Boolean> unsubscribePropertyChangesSReq(
            final FacilityPropertyChangeSubscriber _subscriber) {
        return new SyncBladeRequest<Boolean>() {
            @Override
            protected Boolean processSyncRequest()
                    throws Exception {
                return propertyChangeSubscribers.remove(_subscriber);
            }
        };
    }

    protected ClassLoader getClassLoader() throws Exception {
        return getClass().getClassLoader();
    }

    public AsyncRequest<ClassLoader> getClassLoaderAReq() {
        return new AsyncBladeRequest<ClassLoader>() {
            @Override
            protected void processAsyncRequest() throws Exception {
                processAsyncResponse(getClassLoader());
            }
        };
    }

    public AsyncRequest<Void> initiateAReq(final String _initiatorClassName) {
        return new AsyncBladeRequest<Void>() {
            @Override
            protected void processAsyncRequest() throws Exception {
                Class initiatorClass = getClassLoader().loadClass(_initiatorClassName);
                Initiator initiator = (Initiator) initiatorClass.newInstance();
                initiator.initialize(getReactor());
                send(initiator.startAReq(), this);
            }
        };
    }

    /**
     * The reactor used internally.
     */
    private class InternalReactor extends NonBlockingReactor {

        /**
         * Create an internal reactor.
         */
        public InternalReactor() throws Exception {
            super(Facility.this);
        }

        /**
         * No autoclose.
         */
        @Override
        protected void addAutoClose() throws Exception {
        }
    }
}