package controllers;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import models.Instance;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Result;
import views.html.instances.edit;

import java.io.IOException;

import static play.data.Form.form;

/**
 * Created by plesse on 11/4/14.
 */
public class InstancesView extends HecticusController {

    final static Form<Instance> InstanceViewForm = form(Instance.class);
    public static Result GO_HOME = redirect(routes.InstancesView.list(0, "name", "asc", ""));

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result index() {
        return GO_HOME;
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result blank() {
        return ok(views.html.instances.form.render(InstanceViewForm));
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result list(int page, String sortBy, String order, String filter) {
        return ok(views.html.instances.list.render(Instance.page(page, 10, sortBy, order, filter), sortBy, order, filter, false));
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result edit(Integer id) {
        Instance objBanner = Instance.getByID(id);
        Form<Instance> filledForm = InstanceViewForm.fill(objBanner);
        return ok(edit.render(id, filledForm));
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result update(Integer id) {
        Form<Instance> filledForm = InstanceViewForm.bindFromRequest();
        if(filledForm.hasErrors()) {
            System.out.println(filledForm.toString());
            return badRequest(edit.render(id, filledForm));
        }
        Instance gfilledForm = filledForm.get();
        gfilledForm.update(id);
        flash("success", Messages.get("instances.java.updated", gfilledForm.getName()));
        return GO_HOME;

    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result sort(String ids) {
        String[] aids = ids.split(",");

        for (int i=0; i<aids.length; i++) {
            Instance oPost = Instance.getByID(Integer.parseInt(aids[i]));
            //oWoman.setSort(i);
            oPost.save();
        }

        return ok("Fine!");
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result lsort() {
        return ok(views.html.instances.list.render(Instance.page(0, 0, "configKey", "asc", ""), "date", "asc", "", true));
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result delete(Integer id) {
        Instance instance = Instance.getByID(id);
        instance.delete();
        flash("success", Messages.get("instances.java.deleted", instance.getName()));
        return GO_HOME;

    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result submit() throws IOException {
        Form<Instance> filledForm = InstanceViewForm.bindFromRequest();

        if(filledForm.hasErrors()) {
            System.out.println(filledForm.toString());
            return badRequest(views.html.instances.form.render(filledForm));
        }

        Instance gfilledForm = filledForm.get();
        gfilledForm.save();
        flash("success", Messages.get("instances.java.created", gfilledForm.getName()));
        return GO_HOME;

    }
}
