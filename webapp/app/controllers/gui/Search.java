package controllers.gui;

import gov.nrel.util.Utility;

import java.util.List;

import models.EntityUser;
import models.UserChart;
import play.Play;
import play.mvc.Controller;
import play.mvc.With;
import controllers.gui.auth.GuiSecure;

@With(GuiSecure.class)
public class Search extends Controller{

	//NOTE: These are definitely not in the right place...
	public static void myHelpBootstrapProto_Summary() {
		EntityUser user = Utility.getCurrentUser(session);
		render(user);
	}
	
}
