package controllers.gui;

import models.message.StreamEditor;
import models.message.StreamModule;

public class TreeInfo {

	private StreamModule root;
	private StreamModule selected;
	private StreamEditor editor;
	private int currentId;

	public TreeInfo(StreamModule root, StreamModule selected, StreamEditor editor) {
		this.root = root;
		this.selected = selected;
		this.editor = editor;
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

}
