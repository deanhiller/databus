package controllers.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import models.message.ChartVarMeta;
import models.message.StreamEditor;
import models.message.StreamModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.With;
import controllers.gui.auth.GuiSecure;
import controllers.gui.util.ChartUtil;
import controllers.modules2.framework.ModuleController;
import controllers.modules2.framework.RawProcessorFactory;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.StreamsProcessor;

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
		StreamModule aggregation = tuple.getStream();
		String path = tuple.getPath();

		render(aggregation, path, encoded);
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

	public static void postDeleteStream(String encoded, int index) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule stream = tuple.getStream();
		
		stream.getStreams().remove(index-1);
		
		encoded = DataStreamUtil.encode(editor);
		viewAggregation(encoded);
	}

	public static void editStream(String encoded, int index) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule stream = tuple.getStream();
		if(index < 0) {
			//add
			StreamModule child = new StreamModule();
			child.setModule("stream");
			stream.getStreams().add(child);
			editor.getLocation().add(stream.getStreams().size()-1);
			stream = child;
		} else {
			//edit
			stream = stream.getStreams().get(index-1);
			editor.getLocation().add(index-1);
		}
		encoded = DataStreamUtil.encode(editor);
		render(stream, encoded);
	}

	public static void postStream(String encoded, String name) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule stream = tuple.getStream();
		stream.setName(name);
		encoded = DataStreamUtil.encode(editor);
		viewStream(encoded);
	}
	
	public static void viewStream(String encoded) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule stream = tuple.getStream();
		String path = tuple.getPath();
		render(encoded, stream, path);
	}

	public static void editModule(String encoded, int index) {
		RawProcessorFactory factory = ModuleController.fetchFactory();
		List<String> modules = factory.fetchProcessorNames();
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule parent = tuple.getStream();

		StreamModule module = null;
		if(index >= 0) {
			module = parent.getStreams().get(index-1);
		}
		encoded = DataStreamUtil.encode(editor);
		render(modules, module, encoded, index);
	}

	public static void postModule(String encoded, String moduleName, int index) {
		RawProcessorFactory factory = ModuleController.fetchFactory();
		Map<String, PullProcessor> pullProcs = factory.fetchPullProcessors();
		PullProcessor pullProcessor = pullProcs.get(moduleName);
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule parent = tuple.getStream();
		
		if(pullProcessor instanceof StreamsProcessor) {
			//here, they are adding an aggregation so we need to switch to aggregations page now instead(and allow them to name the aggregation as well)
			StreamModule module = new StreamModule();
			module.setModule(moduleName);
			parent.getStreams().add(module);
			editor.getLocation().add(parent.getStreams().size()-1);
			encoded = DataStreamUtil.encode(editor);
			viewAggregation(encoded);
		}
		
		if(index >= 0) {
			StreamModule module = parent.getStreams().get(index-1);
			module.setModule(moduleName);
		} else {
			StreamModule module = new StreamModule();
			module.setModule(moduleName);
			parent.getStreams().add(module);
			index = parent.getStreams().size(); //the one we just added
		}
		encoded = DataStreamUtil.encode(editor);
		editModuleParams(encoded, index);
	}

	public static void fetchJsonTree(String encoded) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamModule root = editor.getStream();
		Map<String, Object> root2 = new HashMap<String, Object>();
		copyTree(root, root2);
		
		renderJSON(root2);
	}

	private static void copyTree(StreamModule current, Map<String, Object> current2) {
		transfer(current, current2);
		if("stream".equals(current.getModule())) {
			//modify current to the node with no children (or with the real children)
			//the first child is always the one that is the aggregation or rawdata
			if(current.getStreams().size() > 0)
				current = current.getStreams().get(0);
		}
		
		if(current.getStreams().size() == 0)
			return;

		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		current2.put("children", list);
		for(StreamModule child : current.getStreams()) {
			HashMap<String, Object> child2 = new HashMap<String, Object>();
			list.add(child2);
			copyTree(child, child2);
		}
	}

	private static void transfer(StreamModule now, Map<String, Object> now2) {
		if("stream".equals(now.getModule())) {
			String name = now.getName()+"(";
			for(StreamModule m : now.getStreams()) {
				name+="<-"+m.getModule();
			}
			name += ")";
			now2.put("name", name);
		} else {
			now2.put("name", now.getName());
		}
	}

	public static void editModuleParams(String encoded, int index) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		String path = tuple.getPath();
		StreamModule parent = tuple.getStream();
		StreamModule module = parent.getStreams().get(index-1);
		
		RawProcessorFactory factory = ModuleController.fetchFactory();
		Map<String, PullProcessor> nameToProc = factory.fetchPullProcessors();
		PullProcessor proc = nameToProc.get(module.getModule());
		Map<String, ChartVarMeta> paramMeta = proc.getParameterMeta();
		Map<String, String> params2 = module.getParams();
		List<VarWrapper> paramList = new ArrayList<VarWrapper>();
		//here we add variables. to distinguish it from any other parameters that may be coming in like "encoded" above or "index", etc....
		for(Entry<String, ChartVarMeta> entry : paramMeta.entrySet()) {
			VarWrapper var = new VarWrapper(entry.getValue());
			String value = params2.get(entry.getKey());
			if(value == null)
				value = entry.getValue().getDefaultValue();
			var.setValue(value);
			paramList.add(var);
		}
		//We need to lookup the parameters here and form a dynamic form just like we do in the charting wizard
		
		render(encoded, index, module, path, paramList);
	}
	
	public static void postModuleParams(String encoded, int index) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule parent = tuple.getStream();
		StreamModule module = parent.getStreams().get(index-1);
		
		//apply parameters here...decode and re-encode StreamEditor
		Map<String, String[]> paramMap = params.all();
		Map<String, String> params = module.getParams();
		params.clear(); //clear whatever the previous module was before
		for(String key : paramMap.keySet()) {
			if(key.startsWith("variables.")) {
				String[] values = paramMap.get(key);
				String value = values[0];
				String javascriptKey = key.substring("variables.".length());
				params.put(javascriptKey, value);
			}
		}

		encoded = DataStreamUtil.encode(editor);
		viewStream(encoded);
	}
	
	public static void streamComplete(String encoded) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		List<Integer> locs = editor.getLocation();
		locs.remove(locs.size()-1);
		encoded = DataStreamUtil.encode(editor);
		viewAggregation(encoded);
	}
}
