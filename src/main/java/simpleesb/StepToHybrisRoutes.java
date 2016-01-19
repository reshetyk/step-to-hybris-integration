package simpleesb;

import hybris.Converter;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.builder.RouteBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author alre
 */
public class StepToHybrisRoutes {
    public static final String STEPXML_QUEUE = "activemq:queue:HYBRIS.CONVERTER.INPUT_STEPXML";

    public static final String DELIVERY_QUEUE = "activemq:queue:HYBRIS.CONVERTER.DELIVERY";
    public static final String DELIVERY_DEAD_LETTER_QUEUE = "activemq:queue:HYBRIS.CONVERTER.DELIVERY.DEAD_LETTER";

    public static final String SEND_EMAIL_QUEUE = "activemq:queue:SEND_EMAIL";
    public static final String SEND_EMAIL_DEAD_LETTER_QUEUE = "activemq:queue:SEND_EMAIL.DEAD_LETTER";

    private CamelContext context;

    public StepToHybrisRoutes(CamelContext context) {
        this.context = context;
    }


    public void createRouteToConvertXmlToCsv(final String queueQueue, final String outputQueue) throws Exception {

        final RouteBuilder builder = new RouteBuilder() {
            public void configure() {
                from(queueQueue)
                        .bean(Converter.class)
                        .to(outputQueue);
            }
        };
        context.addRoutes(builder);
    }

    public void createRouteToSendEmails(final String fromQueue, final String toQueue) throws Exception {

        final RouteBuilder builder = new RouteBuilder() {
            public void configure() {
                from(fromQueue).to(toQueue);
            }
        };
        builder.setErrorHandlerBuilder(new DeadLetterChannelBuilder(SEND_EMAIL_DEAD_LETTER_QUEUE));
        context.addRoutes(builder);
    }

    public void createRouteToDeliverImpexToHotfolder(final String inputQueue, final String outputDest) throws Exception {

        final RouteBuilder builder = new RouteBuilder() {
            public void configure() {
                from(inputQueue).to(outputDest);
            }
        };
        builder.onException(Throwable.class)
                .maximumRedeliveries(2)
                .maximumRedeliveryDelay(1000)
                .multicast()
                .to(DELIVERY_DEAD_LETTER_QUEUE)
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Throwable caused = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
                        StringWriter sw = new StringWriter();
                        caused.printStackTrace(new PrintWriter(sw));
                        exchange.getIn().setBody(sw.toString());
                        exchange.getIn().setHeader("Subject", "Delivery impex file failed");
                        exchange.getContext().createProducerTemplate().send(SEND_EMAIL_QUEUE, exchange);
                    }
                });

        context.addRoutes(builder);
    }

}
