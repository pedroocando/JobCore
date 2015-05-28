package controllers;

import play.*;
import play.mvc.*;

import views.html.*;

import java.io.File;

public class Application extends Controller {

    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }

    public static Result alive(){
        return ok("alive");
    }

}
