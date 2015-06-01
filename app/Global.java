import backend.ServerInstance;
import exceptions.CouldNotCreateInstanceException;
import models.Config;
import play.Application;
import play.GlobalSettings;
import utils.JobCoreUtils;

public class Global extends GlobalSettings {


	public void onStart(Application app) {
        super.onStart(app);
        try{
            ServerInstance.getInstance();
        } catch (CouldNotCreateInstanceException ex){
            JobCoreUtils.printToLog(Global.class, "ERROR CRITICO Apagando " + Config.getString("app-name"), "No se pudo crear la instancia", true, ex, "support-level-1", Config.LOGGER_ERROR);
            super.onStop(app);
        }
	}

    @Override
    public void onStop(Application application) {
        try {
            ServerInstance.getInstance().shutdown();
        } catch (Exception ex) {

        }
        super.onStop(application);
    }


}