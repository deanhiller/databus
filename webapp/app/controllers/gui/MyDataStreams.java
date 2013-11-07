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
import play.mvc.Router;
import play.mvc.With;
import controllers.gui.auth.GuiSecure;
import controllers.gui.util.ChartUtil;
import controllers.modules2.framework.ModuleController;
import controllers.modules2.framework.RawProcessorFactory;
import controllers.modules2.framework.procs.MetaInformation;
import controllers.modules2.framework.procs.NumChildren;
import controllers.modules2.framework.procs.PullProcessor;
import controllers.modules2.framework.procs.StreamsProcessor;

@With(GuiSecure.class)
public class MyDataStreams extends Controller {

	private static final Logger log = LoggerFactory.getLogger(MyDataStreams.class);

	public static void start() {
		render();
	}

	public static void viewStream(String encoded) {
		Map<String, String> vars = ChartUtil.decodeVariables(encoded);
		StreamEditor editor = DataStreamUtil.decode(vars);
		editor.getLocation().clear();
		encoded = DataStreamUtil.encode(editor, vars);
		render(encoded);
	}

	public static void beginEdit(String encoded, String type) {
		StreamEditor editor = null;
		Map<String, String> vars = new HashMap<String, String>();
		if("start".equals(encoded)) {
			editor = new StreamEditor();
		} else {
			vars = ChartUtil.decodeVariables(encoded);
			editor = DataStreamUtil.decode(vars);
		}
		
		StreamModule module = findCurrentStream(editor);
		if("add".equals(type)) {
			StreamModule child = new StreamModule();
			List<StreamModule> streams = module.getStreams();
			streams.add(child);
			editor.getLocation().add(streams.size()-1);
			module = child;
			module.setModule("rawdataV1");
		} else if("insert".equals(type)) {
			StreamModule child = new StreamModule();
			StreamModule parent = findParent(editor);
			List<StreamModule> streams = parent.getStreams();
			for(int i = 0; i < streams.size(); i++) {
				StreamModule mod = streams.get(i);
				if(mod == module) {
					streams.remove(i); //remove this module
					streams.add(i, child); //insert new child
					child.getStreams().add(mod); //add the module we removed to this new parent
					module = child;
					module.setModule("CURRENT");
					break;
				}
			}
		} else if("delete".equals(type)) {
			StreamModule parent = findParent(editor);
			NumChildren parentNum = findNum(parent);
			int numGrandChildren = module.getStreams().size();
			
			//Basically if grandchildren size is 1, this is easy, delete node and move the one grandchild up
			//If numGrandChildren is > 1 then the parent MUST support having many children or we delete the subtree
			if(numGrandChildren == 1 || 
					(numGrandChildren > 1 && parentNum == NumChildren.MANY)) {
				deleteNode(parent, module);
			} else
				deleteSubTree(parent, module);

			encoded = DataStreamUtil.encode(editor, vars);
			viewStream(encoded);
		}

		encoded = DataStreamUtil.encode(editor, vars);
		editModule(encoded);
	}

	private static void deleteNode(StreamModule parent, StreamModule child) {
		List<StreamModule> streams = parent.getStreams();
		for(int i = 0; i < streams.size(); i++) {
			StreamModule mod = streams.get(i);
			if(mod == child) {
				streams.remove(i); //remove this module
				List<StreamModule> grandchildren = child.getStreams();
				streams.addAll(i, grandchildren);
				break;
			}
		}		
	}

	private static void deleteSubTree(StreamModule parent, StreamModule module) {
		List<StreamModule> streams = parent.getStreams();
		for(int i = 0; i < streams.size(); i++) {
			StreamModule mod = streams.get(i);
			if(mod == module) {
				streams.remove(i); //remove this module
				break;
			}
		}		
	}

	private static NumChildren findNum(StreamModule parent) {
		RawProcessorFactory factory = ModuleController.fetchFactory();
		Map<String, PullProcessor> procs = factory.fetchPullProcessors();
		PullProcessor proc = procs.get(parent.getModule());
		if(proc == null)
			return NumChildren.ONE;
		MetaInformation meta = proc.getGuiMeta();
		return meta.getNumChildren();
	}

	private static StreamModule findParent(StreamEditor editor) {
		StreamModule currentStr = editor.getStream();
		if(editor.getLocation().size() == 0)
			return currentStr;

		//first, find current location of what user was just looking at...
		List<Integer> locList = editor.getLocation();
		for(int i = 0; i < locList.size()-1; i++) {
			Integer loc = locList.get(i);
			currentStr = currentStr.getStreams().get(loc);
		}
		return currentStr;
	}

	public static void editModule(String encoded) {
		Map<String, String> vars = ChartUtil.decodeVariables(encoded);
		StreamEditor editor = DataStreamUtil.decode(vars);
		StreamModule module = findCurrentStream(editor);
		RawProcessorFactory factory = ModuleController.fetchFactory();
		List<MetaInformation> modules = factory.fetchAllModules();

		encoded = DataStreamUtil.encode(editor, vars);
		render(modules, module, encoded);
	}

