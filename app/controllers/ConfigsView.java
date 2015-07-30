package controllers;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import models.Config;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Result;
import views.html.configs.*;

import java.io.IOException;

import static play.data.Form.form;

/**
 * Created by plesse on 11/4/14.
 */
public class ConfigsView extends HecticusController {

    final static Form<Config> ConfigViewForm = form(Config.class);
    public static Result GO_HOME = redirect(routes.ConfigsView.list(0, "configKey", "asc", ""));

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result index() {
        return GO_HOME;
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result blank() {
        return ok(views.html.configs.form.render(ConfigViewForm));
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result list(int page, String sortBy, String order, String filter) {
        return ok(views.html.configs.list.render(Config.page(page, 25, sortBy, order, filter), sortBy, order, filter, false));
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result edit(Long id) {
        Config objBanner = Config.getByID(id);
        Form<Config> filledForm = ConfigViewForm.fill(Config.getByID(id));
        return ok(edit.render(id, filledForm));
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result update(Long id) {
        Form<Config> filledForm = ConfigViewForm.bindFromRequest();
        if(filledForm.hasErrors()) {
            System.out.println(filledForm.toString());
            return badRequest(edit.render(id, filledForm));
        }
        Config gfilledForm = filledForm.get();
        gfilledForm.update(id);
        flash("success", Messages.get("configs.java.updated", gfilledForm.getConfigKey()));
        return GO_HOME;

    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result sort(String ids) {
        String[] aids = ids.split(",");

        for (int i=0; i<aids.length; i++) {
            Config oPost = Config.getByID(Long.parseLong(aids[i]));
            //oWoman.setSort(i);
            oPost.save();
        }

        return ok("Fine!");
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result lsort() {
        return ok(views.html.configs.list.render(Config.page(0, 0, "configKey", "asc", ""), "date", "asc", "", true));
    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result delete(Long id) {
        Config config = Config.getByID(id);
        config.delete();
        flash("success", Messages.get("configs.java.deleted", config.getConfigKey()));
        return GO_HOME;

    }

    @Restrict(@Group(Application.ADMIN_ROLE))
    public static Result submit() throws IOException {
        Form<Config> filledForm = ConfigViewForm.bindFromRequest();

        if(filledForm.hasErrors()) {
            System.out.println(filledForm.toString());
            return badRequest(views.html.configs.form.render(filledForm));
        }

        Config gfilledForm = filledForm.get();
        gfilledForm.save();
        flash("success", Messages.get("configs.java.created", gfilledForm.getConfigKey()));
        return GO_HOME;

    }
}
