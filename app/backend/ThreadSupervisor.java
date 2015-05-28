package backend;

import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import com.fasterxml.jackson.databind.ObjectMapper;
import exceptions.CouldNotCreateInstanceException;
import models.Config;
import models.Instance;
import models.Job;
import org.apache.commons.lang3.StringEscapeUtils;
import play.libs.F;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import scala.concurrent.duration.Duration;
import utils.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by plesse on 10/6/14.
 */
public class ThreadSupervisor extends HecticusThread {

    private ActorSystem system = null;
    private ArrayList<HecticusThread> activeJobs = null;

    public ThreadSupervisor(String name, AtomicBoolean run, Cancellable cancellable, ActorSystem system) {
        super("ThreadSupervisor-"+name, run, cancellable);
        this.system = system;
        init();
    }

    public ThreadSupervisor(AtomicBoolean run, ActorSystem system) {
        super("ThreadSupervisor",run);
        this.system = system;
        init();
    }

    /**
     * Metodo para monitorear los tiempos de ejecucion de los hilos del kyubi
     */
    @Override
    public void process(Map args) {
        try {
            ServerInstance serverInstance = ServerInstance.getInstance();
            if(serverInstance.isInstanceMaster()){
                if(!serverInstance.isInstanceTest()) {
                    Utils.printToLog(ThreadSupervisor.class, "", "Esperando por otras instancias...", false, null, "support-level-1", Config.LOGGER_INFO);
                    try {
                        Thread.sleep(30000);
                    } catch (Exception e) {

                    }
                    Utils.printToLog(ThreadSupervisor.class, "", "Espera Terminada", false, null, "support-level-1", Config.LOGGER_INFO);
                }
                distributeJobs();
            } else {
                checkMaster(serverInstance);
            }
            Job.resetOrphanJobs();
            checkAliveThreads();
            //stop jobs
            stopActiveJobs();
            //start jobs
            activateJobs();
            //check for bad jobs
        } catch (Exception e){
            Utils.printToLog(ThreadSupervisor.class, "Error en el ThreadSupervisor", "Error desconocido procesando Jobs", true, e, "support-level-1", Config.LOGGER_ERROR);
        }
    }

    private void distributeJobs() {
        Job.resetJobsOnReassign();
        List<Job> unasignedDaemons = Job.getUnasignedDaemons();
        List<Job> unasignedScheduled = Job.getUnasignedScheduled();
        List<Instance> runningInstances = Instance.getRunningInstances();
        runningInstances = validateRunningInstances(runningInstances);
        System.out.println(unasignedDaemons.size() + " " + unasignedScheduled.size() + " " + runningInstances.size());
        if((!unasignedDaemons.isEmpty() || !unasignedScheduled.isEmpty()) && !runningInstances.isEmpty()) {
            int daemonMax = (int) Math.ceil(unasignedDaemons.size() / (double) runningInstances.size());
            int daemonMin = (int) Math.floor(unasignedDaemons.size() / (double) runningInstances.size());
            int scheduledMax = (int) Math.ceil(unasignedScheduled.size() / (double) runningInstances.size());
            int scheduledMin = (int) Math.floor(unasignedScheduled.size() / (double) runningInstances.size());
            Utils.printToLog(ThreadSupervisor.class, "", "Distribuyendo jobs... instancias = " + runningInstances.size() + " daemonMax = " + daemonMax + " daemonMin = " + daemonMin + " scheduledMax = " + scheduledMax + " scheduledMin = " + scheduledMin, false, null, "support-level-1", Config.LOGGER_INFO);
            int daemonJobs = 0;
            int scheduledJobs = 0;
            int daemonIndex = 0;
            int scheduledIndex = 0;
            int mark = 0;
            Job actual = null;
            for (Instance instance : runningInstances) {
                if (instance.isMaster()) {
                    daemonJobs = daemonMax;
                    scheduledJobs = scheduledMax;
                } else {
                    daemonJobs = daemonMin;
                    scheduledJobs = scheduledMin;
                }
                mark = daemonIndex + daemonJobs;
                for (; daemonIndex < mark; ++daemonIndex) {
                    actual = unasignedDaemons.get(daemonIndex);
                    actual.setInstance(instance);
                    actual.update();
                }
                mark = scheduledIndex + scheduledJobs;
                for (; scheduledIndex < mark; ++scheduledIndex) {
                    actual = unasignedScheduled.get(scheduledIndex);
                    actual.setInstance(instance);
                    actual.update();
                }
            }
        }
    }

