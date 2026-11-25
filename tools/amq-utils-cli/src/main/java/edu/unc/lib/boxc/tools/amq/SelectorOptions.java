package edu.unc.lib.boxc.tools.amq;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;

/**
 * Options for restricting messages returned by a JMS queue browser or consumer.
 *
 * @author bbpennel
 */
public class SelectorOptions {
    @Option(names = { "-s", "--selector" }, description = "Optional JMS selector.")
    private String selector;

    @Option(names = "--after", converter = UtcTimestampConverter.class,
            paramLabel = "<yyyy-MM-ddTHH:mm:ss>",
            description = "Only select messages after this UTC timestamp.")
    private Long after;

    public String getSelector() {
        if (after == null) {
            return selector;
        }

        String timestampSelector = "JMSTimestamp > " + after;
        return selector == null ? timestampSelector : timestampSelector + " AND (" + selector + ")";
    }

    /**
     * Converts a timezone-less ISO date-time into epoch milliseconds in UTC.
     *
     * @author bbpennel
     */
    public static class UtcTimestampConverter implements ITypeConverter<Long> {
        @Override
        public Long convert(String value) {
            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli();
        }
    }
}
