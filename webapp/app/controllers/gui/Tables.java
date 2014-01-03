package controllers.gui;

import models.SecureTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.With;
import controllers.SecurityUtil;
import controllers.gui.auth.GuiSecure;

@With(GuiSecure.class)
public class Tables extends Controller {

	private static final Logger log = LoggerFactory.getLogger(Tables.class);
	
	public static void uploadForm(String table) {
		SecureTable sdiTable = SecurityUtil.checkSingleTable(table);
		render(sdiTable, table);
	}

	public static void uploadSuccess(String table) {
		render();
	}

}
