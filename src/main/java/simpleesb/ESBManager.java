package simpleesb;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

import java.util.Arrays;
import java.util.Scanner;

import static org.apache.activemq.camel.component.ActiveMQComponent.activeMQComponent;
import static simpleesb.StepToHybrisRoutes.*;

/**
 * @author alre
 */
public class ESBManager {

    private CamelContext context;

    public static void main(String[] args) {
        System.out.println("args=" + Arrays.toString(args));
        ESBManager esbManager = null;
        try {
            if (args == null || args.length == 0) {
                System.out.println("Please, set broker url, hybris hot-folder endpoint, smtp endpoint");
                return;
            }
            esbManager = getDefaultInstanceAndStart(args[0], args[1], args[2]);

            System.out.println("Press 'n' to stop ESB!");

            Scanner s = new Scanner(System.in);
            while (true) {
                if (s.next().equals("n")) {
                    esbManager.stopCamel();
                    System.out.println("exit");
                    break;
                }
            }

        } catch (Exception e) {
            if (esbManager != null)
                esbManager.stopCamel();

            throw new RuntimeException(e);
        }


    }

    public static ESBManager getDefaultInstanceAndStart(String brokerUrl, String hybisHotfolderEndpoint, String smtpEndpoint) {
        System.out.println("trying to start broker on the url:" + brokerUrl);
        ESBManager esbManager = new ESBManager();
        try {
            esbManager.startCamel(brokerUrl);
            StepToHybrisRoutes routes = new StepToHybrisRoutes(esbManager.getCamelContext());
            routes.createRouteToConvertXmlToCsv(STEPXML_QUEUE, DELIVERY_QUEUE);
            routes.createRouteToDeliverImpexToHotfolder(DELIVERY_QUEUE, hybisHotfolderEndpoint);
            routes.createRouteToSendEmails(SEND_EMAIL_QUEUE, smtpEndpoint);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return esbManager;

    }

    public void startCamel(String brokerURL) {
        try {
            context = new DefaultCamelContext();
            context.addComponent("activemq", activeMQComponent(brokerURL));
            context.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stopCamel() {
        try {
            context.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CamelContext getCamelContext() {
        return context;
    }


}
