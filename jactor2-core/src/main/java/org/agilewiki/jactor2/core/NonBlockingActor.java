package org.agilewiki.jactor2.core;

import org.agilewiki.jactor2.core.processing.NonBlockingMessageProcessor;

/**
 * An actor which does not perform long computations nor otherwise block the thread.
 * </p>
 * This is just a convenience class, as any actor which uses a non-blocking message processor
 * is a non-blocking actor.
 * </p>
 * <h3>Sample Usage:</h3>
 * <pre>
 * public class NonBlockingActorSample extends NonBlockingActor {
 *     public NonBlockingActorSample(final ModuleContext _moduleContext) throws Exception {
 *         super(new NonBlockingMessageProcessor(_moduleContext));
 *     }
 * }
 * </pre>
 */
public class NonBlockingActor extends ActorBase {

    /**
     * Create a non-blocking actor.
     *
     * @param _nonBlockingMessageProcessor A message processor for actors which process messages
     *                                     quickly and without blocking the thread.
     */
    public NonBlockingActor(final NonBlockingMessageProcessor _nonBlockingMessageProcessor)
            throws Exception {
        initialize(_nonBlockingMessageProcessor);
    }
}