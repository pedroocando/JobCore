package models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import backend.ThreadSupervisor;
import play.Play;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.libs.Json;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by plesse on 8/8/14.
 */
@Entity
@Table(name="instances")
public class Instance extends HecticusModel {

    @Id
    private Long idInstance;
    @Constraints.Required
    private String ip;
    @Constraints.Required
    private String name;
    @Constraints.Required
    private boolean running;
    @Constraints.Required
    private boolean test;
    @Constraints.Required
    private boolean master;

    @Transient
    private ThreadSupervisor supervisor;
    @Transient
    private AtomicBoolean run;

    private static Model.Finder<Integer, Instance> finder = new Model.Finder<Integer, Instance>(Integer.class, Instance.class);

    public Instance(String ip, String name, boolean running) {
        this.ip = ip;
        this.name = name;
        this.running = running;
    }

    public Instance(String ip, String name, boolean running, boolean test) {
        this.ip = ip;
        this.name = name;
        this.running = running;
        this.test = test;
    }

    public Long getIdInstance() {
        return idInstance;
    }

    public void setIdInstance(long idInstance) {
        this.idInstance = idInstance;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public void setIdInstance(Long idInstance) {
        this.idInstance = idInstance;
    }

    public boolean isRunning() {
        return running;
    }

    public ThreadSupervisor getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(ThreadSupervisor supervisor) {
        this.supervisor = supervisor;
    }

    public AtomicBoolean getRun() {
        return run;
    }

    public void setRun(AtomicBoolean run) {
        this.run = run;
    }

    public void setRunValue(boolean run) {
        this.run.set(run);
    }

    @Override
    public ObjectNode toJson() {
        ObjectNode response = Json.newObject();
        response.put("idInstance", idInstance);
        response.put("ip", ip);
        response.put("name", name);
        response.put("running", running);
        response.put("test", test);
        response.put("master", master);
        return response;
    }

    public static Instance getInstance(String serverIp){
        return finder.where().eq("ip", serverIp).findUnique();
    }

    public static Instance getMaster(){
        return finder.setForUpdate(true).where().eq("master", true).eq("test", !Play.isProd()).findUnique();
    }

    public static List<Instance> getSlaves(int idMaster){
        return finder.where().ne("idInstance", idMaster).eq("test", false).findList();
    }

    public static List<Instance> getRunningInstances(){
        return finder.where().eq("running", true).eq("test", !Play.isProd()).findList();
    }

    public static List<Instance> getDownInstances(){
        return finder.where().eq("running", false).eq("test", false).findList();
    }

}
