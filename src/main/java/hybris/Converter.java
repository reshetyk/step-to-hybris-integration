package hybris;

import org.apache.camel.Exchange;

import java.util.Date;

/**
 * @author alre
 */
public class Converter {

    public String convert (Exchange exchange) {
        System.out.println("--- Converter caught the message: " + exchange.getIn().getMessageId());
    return "created: " + new Date() + "\n\nThe input message was: \n\n" + exchange.getIn().getBody();
}
}
