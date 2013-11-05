package controllers.gui;

import java.util.Map;

import models.message.StreamEditor;
import models.message.StreamModule;

public class TreeInfo {

	private StreamModule root;
	private StreamModule selected;
	private StreamEditor editor;
	private int currentId;
	private Map<String, String> variableMap;

	public TreeInfo(StreamModule root, StreamModule selected, StreamEditor editor, Map<String, String> variableMap) {
		this.root = root;
		this.selected = selected;
		this.editor = editor;
		this.variableMap = variableMap;
	}
	
	public int getNextId() {
		return currentId++;
	}

	public StreamModule getRoot() {
		return root;
	}

	public StreamModule getSelected() {
		return selected;
	}

	public StreamEditor getEditor() {
		return editor;
	}

	public int getCurrentId() {
		return currentId;
	}

	public Map<String, String> getVariableMap() {
		return variableMap;
	}

	
}
