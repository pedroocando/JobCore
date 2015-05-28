package utils;

import backend.ServerInstance;
import com.sun.mail.smtp.SMTPTransport;
import com.sun.xml.messaging.saaj.util.ByteOutputStream;
import exceptions.CouldNotCreateInstanceException;
import models.Config;
import play.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.PrintStream;
import java.security.Security;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Properties;


/**
 * Clase que genera las alarmas
 * @author plesse
 */
public class Alarm {
    private static Hashtable<String, Long> tabla = new Hashtable<String, Long>(40);

    /**
     * Mensaje (e-mail) a enviar como Alarma
     * @param recipients A quien va destinado el Mail
     * @param subject Asunto (Titulo de la Alarma)
     * @param message mensaje a enviar
     * @param e es la exception que lanza el error
     */
    public static void sendMail(String recipients[], String subject, String message, Throwable e) {

        //ByteArrayOutputStream ba = new ByteArrayOutputStream();
        ByteOutputStream ba = new ByteOutputStream();
        PrintStream ps = new PrintStream(ba);
        e.printStackTrace(ps);
        String str = new String(ba.getBytes(), 0, ba.getCount());
        sendMail(recipients, subject, message + "\n\n" + str);
    }

    /**
     * Mensaje (e-mail) a enviar como Alarma
     * @param recipients A quien va destinado el Mail
     * @param subject Asunto (Titulo de la Alarma)
     * @param message mensaje a enviar
     */
    public static void sendMail(String recipients[], String subject, String message) {
        try {
            subject = ServerInstance.instanceName + " - " + subject;
            Long o = tabla.get(subject);
            if (o != null) {
                if ((System.currentTimeMillis() - o) < Config.getLong("alarm-send-millis")) {
                    return;
                }
            }

            tabla.put(subject, System.currentTimeMillis());

            boolean debug = false;
            GregorianCalendar g = new GregorianCalendar();
            message = g.getTime().toString() + "\n\n" + message;

            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            //Set the host smtp address
            Properties props = new Properties();
            props.put("mail.smtp.host", Config.getString("alarm-smtp-server"));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.transport.protocol", "smtps");
            props.put("mail.smtp.port", "465");

            // create some properties and get the default Session
            Session session = Session.getInstance(props);
            session.setDebug(debug);

            // create a message
            Message msg = new MimeMessage(session);

            // set the from and to address
            InternetAddress addressFrom = new InternetAddress(Config.getString("alarm-sender-address"));
            msg.setFrom(addressFrom);

            InternetAddress[] addressTo = new InternetAddress[recipients.length];
            StringBuffer aux = new StringBuffer();
            for (int i = 0; i < recipients.length; i++) {
                addressTo[i] = new InternetAddress(recipients[i]);
                aux.append(recipients[i]).append(",");
            }
            msg.setRecipients(Message.RecipientType.TO, addressTo);
            Logger.info("Sending alarm to: " + aux.toString());

            // Optional : You can also set your custom headers in the Email if you Want
            //msg.addHeader("MyHeaderName", "myHeaderValue");

            // Setting the Subject and Content Type

            msg.setSubject(subject);
            msg.setContent(message, "text/plain");

            //Transport.send(msg);
            msg.saveChanges();
            SMTPTransport t = null;
            try {
                t = (SMTPTransport) session.getTransport("smtps");
            } catch (NoSuchProviderException e) {
                Logger.error("Error en la alarma.", e);
                e.printStackTrace();
            }

            try {
                t.connect(Config.getString("alarm-smtp-server"), Config.getString("alarm-sender-address"), Config.getString("alarm-sender-pw"));
            } catch (MessagingException e) {
                Logger.error("Error en la alarma.", e);
                e.printStackTrace();
            }

            try {
                //t.send(msg);
                t.sendMessage(msg, msg.getAllRecipients());
                t.close();
            } catch (SendFailedException e) {
                e.printStackTrace();
                Logger.error("Fatal error sending alarms.", e);
            } catch (MessagingException e) {
                e.printStackTrace();
                Logger.error("Fatal error sending alarms.", e);
            }

        } catch (Exception e) {
            Logger.error("Fatal error sending alarms.", e);
        }
    }

