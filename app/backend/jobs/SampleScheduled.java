package backend.jobs;

import backend.HecticusThread;

import java.util.Map;

/**
 * Created by plessmann on 25/05/15.
 */
public class SampleScheduled extends HecticusThread {
    @Override
    public void process(Map args) {
        System.out.println("this is not a daemon " + getJob().getName());
    }
}
