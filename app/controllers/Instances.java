package controllers;

import play.mvc.Result;

import java.io.File;

/**
 * Created by plessmann on 01/06/15.
 */
public class Instances extends HecticusController {

    public static Result alive(){
        return ok("alive");
    }

    public static Result checkFile(String name){
        File file = new File(name);
        //Logger.info("nameFile "+name+", path "+file.getAbsolutePath());
        if(file.exists()){
            return ok("OK");
        }else{
            return badRequest("file not found");
        }
    }

}
