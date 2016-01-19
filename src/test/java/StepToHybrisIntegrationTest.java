import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import helper.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.SshServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import simpleesb.ESBManager;
import simpleesb.StepToHybrisRoutes;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.io.File;

import static helper.Utils.*;
import static org.junit.Assert.assertEquals;
import static simpleesb.StepToHybrisRoutes.*;

/**
 * @author alre
 */
public class StepToHybrisIntegrationTest {
    public static final int SSH_PORT = 22222;
    public static final String SMTP_ENDPOINT = "smtp://" + TEST_EMAIL_ADDR_TO + ":" + SMTP_SERVER_PORT + "?password=" + TEST_EMAIL_PASSWORD + "&username=" + TEST_EMAIL_LOGIN;
    public static final String HOT_FOLDER_PATH = "src/test/resources/hot-folder";
    public static final String SFTP_HOTFOLDER_ENDPOINT = "sftp://none@localhost:" + SSH_PORT + "/" + HOT_FOLDER_PATH + "?fileName=output.impex&stepwise=false";
    public static final String BROKER_URL = "vm://localhost:61616";

    private GreenMail greenMail;
    private GreenMailUser greenMailUser;
    private ESBManager esbManager;
    private StepToHybrisRoutes routes;
    private SshServer sshServer;

    @Before
    public void setUp() throws Exception {
        //clear ActiveMQ data, to be sure that there are no old messages
        FileUtils.deleteDirectory(new File("activemq-data"));

        esbManager = ESBManager.getDefaultInstanceAndStart(BROKER_URL, SFTP_HOTFOLDER_ENDPOINT, SMTP_ENDPOINT);
        routes = new StepToHybrisRoutes(esbManager.getCamelContext());
        sshServer = createAndStartSshServer(SSH_PORT);

        greenMail = Utils.initGreenMail();
        greenMailUser = Utils.initGreenMailUser(greenMail);
        greenMail.start();
    }

    @After
    public void tearDown() throws Exception {
        esbManager.stopCamel();
        sshServer.stop();
        greenMail.stop();
    }

    /**
     * The full test that proves and demonstrates the whole way from converting xml to csv and
     * delivering impex file to hybris hot-folder through sftp
     *
     * @throws Exception
     */
    @Test
    public void convertStepXmlToCsvAndDeliverToHybrisHotfolder() throws Exception {
        clearDir(HOT_FOLDER_PATH);

        //send stepxml message to the input stepxml queue
        esbManager.getCamelContext().createProducerTemplate().sendBody(STEPXML_QUEUE, "This is step xml content");

        //check that hot-folder has impex file
        waitUntilHotFolderHasFiles(HOT_FOLDER_PATH, "impex", 1, 5);
    }

    /**
     * Check that messages which come into the '{@value simpleesb.StepToHybrisRoutes#DELIVERY_QUEUE}'
     * queue will be delivered by sftp to the destination point
     *
     * @throws Exception
     */
    @Test
    public void deliverImpexFileBySftp() throws Exception {

        clearDir(HOT_FOLDER_PATH);

        esbManager.getCamelContext().createProducerTemplate().sendBody(DELIVERY_QUEUE, "this is impex file");

        waitUntilHotFolderHasFiles(HOT_FOLDER_PATH, "impex", 1, 5);

    }

    /**
     * Check that messages which come into the '{@value simpleesb.StepToHybrisRoutes#SEND_EMAIL_QUEUE}'
     * queue will be sent to the email inbox
     *
     * @throws Exception
     */
    @Test
    public void sendEmails() throws Exception {
        final String subject = "test message";
        final String bodyMessage = "Test Message: hello world!";

        esbManager.getCamelContext().createProducerTemplate().sendBodyAndHeader(SEND_EMAIL_QUEUE, bodyMessage, "Subject", subject);

        greenMail.waitForIncomingEmail(1);

        MimeMessage receivedMimeMessage = Utils.extractMimeMessage(greenMail, greenMailUser);

        assertEquals(TEST_EMAIL_ADDR_TO, receivedMimeMessage.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals(subject, receivedMimeMessage.getSubject());
        assertEquals(bodyMessage, receivedMimeMessage.getContent().toString().trim());
    }
}
