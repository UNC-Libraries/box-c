package edu.unc.lib.boxc.tools.amq;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.TextMessage;

/**
 * Formats JMS messages for command-line inspection.
 *
 * @author bbpennel
 */
final class MessagePrinter {
    private MessagePrinter() {
    }

    static void print(int number, Message message, int bodyLimit, PrintWriter output) throws JMSException {
        output.printf("Message %d%n", number);
        output.printf("  id: %s%n", message.getJMSMessageID());
        output.printf("  timestamp: %d%n", message.getJMSTimestamp());
        output.printf("  redelivered: %s%n", message.getJMSRedelivered());
        output.printf("  body: %s%n", getBody(message, bodyLimit));

        List<String> properties = new ArrayList<>();
        Enumeration<?> propertyNames = message.getPropertyNames();
        while (propertyNames.hasMoreElements()) {
            properties.add((String) propertyNames.nextElement());
        }
        Collections.sort(properties);
        for (String property : properties) {
            output.printf("  property.%s: %s%n", property, message.getObjectProperty(property));
        }
    }

    private static String getBody(Message message, int bodyLimit) throws JMSException {
        int length = Math.max(0, bodyLimit);
        if (message instanceof TextMessage textMessage) {
            return truncate(textMessage.getText(), length);
        }
        if (message instanceof BytesMessage bytesMessage) {
            bytesMessage.reset();
            byte[] bytes = new byte[(int) Math.min(bytesMessage.getBodyLength(), length)];
            int read = bytesMessage.readBytes(bytes);
            String value = read <= 0 ? "" : new String(bytes, 0, read, StandardCharsets.UTF_8);
            return truncate(value, length) + " (bytes)";
        }
        if (message instanceof MapMessage mapMessage) {
            List<String> values = new ArrayList<>();
            Enumeration<?> names = mapMessage.getMapNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                values.add(name + "=" + mapMessage.getObject(name));
            }
            Collections.sort(values);
            return truncate(String.join(", ", values), length) + " (map)";
        }
        if (message instanceof ObjectMessage objectMessage) {
            try {
                Object object = objectMessage.getObject();
                if (object == null) {
                    return "null (object)";
                }
                return truncate(String.valueOf(object), length) + " (object: "
                        + object.getClass().getName() + ")";
            } catch (JMSException e) {
                return "(ObjectMessage body unavailable: " + e.getMessage() + ")";
            }
        }
        return "(" + message.getClass().getSimpleName() + ")";
    }

    private static String truncate(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return String.valueOf(value);
        }
        return value.substring(0, limit) + "...";
    }
}