	public static void postModule(String encoded, String moduleName) {
		Map<String, String> vars = ChartUtil.decodeVariables(encoded);
		StreamEditor editor = DataStreamUtil.decode(vars);
		StreamModule module = findCurrentStream(editor);
		RawProcessorFactory factory = ModuleController.fetchFactory();
		Map<String, PullProcessor> procs = factory.fetchPullProcessors();
		MetaInformation meta = procs.get(moduleName).getGuiMeta();
		if(meta.isTerminal() && module.getStreams().size() > 0) {
			flash.error("The module selected can only be inserted by clicking 'Add Child' as it is a terminal node that can't have children");
			editModule(encoded);
		} else if(!meta.isAggregation() && module.getStreams().size() > 1) {
			flash.error("This aggregation has "+module.getStreams().size()+" children yet the module you selected only supports one child");
			editModule(encoded);
		}
		
		module.setModule(moduleName);
		
		if(meta.getParameterMeta().size() == 0) {
			editor.getLocation().clear(); //reset location since we are done
			encoded = DataStreamUtil.encode(editor, vars);
			viewStream(encoded);
		}

		encoded = DataStreamUtil.encode(editor, vars);
		editModuleParams(encoded);
	}

	public static void editModuleParams(String encoded) {
		Map<String, String> vars = ChartUtil.decodeVariables(encoded);
		StreamEditor editor = DataStreamUtil.decode(vars);
		StreamModule module = findCurrentStream(editor);
		
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

		render(encoded, module, paramList);
	}
	
	public static void postModuleParams(String encoded, int index) {
		Map<String, String> vars = ChartUtil.decodeVariables(encoded);
		StreamEditor editor = DataStreamUtil.decode(vars);
		StreamModule module = findCurrentStream(editor);
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

		if(validation.hasErrors()) {
			validation.keep();
			encoded = DataStreamUtil.encode(editor, vars);
			editModuleParams(encoded);
		}
		
		editor.getLocation().clear();
		encoded = DataStreamUtil.encode(editor, vars);
		viewStream(encoded);
	}

	public static void finish(String encoded) {
		Map<String, String> vars = ChartUtil.decodeVariables(encoded);
		StreamEditor editor = DataStreamUtil.decode(vars);
		//first find the first aggregation
		StreamModule current = editor.getStream();
		while(current.getStreams().size() == 1) {
			current = current.getStreams().get(0);
		}
		
		List<StreamModule> leafNodes = new ArrayList<StreamModule>();
		List<StreamModule> aggregations = new ArrayList<StreamModule>();
		findThings(leafNodes, aggregations, editor.getStream());

		RawProcessorFactory factory = ModuleController.fetchFactory();
		Map<String, PullProcessor> modules = factory.fetchPullProcessors();		
		for(StreamModule m : leafNodes) {
			PullProcessor proc = modules.get(m.getModule());
			if(proc.getGuiMeta().getNumChildren() != NumChildren.NONE) {
				flash.error("Not all leaf nodes are data sources.  Module="+m.getModule()+" is not a data source");
				viewStream(encoded);
				break;
			}
		}
		
		for(StreamModule agg : aggregations) {
			if(!isAggAligned(agg)) {
				flash.error("Not all streams are time aligned(Use splines, time averages, or interpolation to align times first)");
				viewStream(encoded);
			}
		}
		
		render(encoded);
	}

	private static void findThings(List<StreamModule> leafNodes, List<StreamModule> aggregations, StreamModule stream) {
		RawProcessorFactory factory = ModuleController.fetchFactory();
		Map<String, PullProcessor> modules = factory.fetchPullProcessors();
		if(stream.getStreams().size() > 1) {
			MetaInformation aggMeta = modules.get(stream.getModule()).getGuiMeta();
			if(aggMeta.isNeedsAlignment())
				aggregations.add(stream);
		}

		for(StreamModule m : stream.getStreams()) {
			if(m.getStreams().size() == 0)
				leafNodes.add(m);
			else
				findThings(leafNodes, aggregations, m);
		}
	}

	private static boolean isAggAligned(StreamModule aggregation) {
		RawProcessorFactory factory = ModuleController.fetchFactory();
		Map<String, PullProcessor> modules = factory.fetchPullProcessors();
		
		boolean isAligned = true;
		for(StreamModule child : aggregation.getStreams()) {
			MetaInformation meta = modules.get(child.getModule()).getGuiMeta();
			if(meta.isAggregation()) {
				if(!isAggAligned(child))
					isAligned = false;
			} else {
				if(!isChildAligned(child))
					isAligned = false;
			}
		}
		return isAligned;
	}

	private static boolean isChildAligned(StreamModule current) {
		RawProcessorFactory factory = ModuleController.fetchFactory();
		Map<String, PullProcessor> modules = factory.fetchPullProcessors();
		MetaInformation meta = modules.get(current.getModule()).getGuiMeta();
		if(meta.isTimeAligning())
			return true;

		while(current.getStreams().size() > 0) {
			current = current.getStreams().get(0);
			if(current.getStreams().size() > 1) {
				return isAggAligned(current);
			}
			
			MetaInformation meta2 = modules.get(current.getModule()).getGuiMeta();
			if(meta2.isTimeAligning())
				return true;
		}
		return false;
	}

