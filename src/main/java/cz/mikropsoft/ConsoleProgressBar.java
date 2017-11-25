package cz.mikropsoft;
import java.util.Arrays;

/**
 * Console Progress bar
 *
 * The output looks like this:
 *
 * <pre>
 * 100% [================================] in 5,00s
 * </pre>
 *
 * The bar increments on the same cursor line.
 *
 * If you run the progress bar in ant, remember to set the system property
 * <sysproperty key="run.with.ant" value="false" />
 *
 * @author c.cerbo
 *
 */
public class ConsoleProgressBar {
    final private static boolean RUN_WITH_ANT = new Boolean(System
            .getProperty("run.with.ant"));

    private long total;

    private long startTime;

    private String label;

    private int currentPct = 0;

    private int margin = 0;

    /**
     * Create a new instance and start to measure the time
     *
     * @param total
     */
    public ConsoleProgressBar(long total) {
        this(null, total);
    }

    public ConsoleProgressBar(String label, long total) {
        this.total = total;
        this.startTime = System.currentTimeMillis();
        this.label = label;
        printFullBar();
    }

    /**
     * The percentage progress is displayed only if it's a multiple of margin.
     *
     * @param margin
     *            The margin for the display. It cannot be greater than 50 and
     *            only multiple of 5 allowed. Default value is zero, and it
     *            means than every progress should be printed.
     */
    public void setMargin(int margin) {
        if ((margin % 5) != 0) {
            throw new IllegalArgumentException("Margin: Only multiple of 5 allowed!");
        }

        if (margin > 50) {
            throw new IllegalArgumentException("Margin cannot be greater than 50!");
        }

        this.margin = margin;
    }

    /**
     * Update the progress bar with the current progress
     *
     * @param current
     *            The current absolute value. The percentage value will be
     *            calculated.
     */
    public void setCurrent(long current) {
        int tempPct = (int) (100 * current / total);

        if (tempPct == currentPct) {
            return;
        }

        if ((tempPct - currentPct) < margin) {
            return;
        }

        currentPct = tempPct;

        printFullBar();

        long elapsed = (System.currentTimeMillis() - startTime);
        long estimatedTotal = currentPct == 0 ? 0 : Math.round(elapsed
                / (currentPct / 100.0));
        long remaining = estimatedTotal - elapsed;
//        System.out.printf(" Elapsed: %s - Remaining: %s", DateTimeUtil
//                .formatMilliseconds(elapsed), DateTimeUtil.formatMilliseconds(remaining));

        if (current >= total) {
            System.out.println();
        }
    }

    private void printFullBar() {
        char backspace = RUN_WITH_ANT ? '\n' : '\r';
        if (label != null) {
            System.out.printf(backspace + "%s %3d%% %s", label, currentPct, bar());
        } else {
            System.out.printf(backspace + "%3d%% %s", currentPct, bar());
        }
    }

    private String bar() {
        int k = 3;
        int scale = currentPct / k;
        int length = 100 / k + 1;

        char[] line = new char[length];
        Arrays.fill(line, ' ');
        line[0] = '[';
        line[length - 1] = ']';
        if (scale > 0) {
            Arrays.fill(line, 1, scale, '=');
        }

        return String.valueOf(line);
    }

    public static void main(String[] args) throws Exception {
        int n = 130;
        ConsoleProgressBar progressBar = new ConsoleProgressBar("test", n);
        progressBar.setMargin(5);
        for (int i = 0; i <= n; i++) {
            progressBar.setCurrent(i);
            Thread.sleep(50);
        }
    }

}
