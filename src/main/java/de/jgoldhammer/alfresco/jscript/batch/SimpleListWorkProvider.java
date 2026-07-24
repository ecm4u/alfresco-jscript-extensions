/**
 *
 */
package de.jgoldhammer.alfresco.jscript.batch;

import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * a simple list based batch process work provider.
 *
 * @author Jens Goldhammer
 *
 */
public class SimpleListWorkProvider implements BatchProcessWorkProvider<Collection<ScriptNode>> {

    private List<ScriptNode> simpleList;
    private final int collectSize;
	private final int size;
	private int index = 0;

    /**
     * @param simpleList
	 * 			the list of the noderefs
	 * @param  listSize
	 * 			the size of the list
     */
    public SimpleListWorkProvider(List<ScriptNode> simpleList, final int listSize) {
        this.simpleList = simpleList;
		this.collectSize = listSize;
		this.size = this.simpleList.size();
    }

    /**
 	* the size of the simple list.
 	*/
    public int getTotalEstimatedWorkSize() {
        return simpleList.size();
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized Collection<Collection<ScriptNode>> getNextWork() {
		final Collection<Collection<ScriptNode>> items;
		if (this.size > this.index) {
			items = new ArrayList<Collection<ScriptNode>>();
			List<ScriptNode> nodes = new ArrayList<>();
			items.add(nodes);
			for (int size = 0; size < this.collectSize && this.size > this.index; this.index++, size++) {
				nodes.add(this.simpleList.get(this.index));
			}
		} else {
			items = new ArrayList<>();
		}
		return items;
	}

}
