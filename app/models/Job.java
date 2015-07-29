package models;

import backend.ServerInstance;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.Page;
import com.avaje.ebean.SqlUpdate;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

import javax.persistence.*;
import java.util.*;

/**
 * Created by sorcerer on 9/25/14.
 */

@Entity
@Table(name= "jobs")
public class Job extends HecticusModel {

    @Id
    private Long id;
    private int status;
    private String className;
    private String name;
    private String params;
    private Long nextTimestamp;
    private String time;
    private String timeParams;
    private Integer frequency;
    private boolean daemon;
    private boolean multiInstance;
    private Integer quantity;

    @OneToOne
    @JoinColumn(name = "id_instance")
    private Instance instance;


    private static Finder<Long,Job> finder = new Finder<Long, Job>(Long.class, Job.class);

    @Override
    public ObjectNode toJson() {
        ObjectNode tr = Json.newObject();
        tr.put("id",id);
        tr.put("status", status);
        tr.put("className", className);
        tr.put("name",name);
        return tr;
    }

    /******************bd funtions ******************************************************************************/

    //get active

    public static List<Job> getToActivateJobs(Instance instance){
        return finder.where().eq("status","1").lt("nextTimestamp", System.currentTimeMillis()).eq("instance", instance).eq("multiInstance", false).orderBy("id asc").setMaxRows(1000).findList();
    }

    public static List<Job> getToStopJobs(Instance instance){
        return finder.where().or(Expr.eq("status","0"), Expr.eq("status","3")).eq("instance", instance).eq("multiInstance", false).orderBy("id asc").setMaxRows(1000).findList();
    }

    public static List<Job> getRunningJobs(Instance instance){
        return finder.where().eq("status","2").eq("instance", instance).eq("multiInstance", false).orderBy("id asc").setMaxRows(1000).findList();
    }

    public static List<Job> getBadJobs(Instance instance){ //is this one???
        return finder.where().eq("status","-1").eq("instance", instance).orderBy("id asc").setMaxRows(1000).findList();
    }

    public static List<Job> getUnasignedDaemons(){
        return finder.where().eq("status","1").eq("instance", null).eq("daemon", true).eq("multiInstance", false).orderBy("id asc").findList();
    }

    public static List<Job> getUnasignedScheduled(){
        return finder.where().eq("status","1").eq("instance", null).eq("daemon", false).eq("multiInstance", false).orderBy("id asc").findList();
    }

    public static List<Job> getMultiInstanceJobs(){
        return finder.where().eq("status","1").lt("nextTimestamp", System.currentTimeMillis()).eq("multiInstance", true).orderBy("id asc").setMaxRows(1000).findList();
    }

    public void activateJob(){
        this.refresh();
        this.setStatus(2);
        this.update();
    }

    public void deActivateJob(){
        this.refresh();
        this.setStatus(1);
        rollTimestamp();
        this.setInstance(null);
        this.update();
    }

    public void failedJob(){
        this.setStatus(-1);
        this.update();
    }

    public static void resetJobsOnStart(){
        try {
            String sql = "update jobs set `status` = 1, id_instance = null where `status` = 2 or `status` = 3";
            SqlUpdate update = Ebean.createSqlUpdate(sql);
            int modifiedCount = Ebean.execute(update);
        } catch (Exception ex) {
            //rollback
            ex.printStackTrace();
        } finally {

        }
    }

    public static void resetJobsOnReassign(){
        try {
            String sql = "update jobs set `status` = 1 where `status` = 3";
            SqlUpdate update = Ebean.createSqlUpdate(sql);
            int modifiedCount = Ebean.execute(update);
        } catch (Exception ex) {
            //rollback
            ex.printStackTrace();
        } finally {

        }
    }

    public static void shutdownAllJobs(){
        try {
            String sql = "update jobs set `status` = 3 where `status` = 2";
            SqlUpdate update = Ebean.createSqlUpdate(sql);
            int modifiedCount = Ebean.execute(update);
        } catch (Exception ex) {
            //rollback
            ex.printStackTrace();
        } finally {

        }
    }

