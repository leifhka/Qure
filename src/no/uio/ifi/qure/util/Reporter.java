package no.uio.ifi.qure.util;

/**
 * Class used for reporting progress when using multiple threads.
 */
public class Reporter {

	private double progress, step;
	private Progress prog;

	Reporter(Progress prog, double step) {
		this.prog = prog;
		this.step = step;
		progress = 0;
	}
	public Reporter newReporter() { return prog.makeReporter(); }

	public void update(double done) {
		progress += done;
		if (progress >= step) {
			prog.update(progress);
			progress = 0;
		}
	}

	public void update() {
		progress++;
		if (progress >= step) {
			prog.update(progress);
			progress = 0;
		}
	}
}
