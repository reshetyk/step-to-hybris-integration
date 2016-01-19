import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import helper.Utils;
import org.apache.camel.Exchange;
import org.junit.Test;
import simpleesb.ESBManager;
import simpleesb.StepToHybrisRoutes;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.assertEquals;

import static simpleesb.StepToHybrisRoutes.*;
import static helper.Utils.*;

/**
 * @author alre
 */
public class StepToHybrisBadCaseTest {

    /**
     * Check if ssh server does not response after several attempts
     * send email to specific email with errors AND
     * push original undelivered message to the dead letter queue
     */
    @Test
    public void ifSshDoesNotResponseSendEmail() throws Exception {
        ESBManager esbManager = new ESBManager();
        esbManager.startCamel(StepToHybrisIntegrationTest.BROKER_URL);
        StepToHybrisRoutes routes = new StepToHybrisRoutes(esbManager.getCamelContext());

        GreenMail greenMail = Utils.initGreenMail();
        GreenMailUser greenMailUser = Utils.initGreenMailUser(greenMail);
        greenMail.start();

        //create route with delivery sftp invalid host
        final String sFtpIncorrectEndpoint = "sftp://test@incorrecthost/dir?reconnectDelay=0";
        routes.createRouteToDeliverImpexToHotfolder(DELIVERY_QUEUE, sFtpIncorrectEndpoint);
        routes.createRouteToSendEmails(SEND_EMAIL_QUEUE, StepToHybrisIntegrationTest.SMTP_ENDPOINT);

        esbManager.getCamelContext().createProducerTemplate().sendBody(DELIVERY_QUEUE, "the content of impex file");

        greenMail.waitForIncomingEmail(20000, 1);

        //check content message
        MimeMessage mimeMessage = Utils.extractMimeMessage(greenMail, greenMailUser);
        assertEquals(TEST_EMAIL_ADDR_TO, mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("Delivery impex file failed", mimeMessage.getSubject());
        assertThat(mimeMessage.getContent().toString(), containsString("Cannot connect to sftp://test@incorrecthost"));

        //check that the original message was pushed to the dead letter queue
        final Exchange exchange = esbManager.getCamelContext().createConsumerTemplate().receive(DELIVERY_DEAD_LETTER_QUEUE);
        assertEquals("the content of impex file", exchange.getIn().getBody());

        esbManager.stopCamel();
        greenMail.stop();

    }



}
