package controllers.modules;

import play.mvc.Controller;
import models.Module;

import com.alvazan.play.NoSql;

import controllers.modules.util.ModuleStreamer;

public class SdiCore extends Controller {

	public static void remoteModule(String name, String path) {
		Module module = NoSql.em().find(Module.class, name);
		
		if(module == null)
			badRequest("Module name="+name+" does not exist");
		
		//Need to save authentiction info
		String username = request.user;
		String key = request.password;
		
		//remove this when done....
		badRequest("Module name="+name+" does not exist");
	}
}
