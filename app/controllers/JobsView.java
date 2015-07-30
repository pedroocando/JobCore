package controllers;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import models.Job;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Result;
import views.html.jobs.edit;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static play.data.Form.form;

/**
 * Created by plesse on 11/10/14.
 */
public class JobsView extends HecticusController {

    final static Form<Job> JobViewForm = form(Job.class);
    public static Result GO_HOME = redirect(routes.JobsView.list(0, "name", "asc", ""));

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result index() {
        return GO_HOME;
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result blank() {
        TimeZone tz = TimeZone.getDefault();
        GregorianCalendar cal = new GregorianCalendar(tz);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        JobViewForm.data().put("Date", sdf.format(cal.getTime()));
        return ok(views.html.jobs.form.render(JobViewForm));
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result list(int page, String sortBy, String order, String filter) {
        return ok(views.html.jobs.list.render(Job.page(page, 10, sortBy, order, filter), sortBy, order, filter, false));
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result edit(Long id) {
        Job job = Job.getByID(id);
        Form<Job> filledForm = JobViewForm.fill(job);
        TimeZone tz = TimeZone.getDefault();
        GregorianCalendar cal = new GregorianCalendar(tz);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String timeParams = job.getTimeParams();
        String time = job.getTime();
        if(!job.isDaemon()) {
            switch (job.getFrequency()) {
                case 0:
                    int year = Integer.parseInt(timeParams.substring(0, 4));
                    int month = Integer.parseInt(timeParams.substring(4, 6), 10) - 1;
                    int date = Integer.parseInt(timeParams.substring(6, 8), 10);
                    int hourOfDay = Integer.parseInt(time.substring(0, 2), 10);
                    int minute = Integer.parseInt(time.substring(2), 10);
                    cal.set(year, month, date, hourOfDay, minute);
                    filledForm.data().put("Date", sdf.format(cal.getTime()));
                    break;
                case 1:
                    cal.set(Calendar.MONTH, Integer.parseInt(timeParams.substring(0, 2), 10) - 1);
                    cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(timeParams.substring(2, 4), 10));
                    cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2), 10));
                    cal.set(Calendar.MINUTE, Integer.parseInt(time.substring(2), 10));
                    filledForm.data().put("Date", sdf.format(cal.getTime()));
                    break;
                case 2:
                    cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(timeParams.substring(0, 2), 10));
                    cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2), 10));
                    cal.set(Calendar.MINUTE, Integer.parseInt(time.substring(2), 10));
                    filledForm.data().put("Date", sdf.format(cal.getTime()));
                    break;
                case 3:
                    int flag = Integer.parseInt(timeParams);
                    for (int i = 0; i < 7; i++) {
                        int mask = 1 << i;
                        if ((flag & mask) != 0) {
                            switch (i) {
                                case 0:
                                    filledForm.data().put("sunday", "true");
                                    break;
                                case 1:
                                    filledForm.data().put("monday", "true");
                                    break;
                                case 2:
                                    filledForm.data().put("tuesday", "true");
                                    break;
                                case 3:
                                    filledForm.data().put("wednesday", "true");
                                    break;
                                case 4:
                                    filledForm.data().put("thursday", "true");
                                    break;
                                case 5:
                                    filledForm.data().put("friday", "true");
                                    break;
                                case 6:
                                    filledForm.data().put("saturday", "true");
                                    break;

                            }
                        }
                    }
                    cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2), 10));
                    cal.set(Calendar.MINUTE, Integer.parseInt(time.substring(2), 10));
                    filledForm.data().put("Date", sdf.format(cal.getTime()));
                    break;
                case 4:
                    cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2), 10));
                    cal.set(Calendar.MINUTE, Integer.parseInt(time.substring(2), 10));
                    filledForm.data().put("Date", sdf.format(cal.getTime()));
                    break;
            }
        } else {
            filledForm.data().put("daemonFrequency", timeParams);
        }
        return ok(edit.render(id, filledForm));
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result update(Long id) {
        Form<Job> jobForm = JobViewForm.bindFromRequest();
        System.out.println(jobForm.toString());
        if(jobForm.hasErrors()) {
            return badRequest(edit.render(id, jobForm));
        }
        Job job = jobForm.get();
        String date = jobForm.data().get("Date");//Date=01/01/2014 06:00
        if(!job.isDaemon()) {
            switch (job.getFrequency()) {
                case 0://once
                    job.setTimeParams(date.substring(6, 10) + date.substring(3, 5) + date.substring(0, 2));
                    job.setTime(date.substring(11).replace(":", ""));
                    break;
                case 1://year
                    job.setTimeParams(date.substring(3, 5) + date.substring(0, 2));
                    job.setTime(date.substring(11).replace(":", ""));
                    break;
                case 2://month
                    job.setTimeParams(date.substring(0, 2));
                    job.setTime(date.substring(11).replace(":", ""));
                    break;
                case 3://week
                    int time = 0;
                    if (jobForm.data().containsKey("sunday")) {
                        time += Math.pow(2, 0);
                    }

                    if (jobForm.data().containsKey("monday")) {
                        time += Math.pow(2, 1);
                    }

                    if (jobForm.data().containsKey("tuesday")) {
                        time += Math.pow(2, 2);
                    }

                    if (jobForm.data().containsKey("wednesday")) {
                        time += Math.pow(2, 3);
                    }

                    if (jobForm.data().containsKey("thursday")) {
                        time += Math.pow(2, 4);
                    }

                    if (jobForm.data().containsKey("friday")) {
                        time += Math.pow(2, 5);
                    }

                    if (jobForm.data().containsKey("saturday")) {
                        time += Math.pow(2, 6);
                    }
                    job.setTimeParams("" + time);
                    job.setTime(date.substring(11).replace(":", ""));
                    break;
                case 4://day
                    job.setTimeParams("");
                    job.setTime(date.substring(11).replace(":", ""));
                    break;
            }
            job.rollTimestamp();
        } else {
            job.setTimeParams(jobForm.data().get("daemonFrequency"));
            if(job.getNextTimestamp() == null) {
                job.setNextTimestamp(0L);
            }
        }
        job.update(id);
        flash("success", Messages.get("jobs.java.updated", job.getName()));
        return GO_HOME;

    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result sort(String ids) {
        String[] aids = ids.split(",");

        for (int i=0; i<aids.length; i++) {
            Job oPost = Job.getByID(Long.parseLong(aids[i]));
            //oWoman.setSort(i);
            oPost.save();
        }

        return ok("Fine!");
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result lsort() {
        return ok(views.html.jobs.list.render(Job.page(0, 0, "configKey", "asc", ""), "date", "asc", "", true));
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result delete(Long id) {
        Job job = Job.getByID(id);
        job.delete();
        flash("success", Messages.get("jobs.java.deleted", job.getName()));
        return GO_HOME;

    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result submit() throws IOException {
        Form<Job> jobForm = JobViewForm.bindFromRequest();

        if(jobForm.hasErrors()) {
            System.out.println(jobForm.toString());
            return badRequest(views.html.jobs.form.render(jobForm));
        }

        Job job = jobForm.get();
        String date = jobForm.data().get("Date");//Date=01/01/2014 06:00
        if(!job.isDaemon()) {
            switch (job.getFrequency()) {
                case 0://once
                    job.setTimeParams(date.substring(6, 10) + date.substring(3, 5) + date.substring(0, 2));
                    job.setTime(date.substring(11).replace(":", ""));
                    break;
                case 1://year
                    job.setTimeParams(date.substring(3, 5) + date.substring(0, 2));
                    job.setTime(date.substring(11).replace(":", ""));
                    break;
                case 2://month
                    job.setTimeParams(date.substring(0, 2));
                    job.setTime(date.substring(11).replace(":", ""));
                    break;
                case 3://week
                    int time = 0;
                    if (jobForm.data().containsKey("sunday")) {
                        time += Math.pow(2, 0);
                    }

                    if (jobForm.data().containsKey("monday")) {
                        time += Math.pow(2, 1);
                    }

                    if (jobForm.data().containsKey("tuesday")) {
                        time += Math.pow(2, 2);
                    }

                    if (jobForm.data().containsKey("wednesday")) {
                        time += Math.pow(2, 3);
                    }

                    if (jobForm.data().containsKey("thursday")) {
                        time += Math.pow(2, 4);
                    }

                    if (jobForm.data().containsKey("friday")) {
                        time += Math.pow(2, 5);
                    }

                    if (jobForm.data().containsKey("saturday")) {
                        time += Math.pow(2, 6);
                    }
                    job.setTimeParams("" + time);
                    job.setTime(date.substring(11).replace(":", ""));
                    break;
                case 4://day
                    job.setTimeParams("");
                    job.setTime(date.substring(11).replace(":", ""));
                    break;
            }
            job.rollTimestamp();
        } else {
            job.setTimeParams(jobForm.data().get("daemonFrequency"));
            job.setNextTimestamp(0L);
        }
        job.save();
        flash("success", Messages.get("jobs.java.created", job.getName()));
        return GO_HOME;

    }
}
