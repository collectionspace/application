package org.collectionspace.toronto1.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

public class TaskList implements Task {
	private List<Task> tasks=new ArrayList<Task>();

	public void addTask(Task t) { tasks.add(t); }

	public void preact(AJAXRequest request) throws TaskException {
		for(Task t : tasks) {
			t.preact(request);
		}
	}
	
	public void act(AJAXRequest request) throws TaskException {
		for(Task t : tasks) {
			t.act(request);
		}
	}

	public void postact(AJAXRequest request) throws TaskException {
		for(Task t : tasks) {
			t.postact(request);
		}
	}
}
