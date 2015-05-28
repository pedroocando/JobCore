package backend.jobs;

import backend.HecticusThread;

import java.util.Map;

/**
 * Created by plessmann on 25/05/15.
 */
public class SampleDaemon  extends HecticusThread {
    @Override
    public void process(Map args) {
        System.out.println("this is a daemon " + getJob().getName());
    }
}
//backend.jobs.SampleDaemon
//backend.jobs.SampleScheduled