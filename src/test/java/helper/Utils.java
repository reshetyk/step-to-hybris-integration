package helper;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.SimpleStoredMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.sftp.SftpSubsystem;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author alre
 */
public class Utils {

    public static final String TEST_EMAIL_LOGIN = "login";
    public static final String TEST_EMAIL_PASSWORD = "password";
    public static final String TEST_MAIL_SERVER_HOST = "localhost";
    public static final String TEST_EMAIL_ADDR_TO = "admin@" + TEST_MAIL_SERVER_HOST;
    public static final int SMTP_SERVER_PORT = 55555;
    public static final int POP_3_SERVER_PORT = 3110;

    public static MimeMessage extractMimeMessage(GreenMail greenMail, GreenMailUser greenMailUser) throws FolderException {
        final MailFolder inbox = greenMail.getManagers().getImapHostManager().getInbox(greenMailUser);
        assertFalse("there are no emails", inbox.getMessages().isEmpty());
        return ((SimpleStoredMessage) inbox.getMessages().iterator().next()).getMimeMessage();
    }

    public static GreenMailUser initGreenMailUser(GreenMail greenMail) {
        return greenMail.setUser(TEST_EMAIL_ADDR_TO, TEST_EMAIL_LOGIN, TEST_EMAIL_PASSWORD);
    }

    public static GreenMail initGreenMail() {
        return new GreenMail(new ServerSetup[]{
                new ServerSetup(SMTP_SERVER_PORT, TEST_MAIL_SERVER_HOST, ServerSetup.PROTOCOL_SMTP),
                new ServerSetup(POP_3_SERVER_PORT, TEST_MAIL_SERVER_HOST, ServerSetup.PROTOCOL_POP3)
        });
    }

    public static SshServer createAndStartSshServer(int port) throws IOException {
        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setHost("localhost");
        sshd.setPort(port);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("src/test/resources/hostkey.ser"));

        List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<>();
        userAuthFactories.add(new UserAuthNone.Factory());
        sshd.setUserAuthFactories(userAuthFactories);

        sshd.setCommandFactory(new ScpCommandFactory());

        List<NamedFactory<Command>> namedFactoryList = new ArrayList<>();
        namedFactoryList.add(new SftpSubsystem.Factory());
        sshd.setSubsystemFactories(namedFactoryList);
        sshd.start();
        return sshd;
    }

    public static void waitUntilHotFolderHasFiles(String hybrisHotFolderPath, String ext, int countFilesExpected, int countAttempts) throws InterruptedException {
        Collection<File> files = FileUtils.listFiles(new File(hybrisHotFolderPath), new String[]{ext}, false);

        int attempts = 0;
        while (files.size() != countFilesExpected) {
            files = FileUtils.listFiles(new File(hybrisHotFolderPath), new String[]{ext}, false);
            Thread.currentThread().sleep(3000);
            if (++attempts == countAttempts) {
                fail("timeout for waiting " + countFilesExpected + " with extension '" + ext + " in the folder '" + hybrisHotFolderPath);
            }
        }
    }

    public static void clearDir(String hybrisPath) throws IOException {
        FileUtils.deleteDirectory(new File(hybrisPath));
        new File(hybrisPath).mkdir();
    }

}