	private static StreamModule findCurrentStream(StreamEditor editor) {
		StreamModule currentStr = editor.getStream();
		if(editor.getLocation().size() == 0)
			return currentStr;

		//first, find current location of what user was just looking at...
		for(Integer loc : editor.getLocation()) {
			List<StreamModule> streams = currentStr.getStreams();
			if(loc >= streams.size())
				return null;
			currentStr = currentStr.getStreams().get(loc);
		}
		return currentStr;
	}

	public static void fetchJsonTree(String encoded) {
		Map<String, String> vars = ChartUtil.decodeVariables(encoded);
		StreamEditor editor = DataStreamUtil.decode(vars);
		StreamModule module = findCurrentStream(editor);
		StreamModule root = editor.getStream();
		Map<String, Object> root2 = new HashMap<String, Object>();
		List<Integer> location = new ArrayList<Integer>();
		
		TreeInfo info = new TreeInfo(root, module, editor, vars);
		copyTree(null, root, root2, info, location);
		
		renderJSON(root2);
	}

	private static void copyTree(StreamModule parent, StreamModule current, Map<String, Object> current2, TreeInfo info, List<Integer> location) {
		transfer(parent, current, current2, info, location);

		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		current2.put("children", list);
		List<StreamModule> streams = current.getStreams();
		for(int i = 0; i < streams.size(); i++) {
			StreamModule child = streams.get(i);
			HashMap<String, Object> child2 = new HashMap<String, Object>();
			list.add(child2);
			List<Integer> newLocation = clone(location);
			newLocation.add(i);
			copyTree(current, child, child2, info, newLocation);
		}
	}

	private static List<Integer> clone(List<Integer> location) {
		List<Integer> newLoc = new ArrayList<Integer>(location.size());
		newLoc.addAll(location);
		return newLoc;
	}

	private static void transfer(StreamModule parent, StreamModule now, Map<String, Object> now2, TreeInfo info, List<Integer> location) {
		StreamModule selectedStream = info.getSelected();
		StreamModule root = info.getRoot();
		StreamModule second = root.getStreams().get(0);
		
		RawProcessorFactory factory = ModuleController.fetchFactory();
		Map<String, PullProcessor> procs = factory.fetchPullProcessors();
		PullProcessor proc = procs.get(now.getModule());
		
		now2.put("id", info.getNextId());
		if(now == root)
			now2.put("isRoot", true);
		else if(now == selectedStream)
			now2.put("isSelected", true);
		else if(now == second && now.getStreams().size() != 1) {
			now2.put("hideMinus", true);
		}

		if(parent != null) {
			NumChildren numChildSupported = NumChildren.ONE;
			PullProcessor parentProc = procs.get(parent.getModule());
			if(parentProc != null)
				numChildSupported = parentProc.getGuiMeta().getNumChildren();
			
			if(now.getStreams().size() > 1 && numChildSupported != NumChildren.MANY) {
				//add the flag to popup a warning and explain why it can't be deleted yet
				now2.put("minusPopupLink", true);
			}
		}
			
		now2.put("module", now.getModule());

		if(proc != null) {
			MetaInformation meta = proc.getGuiMeta();
			
			if(meta.getNumChildren() == NumChildren.NONE) {
				String name = now.getParams().get("table");
				now2.put("module", name);
			} else
				now2.put("module", meta.getGuiLabel());
			
			boolean canAddChild = false;
			switch(meta.getNumChildren()) {
			case MANY:
				canAddChild = true;
				break;
			case ONE:
				if(now.getStreams().size() == 0)
					canAddChild = true;
				break;
			}
			
			now2.put("canAddChild", canAddChild);

			StreamEditor editor = info.getEditor();
			editor.setLocation(location);
			String encoded = DataStreamUtil.encode(editor, info.getVariableMap());
			
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("encoded", encoded);
			map.put("type", "insert");
			String insertUrl = Router.reverse("gui.MyDataStreams.beginEdit", map).url;
			map.put("type", "add");
			String addUrl = Router.reverse("gui.MyDataStreams.beginEdit", map).url;
			map.put("type", "edit");
			String editUrl = Router.reverse("gui.MyDataStreams.beginEdit", map).url;
			map.put("type", "delete");
			String deleteUrl = Router.reverse("gui.MyDataStreams.beginEdit", map).url;
			
			now2.put("insertUrl", insertUrl);
			now2.put("addUrl", addUrl);
			now2.put("editUrl", editUrl);
			now2.put("deleteUrl", deleteUrl);
		}
	}

	private static MetaInformation fetchMeta(StreamModule module) {
		RawProcessorFactory factory = ModuleController.fetchFactory();
		Map<String, PullProcessor> nameToProc = factory.fetchPullProcessors();
		PullProcessor proc = nameToProc.get(module.getModule());
		return proc.getGuiMeta();
	}

	public static void createChart(String encoding) {
		MyChartsGeneric.modifyChart(encoding);
	}
}
