package no.uio.ifi.qure;

import java.text.DecimalFormat;

public class Progress {

    private String prefix;
    private final double total, step, stepPercent;
    private int stepsDone;
    private double totalDone;
    private double progress;
    private int longestStringLength = 0;
    private boolean printOnEveryUpdate;
    private boolean convertToLong = true;
    private int numberOfDecimals = 2;
    private DecimalFormat df;

    public Progress(String prefix, double total, double stepPercent, String format) {
        this.prefix = prefix;
        this.total = total;
        this.step = (total/100.0) * stepPercent;
        this.stepPercent = stepPercent;
        df = new DecimalFormat(format);
        printOnEveryUpdate = false;
    }

    public Progress(String prefix, double total, double stepPercent, String format, 
                    boolean printOnEveryUpdate) {
        this.prefix = prefix;
        this.total = total;
        this.step = (total/100.0) * stepPercent;
        this.stepPercent = stepPercent;
        this.printOnEveryUpdate = printOnEveryUpdate;
        df = new DecimalFormat(format);
    }

    public void setConvertToLong(boolean convertToLong) {
        this.convertToLong = convertToLong;
    }

    public void setNumberOfDecimals(int numberOfDecimals) {
        this.numberOfDecimals = numberOfDecimals;
    }

    public void init() {

        progress = 0;
        stepsDone = 0;
        printProgress();
        totalDone = 0;
    }

    private void printProgress() {

        String progressStr = df.format(stepsDone * stepPercent) + "%";
        if (convertToLong)
            progressStr += " [" + ((long) totalDone) + "/" + ((long) total) + "]";
        else
            progressStr += " [" + totalDone + "/" + total + "]";
        int ls = prefix.length() + progressStr.length();

        String mid = " ";
        for (int i = 0; i < (longestStringLength - ls); i++) mid += " ";

        String out = "\r" + prefix + mid + progressStr;
        System.out.print(out);

        if (longestStringLength < ls) longestStringLength = ls;
    }

    public void update(String prefix, double done) {

        this.prefix = prefix;

        progress += done;
        totalDone += done;
        if (progress >= step) {
            double surp = Math.floor(progress/step);
            stepsDone += surp;
            progress = progress - (surp*step);
        }  
        printProgress();
    }

    public void update(double done) {

        progress += done;
        totalDone += done;

        boolean percentUpdate = false;

        if (progress >= step) {
            double surp = Math.floor(progress/step);
            stepsDone += surp;
            progress = progress - (surp*step);
            percentUpdate = true;
        }  
        if (percentUpdate || printOnEveryUpdate) printProgress();
    }

    public void update(String prefix) {

        this.prefix = prefix;

        totalDone++;
        progress++;

        if (progress >= step) {
            stepsDone++;
            progress = 0;
        }
        printProgress();
    }

    public void update() {

        progress++;
        totalDone++;

        boolean percentUpdate = false;

        if (progress >= step) {
            stepsDone++;
            printProgress();
            progress = 0;
            percentUpdate = true;
        }
        if (percentUpdate || printOnEveryUpdate) printProgress();
    }

    public void done() {
        String done = prefix + " Done";
        String end = " ";
        for (int i = 0; i < (longestStringLength) - done.length(); i++) end += " ";
        System.out.println("\r" + done + end);
    }

    public void done(String prefix) {

        this.prefix = prefix;

        String done = prefix + " Done";
        String end = " ";
        for (int i = 0; i < (longestStringLength) - done.length(); i++) end += " ";
        System.out.println("\r" + done + end);
    }
}
