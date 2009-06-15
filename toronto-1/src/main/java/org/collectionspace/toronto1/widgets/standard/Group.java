package org.collectionspace.toronto1.widgets.standard;

import org.collectionspace.toronto1.widgets.Fragment;

public interface Group extends Fragment {
	public void addMember(String title,Fragment content);
	public void addHint(String hint);
}