    /**
     * Mensaje (e-mail) a enviar como Reporte
     * @param recipients A quien va destinado el Mail
     * @param subject Asunto (Titulo de la Alarma)
     * @param message mensaje a enviar
     */
    public static void sendReportMail(String recipients[], String subject, String message) {
        //Logger logger = LoggerFactory.getLogger(Alarm.class);
//		Logger logger = Logger.getLogger(Alarm.class);

        String host = Config.getString("alarm-smtp-server");
        String password = Config.getString("reports-sender-pw");

        try {
            subject = Config.getString("app-name")+" - "+subject;
            Long o = tabla.get(subject);
            if (o != null) {
                if ((System.currentTimeMillis() - o) < Config.getLong("alarm-send-millis")) {
                    return;
                }
            }

            tabla.put(subject, System.currentTimeMillis());

            boolean debug = false;
            GregorianCalendar g = new GregorianCalendar();
            message = g.getTime().toString() + "\n\n" + message;

            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            //Set the host smtp address
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.transport.protocol", "smtps");
            props.put("mail.smtp.port", "587");

            // create some properties and get the default Session
            Session session = Session.getInstance(props);
            session.setDebug(debug);

            // create a message
            Message msg = new MimeMessage(session);

            // set the from and to address
            String from = Config.getString("reports-sender-address");
            InternetAddress addressFrom = new InternetAddress(from);
            msg.setFrom(addressFrom);

            InternetAddress[] addressTo = new InternetAddress[recipients.length];
            StringBuffer aux = new StringBuffer();
            for (int i = 0; i < recipients.length; i++) {
                addressTo[i] = new InternetAddress(recipients[i]);
                aux.append(recipients[i]).append(",");
            }
            msg.setRecipients(Message.RecipientType.TO, addressTo);
            Logger.info("Sending Report to: " + aux.toString());

            // Optional : You can also set your custom headers in the Email if you Want
            //msg.addHeader("MyHeaderName", "myHeaderValue");

            // Setting the Subject and Content Type

            msg.setSubject(subject);
            msg.setContent(message, "text/plain");

            //Transport.send(msg);
            msg.saveChanges();
            SMTPTransport t = null;
            try {
                t = (SMTPTransport) session.getTransport("smtps");
            } catch (NoSuchProviderException e) {
                Logger.error("Error en la reporte.", e);
                e.printStackTrace();
            }

            try {
                //t.connect(Config.getString("alarm-smtp-server"), Config.getString("alarm-sender-address"), Config.getString("alarm-sender-pw"));
                t.connect(host, from, password);
            } catch (MessagingException e) {
                Logger.error("Error en el reporte.", e);
                e.printStackTrace();
            }

            try {
                //t.send(msg);
                t.sendMessage(msg, msg.getAllRecipients());
                t.close();
            } catch (SendFailedException e) {
                e.printStackTrace();
                Logger.error("Fatal error sending reports.", e);
            } catch (MessagingException e) {
                e.printStackTrace();
                Logger.error("Fatal error sending reports.", e);
            }

        } catch (Exception e) {
            Logger.error("Fatal error sending reports.", e);
        }
    }


    /**
     * Mensaje (e-mail) a enviar como Alarma
     * @param recipients A quien va destinado el Mail
     * @param subject Asunto (Titulo de la Alarma)
     * @param message mensaje a enviar
     */
    public static void sendMailHtml(String recipients[], String subject, String message){
        //Logger logger = LoggerFactory.getLogger(Alarm.class);
//		 Logger logger = Logger.getLogger(Alarm.class);

        try {
            Long o = tabla.get(subject);
            if (o != null) {
                if ((System.currentTimeMillis() - o) < Config.getLong("alarm-send-millis")) {
                    return;
                }
            }

            tabla.put(subject, System.currentTimeMillis());

            boolean debug = false;
            GregorianCalendar g = new GregorianCalendar();
            message = g.getTime().toString() + "\n\n"+  "<br></br>"+ message;

            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            //Set the host smtp address
            Properties props = new Properties();
            props.put("mail.smtp.host", Config.getString("alarm-smtp-server"));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.port", "587");

            // create some properties and get the default Session
            Session session = Session.getInstance(props);
            session.setDebug(debug);

            // create a message
            Message msg = new MimeMessage(session);

            // set the from and to address
            InternetAddress addressFrom = new InternetAddress(Config.getString("alarm-sender-address"));
            msg.setFrom(addressFrom);

            InternetAddress[] addressTo = new InternetAddress[recipients.length];
            StringBuffer aux = new StringBuffer();
            for (int i = 0; i < recipients.length; i++) {
                addressTo[i] = new InternetAddress(recipients[i]);
                aux.append(recipients[i]).append(",");
            }
            msg.setRecipients(Message.RecipientType.TO, addressTo);
            Logger.info("Sending alarm to: " + aux.toString());

            // Optional : You can also set your custom headers in the Email if you Want
            //msg.addHeader("MyHeaderName", "myHeaderValue");

            // Setting the Subject and Content Type

            msg.setSubject(subject);
            msg.setContent(message, "text/html");

            //Transport.send(msg);
            msg.saveChanges();
            SMTPTransport t = null;
            try {
                t = (SMTPTransport) session.getTransport("smtps");
            } catch (NoSuchProviderException e) {
                Logger.error("Error en la alarma.", e);
                e.printStackTrace();
            }

            try {
                t.connect(Config.getString("alarm-smtp-server"), Config.getString("alarm-sender-address"), Config.getString("alarm-sender-pw"));
            } catch (MessagingException e) {
                Logger.error("Error en la alarma.", e);
                e.printStackTrace();
            }

            try {
                //t.send(msg);
                t.sendMessage(msg, msg.getAllRecipients());
                t.close();
            } catch (SendFailedException e) {
                e.printStackTrace();
                Logger.error("Fatal error sending alarms.", e);
            } catch (MessagingException e) {
                e.printStackTrace();
                Logger.error("Fatal error sending alarms.", e);
            }

        } catch (Exception e) {
            Logger.error("Fatal error sending alarms.", e);
        }
    }
}
