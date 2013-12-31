package org.agilewiki.jactor2.modules;

import org.agilewiki.jactor2.core.blades.BlockingBladeBase;
import org.agilewiki.jactor2.core.messages.AsyncRequest;
import org.agilewiki.jactor2.core.messages.AsyncResponseProcessor;
import org.agilewiki.jactor2.core.messages.SyncRequest;
import org.agilewiki.jactor2.core.plant.Plant;
import org.agilewiki.jactor2.core.reactors.BlockingReactor;
import org.agilewiki.jactor2.core.reactors.CommonReactor;
import org.agilewiki.jactor2.core.util.immutable.ImmutableProperties;
import org.agilewiki.jactor2.modules.pubSub.RequestBus;
import org.agilewiki.jactor2.modules.pubSub.SubscribeAReq;
import org.agilewiki.jactor2.modules.transactions.properties.ImmutablePropertyChanges;
import org.agilewiki.jactor2.modules.transactions.properties.PropertiesProcessor;
import org.agilewiki.jactor2.modules.transactions.properties.PropertyChange;
import org.agilewiki.jactor2.modules.transactions.properties.PropertyChangesFilter;

import java.io.PrintStream;
import java.util.Locale;

/**
 * <p>
 * A blocking blade is used for printing.
 * </p>
 * <h3>Sample Usage:</h3>
 * <pre>
 * public class PrinterSample {
 *
 *     public static void main(String[] args) throws Exception {
 *
 *         //A facility with one thread.
 *         final Plant plant = new Plant(1);
 *
 *         try {
 *
 *             //Print something.
 *             Printer.printlnAReq(plant, "Hello World!").call();
 *
 *         } finally {
 *             //shutdown the plant
 *             plant.close();
 *         }
 *
 *     }
 * }
 * </pre>
 */
public class Printer extends BlockingBladeBase {

    public static AsyncRequest<Void> printlnAReq(final String _string) throws Exception {
        return new AsyncRequest<Void>(Plant.getSingleton().getReactor()) {
            AsyncResponseProcessor<Void> dis = this;

            @Override
            public void processAsyncRequest() throws Exception {
                send(stdoutAReq(),
                        new AsyncResponseProcessor<Printer>() {
                            @Override
                            public void processAsyncResponse(
                                    final Printer _printer) throws Exception {
                                send(_printer.printlnSReq(_string), dis);
                            }
                        });
            }
        };
    }

    public static AsyncRequest<Void> printfAReq(final String _format, final Object... _args) throws Exception {
        return new AsyncRequest<Void>(Plant.getSingleton().getReactor()) {
            AsyncResponseProcessor<Void> dis = this;

            @Override
            public void processAsyncRequest() throws Exception {
                send(stdoutAReq(),
                        new AsyncResponseProcessor<Printer>() {
                            @Override
                            public void processAsyncResponse(
                                    final Printer _printer) throws Exception {
                                send(_printer.printfSReq(_format, _args), dis);
                            }
                        });
            }
        };
    }

    static public AsyncRequest<Printer> stdoutAReq()
            throws Exception {
        return new AsyncRequest<Printer>(Plant.getSingleton().getReactor()) {
            AsyncResponseProcessor<Printer> dis = this;
            PropertiesProcessor propertiesProcessor = Plant.getSingleton().asFacility().getPropertiesProcessor();
            ImmutableProperties<Object> immutableProperties = propertiesProcessor.getImmutableState();
            Printer printer = (Printer) immutableProperties.get("stdout");

            AsyncResponseProcessor<Void> cnsResponseProcessor = new AsyncResponseProcessor<Void>() {
                @Override
                public void processAsyncResponse(Void _response) throws Exception {
                    immutableProperties = propertiesProcessor.getImmutableState();
                    Printer printer2 = (Printer) immutableProperties.get("stdout");
                    if (printer != printer2) {
                        dis.processAsyncResponse(printer2);
                        return;
                    }
                    RequestBus<ImmutablePropertyChanges> validationBus = propertiesProcessor.validationBus;
                    send(new SubscribeAReq<ImmutablePropertyChanges>(
                            validationBus,
                            (CommonReactor) getTargetReactor(),
                            new PropertyChangesFilter("stdout")) {
                        @Override
                        protected void processContent(ImmutablePropertyChanges _changes) throws Exception {
                            PropertyChange propertyChange = _changes.readOnlyChanges.get("stdout");
                            if (propertyChange == null)
                                return;
                            if (propertyChange.oldValue != null)
                                throw new IllegalStateException("stdout property can not be changed");
                        }
                    }, dis, printer);
                }
            };

            @Override
            public void processAsyncRequest() throws Exception {
                if (printer != null) {
                    dis.processAsyncResponse(printer);
                    return;
                }
                printer = new Printer(new BlockingReactor());
                send(propertiesProcessor.compareAndSetAReq("stdout", null, printer), cnsResponseProcessor);
            }
        };
    }

    public final PrintStream printStream;

    public final Locale locale;

    /**
     * Create a Printer blades.
     *
     * @param _reactor The reactor used by the blocking blade.
     */
    public Printer(final BlockingReactor _reactor) throws Exception {
        this(_reactor, System.out);
    }

    /**
     * Create a Printer blades.
     *
     * @param _reactor     The reactor used by the blocking blade.
     * @param _printStream Where to print the string.
     */
    public Printer(final BlockingReactor _reactor,
            final PrintStream _printStream) throws Exception {
        this(_reactor, _printStream, null);
    }

    public Printer(final BlockingReactor _reactor,
            final PrintStream _printStream, final Locale _locale)
            throws Exception {
        initialize(_reactor);
        printStream = _printStream;
        locale = _locale;
    }

    /**
     * A request to print a string.
     *
     * @param _string The string to be printed
     * @return The request.
     */
    public SyncRequest<Void> printlnSReq(final String _string) {
        return new SyncBladeRequest<Void>() {
            @Override
            public Void processSyncRequest() throws Exception {
                System.out.println(_string);
                return null;
            }
        };
    }

    /**
     * A request to print a formated string.
     *
     * @param _format The formatting.
     * @param _args   The data to be formatted.
     * @return The request.
     */
    public SyncRequest<Void> printfSReq(final String _format,
            final Object... _args) {
        return new SyncBladeRequest<Void>() {
            @Override
            public Void processSyncRequest() throws Exception {
                printStream.print(String.format(locale, _format, _args));
                return null;
            }
        };
    }
}