    private List<Instance> validateRunningInstances(List<Instance> runningInstances) {
        ArrayList<Instance> realRunning = new ArrayList<>();
        F.Promise<WSResponse> result = null;
        WSResponse wsResponse = null;
        int wsStatus = 0;
        StringBuilder fallenInstances = new StringBuilder();
        Utils.printToLog(ThreadSupervisor.class, "", "Checkeando Instancias...", false, null, "support-level-1", Config.LOGGER_INFO);
        for(Instance instance : runningInstances){
            try {
                result = WS.url("http://" + instance.getIp() + "/alive").get();
                wsResponse = result.get(Config.getLong("ws-timeout-millis"), TimeUnit.MILLISECONDS);
                wsStatus = wsResponse.getStatus();
                if (wsStatus != 200) {
                    Utils.printToLog(ThreadSupervisor.class, "", instance.getName() + " muerta", false, null, "support-level-1", Config.LOGGER_INFO);
                    fallenInstances.append("\t - ").append(instance.getIp());
                    instance.setRunning(false);
                    instance.update();
                } else {
                    Utils.printToLog(ThreadSupervisor.class, "", instance.getName() + " vive", false, null, "support-level-1", Config.LOGGER_INFO);
                    realRunning.add(instance);
                }
            } catch (Exception e){
                Utils.printToLog(ThreadSupervisor.class, "", instance.getName() + " muerta", false, null, "support-level-1", Config.LOGGER_INFO);
                fallenInstances.append("\t - ").append(instance.getIp());
                instance.setRunning(false);
                instance.update();
            }
        }
//        List<Instance> downInstances = Instance.getDownInstances();
//        for(Instance instance : downInstances){
//            fallenInstances.append("\t - ").append(instance.getIp());
//        }
//        downInstances.clear();
        runningInstances.clear();
        if(!fallenInstances.toString().isEmpty()){
            Utils.printToLog(ThreadSupervisor.class, "Instancias Apagadas", "Las instancias siguientes instancias estan apagadas: \n " + fallenInstances.toString(), true, null, "support-level-1", Config.LOGGER_ERROR);
            fallenInstances.delete(0, fallenInstances.length());
        }
        return realRunning;
    }

    private void checkMaster(ServerInstance serverInstance) {
        Utils.printToLog(ThreadSupervisor.class, "", "Buscando al master" , false, null, "support-level-1", Config.LOGGER_INFO);
        Instance master = Instance.getMaster();
        if(master == null) {
            serverInstance.takeMaster();
            Utils.printToLog(ThreadSupervisor.class, "Instancia master apagada", "No hay master, se reasignara el rol de master" , true, null, "support-level-1", Config.LOGGER_ERROR);
        } else {
            boolean takeMaster = false;
            try {
                F.Promise<WSResponse> result = WS.url("http://" + master.getIp() + "/alive").get();
                WSResponse wsResponse = result.get(Config.getLong("ws-timeout-millis"), TimeUnit.MILLISECONDS);
                int wsStatus = wsResponse.getStatus();
                if(wsStatus != 200){
                    takeMaster = true;
                }
            } catch (Exception e) {
                takeMaster = true;
            } finally {
                if(takeMaster){
                    serverInstance.takeMaster();
                    master.setMaster(false);
                    master.update();
                    Utils.printToLog(ThreadSupervisor.class, "Instancia master apagada", "La instancia " + master.getName() + " esta apagada, se reasignara el rol de master" , true, null, "support-level-1", Config.LOGGER_ERROR);
                }
            }
        }
    }

    private void checkSlaves(int idMaster) {
        F.Promise<WSResponse> result = null;
        WSResponse wsResponse = null;
        int wsStatus = 0;
        List<Instance> slaves = Instance.getSlaves(idMaster);
        StringBuilder fallenInstances = new StringBuilder();
        for(Instance slave : slaves){
            try {
                result = WS.url("http://" + slave.getIp() + "/alive").get();
                wsResponse = result.get(Config.getLong("ws-timeout-millis"), TimeUnit.MILLISECONDS);
                wsStatus = wsResponse.getStatus();
                if (wsStatus != 200) {
                    fallenInstances.append(slave.getIp()).append(", ");
                }
            } catch (Exception e){
                fallenInstances.append(slave.getIp()).append(", ");
            }
        }
        if(!fallenInstances.toString().isEmpty()){
            Utils.printToLog(ThreadSupervisor.class, "Instancias Apagadas", "Las instancias " + fallenInstances.toString() + " estan apagadas", true, null, "support-level-1", Config.LOGGER_ERROR);
            fallenInstances.delete(0, fallenInstances.length());
        }
    }

    private boolean checkInstance(Instance instance) {
        try {
            F.Promise<WSResponse> result = WS.url("http://" + instance.getIp() + "/alive").get();
            WSResponse wsResponse = result.get(Config.getLong("ws-timeout-millis"), TimeUnit.MILLISECONDS);
            int wsStatus = wsResponse.getStatus();
            return wsStatus != 200;
        } catch (Exception e){
            return false;
        }
    }

