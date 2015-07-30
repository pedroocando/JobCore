package controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class Application extends Controller {

    public static final String USER_ROLE = "user";
    public static final String ADMIN_ROLE = "admin";

    public static Result alive(){
        return ok("alive");
    }

}
