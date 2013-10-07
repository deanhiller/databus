package controllers.gui;

import java.util.ArrayList;
import java.util.Collections;
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
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.StreamsProcessor;

@With(GuiSecure.class)
public class MyDataStreams extends Controller {

	private static final Logger log = LoggerFactory.getLogger(MyDataStreams.class);

	public static void start() {
		render();
	}

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
		StreamTuple tuple = findCurrentStream(editor);
		if(tuple.getStream().getStreams().size() == 0) {
			validation.addError("somefield", "error");
			flash.error("An aggregation must have at least one stream");
		} else if(!streamsAligned(tuple.getStream().getStreams())) {
			validation.addError("somefield", "error");
			flash.error("Streams must be aligned so each stream needs to be splined or time averaged or linearly interpolated so times match up");
		}
		
		if(validation.hasErrors()) {
			viewAggregation(encoded);
		}
		
		List<Integer> locs = editor.getLocation();
		locs.remove(locs.size()-1);
		StreamTuple str = findCurrentStream(editor);
		encoded = DataStreamUtil.encode(editor);
		viewStream(encoded);
	}

	private static boolean streamsAligned(List<StreamModule> streams) {
		//let's first fine all modules with "stream".equals(getModule()) as they have all the relevant children
		RawProcessorFactory factory = ModuleController.fetchFactory();
		Map<String, PullProcessor> modules = factory.fetchPullProcessors();
		
		for(StreamModule m : streams) {
			if(!isStreamAligned(m, modules)) 
				return false;
		}
		return true;
	}

	private static boolean isStreamAligned(StreamModule m, Map<String, PullProcessor> modules) {
		//we are looking for one child that is aligning the times
		for(StreamModule child : m.getStreams()) {
			PullProcessor proc = modules.get(child.getModule());
			MetaInformation meta = proc.getGuiMeta();
			if(meta.isTimeAligning())
				return true;
			else if(meta.isStreamTerminator() && child.getStreams().size() > 0) {
				return streamsAligned(child.getStreams());
			}
		}
		return false;
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
		if("start".equals(encoded)) {
			render(null, encoded);
		}

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
		StreamEditor editor;
		if("start".equals(encoded)) {
			StreamModule stream = new StreamModule();
			stream.setName(name);
			stream.setModule("stream");
			editor = new StreamEditor();
			editor.setStream(stream);
		} else {
			editor = DataStreamUtil.decode(encoded);
			StreamTuple tuple = findCurrentStream(editor);
			StreamModule stream = tuple.getStream();
			stream.setName(name);
		}

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

	public static void insertModule(String encoded) {
		RawProcessorFactory factory = ModuleController.fetchFactory();
		List<String> modules = factory.fetchNonTerminalModules();
		StreamEditor editor = DataStreamUtil.decode(encoded);

		int index = -2;
		StreamModule module = null;
		encoded = DataStreamUtil.encode(editor);
		render("@editModule", modules, module, encoded, index);
	}

	public static void editModule(String encoded, int index) {
		RawProcessorFactory factory = ModuleController.fetchFactory();
		List<String> modules = factory.fetchAllModules();
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule parent = tuple.getStream();

		int size = parent.getStreams().size();
		StreamModule module = null;
		if(index >= 0) {
			module = parent.getStreams().get(index-1);
		} else if(size > 0) {
			StreamModule mod = parent.getStreams().get(size - 1);
			PullProcessor proc = factory.fetchPullProcessors().get(mod.getModule());
			if(proc.getGuiMeta().isStreamTerminator()) {
				if(mod.getModule().startsWith("rawdata"))
					flash.error("The module="+mod.getModule()+" is the last source.  Insert modules instead");
				else
					flash.error("The module="+mod.getModule()+" is a source....You can edit the module to add stuff to it.");
				viewStream(encoded);
			}
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

		if(index >= 0) {
			StreamModule module = parent.getStreams().get(index-1);
			String prevModule = module.getModule();
			module.setModule(moduleName);
			
			PullProcessor prevProc = pullProcs.get(prevModule);
			if(prevProc instanceof StreamsProcessor && !(pullProcessor instanceof StreamsProcessor)) {
				//we are changing from StreamsProc to NOT streams proc so wipe the children out
				module.getStreams().clear();
				module.setName(null);
			} else if(pullProcessor instanceof StreamsProcessor){
				if(!(prevProc instanceof StreamsProcessor)) {
					//going from normal proc to an aggregation processor so wipe out parameters and go to aggregation page
					module.getParams().clear();
					module.setName(parent.getName()+"("+moduleName+")");
				}
				editor.getLocation().add(index-1);
				encoded = DataStreamUtil.encode(editor);
				viewAggregation(encoded);
			}
		} else if(index == -1) {
			StreamModule module = new StreamModule();
			module.setModule(moduleName);
			parent.getStreams().add(module);
			index = parent.getStreams().size();
			
			if(pullProcessor instanceof StreamsProcessor) {
				module.setName(parent.getName()+"("+moduleName+")");
				encoded = DataStreamUtil.encode(editor);
				viewStream(encoded);
			}
		} else if(index == -2) {
			//-2 is insert...kind of a hack really
			StreamModule module = new StreamModule();
			module.setModule(moduleName);
			parent.getStreams().add(0, module);
			index = 0+1; //the one we just added
		}
		
		encoded = DataStreamUtil.encode(editor);
		editModuleParams(encoded, index);
	}

	public static void fetchJsonTree(String encoded) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule root = editor.getStream();
		Map<String, Object> root2 = new HashMap<String, Object>();
		copyTree(root, root2, tuple.getStream(), root);
		
		renderJSON(root2);
	}

	private static void copyTree(StreamModule current, Map<String, Object> current2, StreamModule selectedStream, StreamModule root) {
		transfer(current, current2, selectedStream, root);
		if("stream".equals(current.getModule())) {
			//modify current to the node with no children (or with the real children)
			//the first child is always the one that is the aggregation or rawdata
			int size = current.getStreams().size();
			if(size > 0)
				current = current.getStreams().get(size-1);
		}

		List<StreamModule> streams = current.getStreams();
		if(streams.size() == 0)
			return;

		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		current2.put("children", list);
		for(StreamModule child : streams) {
			HashMap<String, Object> child2 = new HashMap<String, Object>();
			list.add(child2);
			copyTree(child, child2, selectedStream, root);
		}
	}

	private static void transfer(StreamModule now, Map<String, Object> now2, StreamModule selectedStream, StreamModule root) {
		if("stream".equals(now.getModule())) {
			String name = now.getName();
			List<StreamModule> streams = now.getStreams();
			String moduleList = "";
			for(int i = 0; i < streams.size()-1; i++) {
				StreamModule m = streams.get(i);
				moduleList+="<-"+m.getModule();
			}

			String value = "";
			if(streams.size() > 0) {
				StreamModule m = streams.get(streams.size()-1);
				value = "<-"+m.getModule();
				if("rawdataV1".equals(m.getModule()))
					value = "<-"+m.getParams().get("table");
			}
			now2.put("name", name);
			now2.put("moduleList", moduleList);
			now2.put("lastModule", value);
		} else {
			now2.put("name", now.getName());
		}
		
		//now == selectedStream is obvious but if the selectedStream is a module inside the stream, we select the stream
		//since modules cannot be selected
		if(now == selectedStream) {
			now2.put("selected", true);
		} else if("stream".equals(now.getModule()) && now.getStreams().contains(selectedStream)) {
			now2.put("selected", true);
			now2.put("isLastModule", true);
		}
		
		if(now == root)
			now2.put("root", true);
	}

	public static void editModuleParams(String encoded, int index) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		String path = tuple.getPath();
		StreamModule parent = tuple.getStream();
		StreamModule module = parent.getStreams().get(index-1);
		
		Map<String, String[]> paramMap = params.all();
		
		Map<String, ChartVarMeta> paramMeta = fetchMeta(module).getParameterMeta();
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

	private static MetaInformation fetchMeta(StreamModule module) {
		RawProcessorFactory factory = ModuleController.fetchFactory();
		Map<String, PullProcessor> nameToProc = factory.fetchPullProcessors();
		PullProcessor proc = nameToProc.get(module.getModule());
		return proc.getGuiMeta();
	}
	
	public static void postModuleParams(String encoded, int index) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule parent = tuple.getStream();
		StreamModule module = parent.getStreams().get(index-1);
		MetaInformation meta = fetchMeta(module);

		//apply parameters here...decode and re-encode StreamEditor
		Map<String, String[]> paramMap = params.all();
		Map<String, String> moduleParams = module.getParams();
		moduleParams.clear(); //clear whatever the previous module was before
		for(String key : paramMap.keySet()) {
			if(key.startsWith("variables.")) {
				String[] values = paramMap.get(key);
				String value = values[0];
				String javascriptKey = key.substring("variables.".length());
				if("".equals(value.trim()))
					value = null; //null out the value if it is an empty string
				moduleParams.put(javascriptKey, value);
			}
		}

		meta.validate(validation, moduleParams);

		encoded = DataStreamUtil.encode(editor);
		if(validation.hasErrors()) {
			validation.keep();
			editModuleParams(encoded, index);
		}
		
		viewStream(encoded);
	}
	
	public static void postDeleteModule(String encoded, int index) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule stream = tuple.getStream();
		
		stream.getStreams().remove(index-1);
		
		encoded = DataStreamUtil.encode(editor);
		viewStream(encoded);
	}
	
	public static void streamComplete(String encoded) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		
		StreamTuple str = findCurrentStream(editor);
		StreamModule stream = str.getStream();
		List<StreamModule> modules = stream.getStreams();
		if(modules.size() == 0) {
			validation.addError("somefield", "error");
			flash.error("A stream must have at least one module");
		} else {
			StreamModule module = modules.get(modules.size()-1);
			if(!"rawdataV1".equals(module.getModule())) {
				List<StreamModule> children = module.getStreams();
				if(children.size() == 0) {
					validation.addError("somefield", "error");
					flash.error("You must click 'Edit' on the aggregation and finish filling in his children first.");
				}
			}
		}
			
		for(int i = 0; i < modules.size(); i++) {
			StreamModule module = modules.get(i);
			MetaInformation meta = fetchMeta(module);
			if(i == modules.size()-1) {
				if(!meta.isStreamTerminator()) {
					validation.addError("somefield", "error");
					flash.error("The very last module in a stream must be a rawdataV1 or an aggregation");
				}
			} else {
				if(meta.isStreamTerminator()) {
					validation.addError("somefield", "error");
					flash.error("Only the very last module can be rawdataV1 or an aggregation");
				}
			}
		}

		if(validation.hasErrors()) {
			flash.keep();
			viewStream(encoded);
		}
		
		List<Integer> locs = editor.getLocation();
		if(locs.size() == 0) {
			//we are at the root element so they are finished now
			encoded = DataStreamUtil.encode(editor);
			finish(encoded);
		}

		locs.remove(locs.size()-1);
		encoded = DataStreamUtil.encode(editor);
		viewAggregation(encoded);
	}
	
	public static void finish(String encoded) {
		render(encoded);
	}
	
	public static void moveModuleUp(String encoded, int index) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule str = tuple.getStream();
		List<StreamModule> streams = str.getStreams();
		int realIndex = index-1;
		if(realIndex == 0 || realIndex > streams.size()-1) {
			viewStream(encoded);
		}
		
		StreamModule moveDown = streams.get(realIndex-1);
		StreamModule moveUp = streams.get(realIndex);
		
		//remove both from list and re-insert them
		streams.remove(realIndex-1);
		streams.remove(realIndex-1);
		
		streams.add(realIndex-1, moveUp);
		streams.add(realIndex, moveDown);
		
		encoded = DataStreamUtil.encode(editor);
		viewStream(encoded);
	}
	
	public static void moveModuleDown(String encoded, int index) {
		StreamEditor editor = DataStreamUtil.decode(encoded);
		StreamTuple tuple = findCurrentStream(editor);
		StreamModule str = tuple.getStream();
		List<StreamModule> streams = str.getStreams();
		int realIndex = index-1;
		if(realIndex < 0 || realIndex >= streams.size()-1) {
			viewStream(encoded);
		}
		
		StreamModule moveDown = streams.get(realIndex);
		StreamModule moveUp = streams.get(realIndex+1);
		
		//remove both from list and re-insert them
		streams.remove(realIndex);
		streams.remove(realIndex);
		
		streams.add(realIndex, moveUp);
		streams.add(realIndex+1, moveDown);
		
		encoded = DataStreamUtil.encode(editor);
		viewStream(encoded);
	}
}
