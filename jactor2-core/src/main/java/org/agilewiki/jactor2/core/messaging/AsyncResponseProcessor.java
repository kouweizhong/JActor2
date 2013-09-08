package org.agilewiki.jactor2.core.messaging;

/**
 * A AsyncResponseProcessor is an application callback to handle the response from a request.
 *
 * @param <RESPONSE_TYPE> The type of response.
 */
public interface AsyncResponseProcessor<RESPONSE_TYPE> {
    /**
     * The processAsyncResponse method accepts the response of a request.
     * <p>
     * This method need not be thread-safe, as it
     * is always invoked from the same light-weight thread (message processor) that passed the
     * AsyncRequest and AsyncResponseProcessor objects.
     * </p>
     *
     * @param _response The response to a request.
     */
    public void processAsyncResponse(final RESPONSE_TYPE _response) throws Exception;
}
