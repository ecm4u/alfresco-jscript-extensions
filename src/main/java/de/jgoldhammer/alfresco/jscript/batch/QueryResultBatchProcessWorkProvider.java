/**
 * 
 */
package de.jgoldhammer.alfresco.jscript.batch;

import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * a de.jgoldhammer.alfresco.jscript.batch process worker which iterates over the query result and provides a
 * list of size {@link #collectSize} as next work.
 * 
 * @author jgoldhammer
 * 
 */
public class QueryResultBatchProcessWorkProvider implements BatchProcessWorkProvider<Collection<ScriptNode>> {

	private final int collectSize;
	private final int size;
	private int index = 0;
	private final ResultSet searchResult;
	private final ServiceRegistry serviceRegistry;

	public QueryResultBatchProcessWorkProvider(final ResultSet searchResult, final int listSize, ServiceRegistry serviceRegistry) {
		this.searchResult = searchResult;
		this.collectSize = listSize;
		this.size = this.searchResult.length();
		this.serviceRegistry = serviceRegistry;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getTotalEstimatedWorkSize() {
		return this.searchResult.length();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized Collection<Collection<ScriptNode>> getNextWork() {
		final Collection<Collection<ScriptNode>> items;
		if (this.size > this.index) {
			items = new ArrayList<>();
			List<ScriptNode> nodes = new ArrayList<>();
			items.add(nodes);
			for (int size = 0; size < this.collectSize && this.size > this.index; this.index++, size++) {			
				// TODO FIXME serviceRegistry
				nodes.add(new ScriptNode(this.searchResult.getNodeRef(this.index), serviceRegistry));
			}
		} else {
			items = Collections.emptySet();
		}
		return items;
	}

}
