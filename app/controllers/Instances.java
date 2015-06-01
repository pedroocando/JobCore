package controllers;

import play.mvc.Result;

/**
 * Created by plessmann on 01/06/15.
 */
public class Instances extends HecticusController {

    public static Result alive(){
        return ok("alive");
    }

}
