package backend;

import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.CouldNotCreateInstanceException;
import models.Config;
import models.Instance;
import models.Job;
import play.Play;
import play.api.libs.json.Json;
import play.libs.F;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import scala.concurrent.duration.Duration;
import utils.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by plessmann on 21/05/15.
 */
public class ServerInstance {

    private static ServerInstance me;
    private Instance instance;
    public static String instanceName = "ServerInstance";

    public ServerInstance() throws CouldNotCreateInstanceException {
        BufferedReader br = null;
        String serverIp = null;
        String serverName = null;
        String serverData = null;
        try {
            br = new BufferedReader(new FileReader(Config.getString("server-ip-file")));
            serverData = br.readLine();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode server = mapper.readTree(serverData);
            serverIp = server.get("ip").asText();
            serverName = server.get("name").asText();
            instance = Instance.getInstance(serverIp);
            if(instance != null) {
                instance.setTest(!Play.isProd());
                instance.setRunning(true);
                instance.setName(serverName);
                instance.update();
            } else {
                instance = new Instance(serverIp, serverName, true, !Play.isProd());
                instance.save();
            }
        } catch (Exception ex) {
            instance = null;
            Utils.printToLog(ServerInstance.class, "Error cargando el IP del servidor", "Ocurrio un error cargando el IP del servidor desde el archivo.", true, ex, "support-level-1", Config.LOGGER_ERROR);
        } finally {
            try {if (br != null)br.close();} catch (Exception ex) {}
        }
        if(instance == null) {
            throw new CouldNotCreateInstanceException("No se pudo crear la instancia");
        } else {
            instanceName = instance.getName();
            ActorSystem system = ActorSystem.create("application");
            instance.setRun(new AtomicBoolean(true));
            if(!instance.isMaster()) {
                boolean shouldTakeMaster = !isThereAMaster();
                //block here
                instance.setMaster(shouldTakeMaster);
                instance.update();
            }
            if(instance.isMaster()){
                Job.resetJobsOnStart();
            }
            int supervisorSleepTime = Config.getInt("supervisor-sleep-time");
            Utils.printToLog(ServerInstance.class, null, "Arrancando ThreadSupervisor", false, null, "support-level-1", Config.LOGGER_INFO);
            ThreadSupervisor supervisor = new ThreadSupervisor(instance.getRun(), system);
            Cancellable cancellable = system.scheduler().schedule(Duration.create(1, SECONDS), Duration.create(supervisorSleepTime, MINUTES), supervisor, system.dispatcher());
            supervisor.setCancellable(cancellable);
            instance.setSupervisor(supervisor);
            Utils.printToLog(ServerInstance.class, null, "Arrancando " + instance.getName(), false, null, "support-level-1", Config.LOGGER_INFO);
        }
    }

    private boolean isThereAMaster() {
        Instance master = Instance.getMaster();
        if(master == null) {
            return false;
        } else {
            try {
                F.Promise<WSResponse> result = WS.url("http://" + master.getIp() + "/alive").get();
                WSResponse wsResponse = result.get(Config.getLong("ws-timeout-millis"), TimeUnit.MILLISECONDS);
                int wsStatus = wsResponse.getStatus();
                System.out.println(wsStatus);
                if (wsStatus == 200) {
                    return true;
                } else {
                    master.setMaster(false);
                    master.update();
                    return false;
                }
            } catch (Exception e){
                return false;
            }
       }
    }


    public static ServerInstance getInstance() throws CouldNotCreateInstanceException {
        if (me == null) {
            me = new ServerInstance();
        }
        return me;
    }

    public int getInstanceID() {
        return instance.getIdInstance();
    }

    public String getInstanceIP() {
        return instance.getIp();
    }

    public String getInstanceName() {
        return instance.getName();
    }

    public boolean isInstanceRunning() {
        return instance.getRunning();
    }

    public boolean isInstanceTest() {
        return instance.isTest();
    }

    public boolean isInstanceMaster() {
        return instance.isMaster();
    }

    public ThreadSupervisor isInstanceSupervisor() {
        return instance.getSupervisor();
    }

    public AtomicBoolean isInstanceRun() {
        return instance.getRun();
    }

    public Instance getRealInstance(){
        return instance;
    }

    public void shutdown(){
        instance.setRunValue(false);
        instance.getSupervisor().cancel();
        instance.setRunning(false);
        instance.setMaster(false);
        instance.update();
        Utils.printToLog(ServerInstance.class, "Apagando " + Config.getString("app-name"), "Apagando " + instance.getName() + ", se recibio la señal de shutdown", true, null, "support-level-1", Config.LOGGER_INFO);
    }

    public void takeMaster(){
        Job.shutdownAllJobs();
        instance.setMaster(true);
        instance.update();
    }

}
