package edu.unc.lib.boxc.deposit;

/**
 * 
 * @author count0
 *
 */
public class GetProperty {
    private GetProperty() {
    }

    /**
     * Prints out system property values based on command line arguments.
     * Copied from https://github.com/rfkrocktk/commons-daemon-example
     * 
     * @param args
     *            Names of system properties to print values for.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err
                    .println("ERROR: Please pass a system property name an an argument "
                            + "to the program.");
            System.exit(1);
        }

        for (String property : args) {
            if (!System.getProperties().containsKey(property)) {
                System.err.println(String.format(
                        "ERROR: Unable to find property '%s'.", property));
                System.exit(1);
            }
        }

        for (String property : args) {
            System.out.println(System.getProperty(property));
        }
    }

}