    private void init(){
        //start things and
        activeJobs = new ArrayList<>();
    }

    @Override
    public void stop() {
        if(activeJobs != null && !activeJobs.isEmpty()){
            for(HecticusThread ht : activeJobs){
                ht.cancel();
            }
            Utils.printToLog(ThreadSupervisor.class, null, "Apagados " + activeJobs.size() + " EventManagers", false, null, "support-level-1", Config.LOGGER_INFO);
            activeJobs.clear();
        }
        Job.surrenderAllJobs();
    }

    private void checkAliveThreads() {
        long allowedTime = Config.getLong("jobs-keep-alive-allowed");
        for(HecticusThread ht : activeJobs){
            long threadTime = ht.runningTime();
            if(isAlive() && ht.isActive() && threadTime > allowedTime){
                Utils.printToLog(ThreadSupervisor.class, "Job Bloqueado", "El job " + ht.getName() + " lleva " + threadTime + " sin pasar por un setAlive()", true, null, "support-level-1", Config.LOGGER_ERROR);
            }
        }
    }

    private void activateJobs() {
        try {
            ServerInstance serverInstance = ServerInstance.getInstance();
            List<Job> currentList = Job.getToActivateJobs(serverInstance.getRealInstance());
            long jobDelay = Config.getLong("job-delay");
            if (currentList != null){
                for (int i = 0 ; i < currentList.size(); i++){
                    Job actual = currentList.get(i);
                    try {
                        //getting class name
                        Class jobClassName = Class.forName(actual.getClassName().trim());
                        final HecticusThread j = (HecticusThread) jobClassName.newInstance();
                        //update to running
                        actual.activateJob();
                        //parse params
                        LinkedHashMap jobParams = null;
                        if (actual.getParams() != null && !actual.getParams().isEmpty()) {
                            String tempParams = StringEscapeUtils.unescapeHtml4(actual.getParams());
                            ObjectMapper mapper = new ObjectMapper();
                            jobParams = mapper.readValue(tempParams, LinkedHashMap.class);
                        }
                        j.setName(actual.getName() + "-" + System.currentTimeMillis());
                        j.setIdApp(actual.getIdApp());
                        j.setParams(jobParams);
                        j.setJob(actual);
                        Cancellable cancellable = null;
                        if(actual.isDaemon()){
                            cancellable = system.scheduler().schedule(Duration.create(jobDelay, SECONDS), Duration.create(Long.parseLong(actual.getTimeParams()), SECONDS), j, system.dispatcher());
                        } else {
                            cancellable = system.scheduler().scheduleOnce(Duration.create(jobDelay, SECONDS), j, system.dispatcher());
                        }
                        j.setCancellable(cancellable);
                        activeJobs.add(j);
                    }catch (Exception ex){
                        actual.failedJob();
                        Utils.printToLog(ThreadSupervisor.class,
                                "Error en el ThreadSupervisor",
                                "ocurrio un error activando el job:" + actual.getName() + " id:" + actual.getId() + " el job sera desactivado.",
                                true,
                                ex,
                                "support-level-1",
                                Config.LOGGER_ERROR);
                    }
                }
            }
        }catch (Exception ex){
            Utils.printToLog(ThreadSupervisor.class,
                    "Error en el ThreadSupervisor",
                    "error desconocido en el activate jobs",
                    true,
                    ex,
                    "support-level-1",
                    Config.LOGGER_ERROR);
        }
    }


    private void stopActiveJobs(){
        if(!activeJobs.isEmpty()) {
            try {
                ServerInstance serverInstance = ServerInstance.getInstance();
                List<Job> currentList = Job.getToStopJobs(serverInstance.getRealInstance());
                if (currentList != null) {
                    for (int i = 0; i < currentList.size(); i++) {
                        try {
                            Job actual = currentList.get(i);
                            for (HecticusThread ht : activeJobs) {
                                if (ht.getJob().getId() == actual.getId()) {
                                    ht.cancel();
                                    activeJobs.remove(ht);
                                    break;
                                }
                            }
                        } catch (Exception ex) {
                            Utils.printToLog(ThreadSupervisor.class,
                                    "Error en el ThreadSupervisor",
                                    "error desconocido en el apagando jobs",
                                    true,
                                    ex,
                                    "support-level-1",
                                    Config.LOGGER_ERROR);
                        }
                    }
                }

            } catch (Exception ex) {
                Utils.printToLog(ThreadSupervisor.class,
                        "Error en el ThreadSupervisor",
                        "error desconocido en el apagando jobs",
                        true,
                        ex,
                        "support-level-1",
                        Config.LOGGER_ERROR);
            }
        }
    }

    public synchronized void removeJob(HecticusThread job){
        activeJobs.remove(job);
    }


}
