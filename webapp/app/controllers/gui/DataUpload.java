package controllers.gui;

import gov.nrel.util.Utility;
import models.EntityUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.gui.auth.GuiSecure;

import play.mvc.Controller;
import play.mvc.With;

@With(GuiSecure.class)
public class DataUpload extends Controller {
	private static final Logger log = LoggerFactory.getLogger(DataUpload.class);

	public static void dataUploadMain() {
		EntityUser user = Utility.getCurrentUser(session);

		render(user);
	}

}
