package backend;

import akka.actor.Cancellable;
import exceptions.CouldNotCreateInstanceException;
import models.Config;
import models.Job;
import utils.Utils;

import javax.persistence.OptimisticLockException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Clase base de los hilos del PMC
 *
 * Created by plesse on 7/9/14.
 */
public abstract class HecticusThread implements Runnable {
    private String name;
    private long initTime;
    private AtomicBoolean run;
    private long prevTime;
    private long actTime;
    private boolean active;
    private Map params;
    private int idApp;
    private Cancellable cancellable;
    private Job job;

    public HecticusThread() {
        this.name = "";
        try {
            run = ServerInstance.getInstance().isInstanceRun();
        } catch (CouldNotCreateInstanceException ex){

        }
    }

    protected HecticusThread(String name,  AtomicBoolean run, Cancellable cancellable) {
        this.initTime = this.actTime = this.prevTime = System.currentTimeMillis();
        this.name = name+"-"+initTime;
        this.run = run;
        this.cancellable = cancellable;
        this.active = false;
    }

    protected HecticusThread(String name,  AtomicBoolean run) {
        this.initTime = this.actTime = this.prevTime = System.currentTimeMillis();
        this.name = name+"-"+initTime;
        this.run = run;
        this.active = false;
    }

    protected HecticusThread(AtomicBoolean run) {
        this.initTime = this.actTime = this.prevTime = System.currentTimeMillis();
        this.name = "HecticusThread-"+initTime;
        this.run = run;
        this.active = false;
    }

    /**
     * Metodo que ejecuta la funcionalidad real de un HecticusThread
     */
    public abstract void process(Map args);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getInitTime() {
        return initTime;
    }

    public AtomicBoolean getRun() {
        return run;
    }

    public boolean isActive() {
        return active;
    }

    public Cancellable getCancellable() {
        return cancellable;
    }

    public void setCancellable(Cancellable cancellable) {
        this.cancellable = cancellable;
    }

    public Map getParams() {
        return params;
    }

    public void setParams(Map params) {
        this.params = params;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public void setRun(AtomicBoolean run) {
        this.run = run;
    }

    public void setAlive(){
        prevTime = actTime;
        actTime = System.currentTimeMillis();
    }

    public void setInitTime(long initTime) {
        this.initTime = initTime;
    }

    public long getPrevTime() {
        return prevTime;
    }

    public void setPrevTime(long prevTime) {
        this.prevTime = prevTime;
    }

    public long getActTime() {
        return actTime;
    }

    public void setActTime(long actTime) {
        this.actTime = actTime;
    }

    /**
     * Funcion para que un hilo sepa si aun el PMC esta vivo
     *
     * @return      true si el PMC esta vivo
     */
    public boolean isAlive(){
        prevTime = actTime;
        actTime = System.currentTimeMillis();
        boolean canExecute = run.get();
        if(cancellable != null) {
            canExecute &= !cancellable.isCancelled();
        }
        return canExecute;
    }

    /**
     * Funcion para obtener el tiempo que ha pasado el hilo  corriendo
     *
     * @return      tiempo entre pasos por el marcador de tiempo
     */
    public long runningTime(){
        return System.currentTimeMillis() - actTime;
    }

    /**
     * Metodo de ejecucion de los HecticusThread
     */
    @Override
    public void run() {
        if(isAlive() && !active){
//            if(job != null) {
//                this.job.markAsRunning();
//            }
            try{
                active = true;
                process(params);
            } catch (Throwable t){
                Utils.printToLog(HecticusThread.class, "Error en el HecticusThread", "Ocurrio un error que llego hasta el HecticusThread: " + name, true, t, "support-level-1", Config.LOGGER_ERROR);
            } finally {
                setAlive();
                active = false;
                markAsFinished();
//                if(job != null) {
//                    this.job.markAsStopped();
//                }
            }
        }
    }

    public void stop() {
        try{
            if(job != null) {
                System.out.println("apagando el job " + job.getName());
                job.setInstance(null);
                job.rollTimestamp();
                job.update();
            }
        } catch (OptimisticLockException t){
            //ignore
        }
    }

    public void cancel() {
        try{
            active = false;
            stop();
            if(cancellable != null) {
                cancellable.cancel();
            }
            Utils.printToLog(HecticusThread.class, null, "Apagado " + name + " vivio " + (System.currentTimeMillis() - initTime), false, null, "support-level-1", Config.LOGGER_INFO);
        } catch (Throwable t){
            Utils.printToLog(HecticusThread.class, "Error en el HecticusThread", "Ocurrio cancelando el HecticusThread: " + name, true, t, "support-level-1", Config.LOGGER_ERROR);
        }
    }

    public int getIdApp() {
        return idApp;
    }

    public void setIdApp(int idApp) {
        this.idApp = idApp;
    }

    public void markAsFinished(){
        if(job != null && !job.isDaemon()) {
            job.deActivateJob();
            try {
                ServerInstance.getInstance().isInstanceSupervisor().removeJob(this);
            } catch (CouldNotCreateInstanceException ex){

            }
            if(cancellable != null) {
                cancellable.cancel();
            }
            Utils.printToLog(HecticusThread.class, null, "Apagado " + name + " vivio " + (System.currentTimeMillis() - initTime), false, null, "support-level-1", Config.LOGGER_INFO);
        }
    }

}