    public static void surrenderAllJobs() {
        try{
            long instanceID = ServerInstance.getInstance().getInstanceID();
            String sql = "update jobs set `status` = 1, `id_instance` = null where `id_instance` = " + instanceID;
            SqlUpdate update = Ebean.createSqlUpdate(sql);
            int modifiedCount = Ebean.execute(update);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void resetOrphanJobs() {
        try{
            String sql = "update jobs set `status` = 1 where `id_instance` is null and `status` = 2";
            SqlUpdate update = Ebean.createSqlUpdate(sql);
            int modifiedCount = Ebean.execute(update);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Funcion que calcula un nuevo timestamp a partir del timestamp anterior,
     * la frecuencia en la que se ejecutara el job, la fecha y hora de ejecucion,
     * y el timestamp actual.
     *
     * <p>
     *
     * Valores para freq y params:<br>
     *	- freq = 0 -> unico -> params = yyyymmdd <br>
     *	- freq = 1 -> anual -> params = mmdd<br>
     *	- freq = 2 -> mensual -> params = dd<br>
     *	- freq = 3 -> semanal -> params = int-flags<br>
     *	- freq = 4 -> diario -> params = null/Don't Care<br>
     *	- freq = 5 -> X dias -> params = <p>
     *
     *
     */
    public void rollTimestamp() {
        if(daemon){
            nextTimestamp = System.currentTimeMillis();
        } else if (frequency == 0) {
            rollSingle();
        } else if (frequency == 1) {
            rollYearly();
        } else if (frequency == 2) {
            rollMonthly();
        } else if (frequency == 3) {
            rollWeekly();
        } else if (frequency == 4) {
            rollDaily();
        } else if (frequency == 5) {
            rollXDays();
        }
    }

    private void rollSingle() {
        TimeZone tz = TimeZone.getDefault();
        GregorianCalendar cal = new GregorianCalendar(tz);
        int year = Integer.parseInt(timeParams.substring(0, 4));
        int month = Integer.parseInt(timeParams.substring(4, 6), 10) - 1;
        int date = Integer.parseInt(timeParams.substring(6, 8), 10);
        int hourOfDay = Integer.parseInt(time.substring(0, 2), 10);
        int minute = Integer.parseInt(time.substring(2), 10);
        cal.set(year, month, date, hourOfDay, minute);
        this.nextTimestamp = cal.getTimeInMillis();
    }

    private void rollYearly() {
        long ts = System.currentTimeMillis();
        TimeZone tz = TimeZone.getDefault();
        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.setTimeInMillis(ts);
        cal.add(Calendar.YEAR, 1);

        cal.set(Calendar.MONTH, Integer.parseInt(timeParams.substring(0, 2), 10) - 1);
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(timeParams.substring(2, 4), 10));
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2), 10));
        cal.set(Calendar.MINUTE, Integer.parseInt(time.substring(2), 10));

        this.nextTimestamp = cal.getTimeInMillis();
    }

    private void rollMonthly() {
        long ts = System.currentTimeMillis();
        TimeZone tz = TimeZone.getDefault();
        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.setTimeInMillis(ts);
        cal.add(Calendar.MONTH, 1);

        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(timeParams.substring(0, 2), 10));
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2), 10));
        cal.set(Calendar.MINUTE, Integer.parseInt(time.substring(2), 10));

        this.nextTimestamp = cal.getTimeInMillis();
    }

    private void rollWeekly() {
        long ts = System.currentTimeMillis();
        TimeZone tz = TimeZone.getDefault();
        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.setTimeInMillis(ts);

        int flag = Integer.parseInt(timeParams);
        int currentDay = cal.get(Calendar.DAY_OF_WEEK) % 7;
        int count = 1;

        for (int i = 0; i < 7; i++) {
            int mask = 1 << currentDay;
            if ((flag & mask) != 0)
                break;

            count++;
            currentDay = (currentDay + 1) % 7;
        }

        cal.add(Calendar.DAY_OF_WEEK, count);
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2), 10));
        cal.set(Calendar.MINUTE, Integer.parseInt(time.substring(2), 10));

        this.nextTimestamp = cal.getTimeInMillis();
    }

    private void rollDaily() {
        long ts = System.currentTimeMillis();
        TimeZone tz = TimeZone.getDefault();

        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.setTimeInMillis(ts);

        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2), 10));
        cal.set(Calendar.MINUTE, Integer.parseInt(time.substring(2), 10));

        this.nextTimestamp = cal.getTimeInMillis();
    }

    private void rollXDays() {
        long ts = System.currentTimeMillis();
        TimeZone tz = TimeZone.getDefault();

        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.setTimeInMillis(ts);

        StringTokenizer strTok = new StringTokenizer(params, ",");
        String initialDate = strTok.nextToken();
        String jumpDays = strTok.nextToken();
        int daysToJump = Integer.parseInt(jumpDays);

        cal.add(Calendar.DAY_OF_MONTH, 1+daysToJump);
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2), 10));
        cal.set(Calendar.MINUTE, Integer.parseInt(time.substring(2), 10));

        this.nextTimestamp = cal.getTimeInMillis();
    }


    /********************getters and setters *********************************************************/

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Long getNextTimestamp() {
        return nextTimestamp;
    }

    public void setNextTimestamp(Long nextTimestamp) {
        this.nextTimestamp = nextTimestamp;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTimeParams() {
        return timeParams;
    }

    public void setTimeParams(String timeParams) {
        this.timeParams = timeParams;
    }

    public Integer getFrequency() {
        return frequency;
    }

    public void setFrequency(Integer frequency) {
        this.frequency = frequency;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public boolean isMultiInstance() {
        return multiInstance;
    }

    public void setMultiInstance(boolean multiInstance) {
        this.multiInstance = multiInstance;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void markAsRunning(){
        this.refresh();
//        this.running = true;
        this.update();
    }

    public void markAsStopped(){
        this.refresh();
//        this.running = false;
        this.update();
    }

    public static Page<Job> page(int page, int pageSize, String sortBy, String order, String filter) {
        return finder.where().ilike("name", "%" + filter + "%").orderBy(sortBy + " " + order).findPagingList(pageSize).getPage(page);
    }

    public static Job getByID(long id){
        return finder.byId(id);
    }


}
