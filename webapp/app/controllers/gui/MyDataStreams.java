package controllers.gui;

import java.util.List;

import models.message.StreamEditor;
import models.message.StreamModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.With;
import controllers.gui.auth.GuiSecure;
import controllers.modules2.framework.ModuleController;
import controllers.modules2.framework.RawProcessorFactory;

@With(GuiSecure.class)
public class MyDataStreams extends Controller {

	private static final Logger log = LoggerFactory.getLogger(MyDataStreams.class);

	public static void editAggregation(String encoded) {
		if("start".equals(encoded)) {
			render(null, null, encoded);
		}
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule stream = tuple.getStream();
		String path = tuple.getPath();
		render(stream, path, encoded);
	}

	public static void postAggregation(String encoded, String name) {
		StreamModule stream = new StreamModule();
		stream.setName(name);
		StreamEditor editor;
		if("start".equals(encoded)) {
			editor = new StreamEditor();
			editor.setStream(stream);
		} else {
			editor = DataStreamUtil.decode(encoded);
			StreamTuple tuple = findCurrentStream(editor);
			tuple.getStream().getStreams().add(stream);
			editor.getLocation().add(tuple.getStream().getStreams().size()-1);
		}

		encoded = DataStreamUtil.encode(editor);
		viewAggregation(encoded);
	}

	public static void viewAggregation(String encoded) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule stream = tuple.getStream();
		String path = tuple.getPath();

		render(stream, path, encoded);
	}

	public static void aggregationComplete(String encoded) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		List<Integer> locs = editor.getLocation();
		locs.remove(locs.size()-1);
		encoded = DataStreamUtil.encode(editor);
		viewAggregation(encoded);
	}

	private static StreamTuple findCurrentStream(StreamEditor editor) {
		StreamModule currentStr = editor.getStream();
		if(editor.getLocation().size() == 0)
			return new StreamTuple(currentStr, currentStr.getName());;

		String path = currentStr.getName();
		//first, find current location of what user was just looking at...
		for(Integer loc : editor.getLocation()) {
			currentStr = currentStr.getStreams().get(loc);
			path += " -> "+currentStr.getName();
		}
		return new StreamTuple(currentStr, path);
	}

	public static void editStream(String encoded) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule stream = tuple.getStream();
		List<StreamModule> modules = stream.getStreams();
		
		render(modules, encoded);
	}

	public static void chooseModule(String encoded, String action) {
		RawProcessorFactory factory = ModuleController.fetchFactory();
		List<String> modules = factory.fetchProcessorNames();
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule stream = tuple.getStream();
		
		if("add".equals(action)) {
			//on an add, we need to insert
			int newLocation = stream.getStreams().size();
			//add this location we are getting so when we redirect to the GET request, it will look up
			//this new location next...
			editor.getLocation().add(newLocation);
			stream.getStreams().add(new StreamModule());

			encoded = DataStreamUtil.encode(editor);
			tuple = findCurrentStream(editor);
			stream = tuple.getStream();
		}

		render(modules, stream, encoded);
	}

	public static void postModule(String encoded, String moduleName) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule stream = tuple.getStream();
		stream.setModule(moduleName);
		encoded = DataStreamUtil.encode(editor);
		editStream(encoded);
	}
}
