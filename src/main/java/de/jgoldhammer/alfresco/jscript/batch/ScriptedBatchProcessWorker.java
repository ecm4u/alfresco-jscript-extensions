package de.jgoldhammer.alfresco.jscript.batch;

import org.alfresco.repo.batch.BatchProcessor.BatchProcessWorker;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.ScriptService;
import org.springframework.util.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * a de.jgoldhammer.alfresco.jscript.batch process worker which uses the scriptservice to execute the provided script for a set of nodes.
 *
 * @author jgoldhammer
 *
 */
public final class ScriptedBatchProcessWorker implements BatchProcessWorker<Collection<ScriptNode>> {
	private final boolean runAsSystem;
	private final Scriptable batchScope;
	private final String processorFunction;
	private Context context;
	private static final String PROCESSING_SCRIPT = "\n for (var index = 0; index < processingNodes.length; index++) {process(processingNodes[index]);}";
	private static final String BEFORE_PROCESSING_SCRIPT = "\n beforeProcess(processingNodes)";
	private static final String AFTER_PROCESSING_SCRIPT = "\n afterProcess(processingNodes)";
	private ScriptService scriptService;
	private ServiceRegistry serviceRegistry;
	private String beforeProcessFunction;
	private String afterProcessFuntion;
	private List<ScriptNode> nodeRefs;

	ScriptedBatchProcessWorker(boolean runAsSystem, Scriptable batchScope, String processorFunction, List<ScriptNode> nodeRefs, String beforeProcessFunction, String afterProcessFuntion, Context context,
			ServiceRegistry serviceRegistry, ScriptService scriptService) {
		this.runAsSystem = runAsSystem;
		this.batchScope = batchScope;
		this.processorFunction = processorFunction;
		this.nodeRefs = nodeRefs;
		this.beforeProcessFunction = beforeProcessFunction;
		this.afterProcessFuntion = afterProcessFuntion;
		this.context = context;
		this.serviceRegistry = serviceRegistry;
		this.scriptService = scriptService;
	}

	@Override
	public void beforeProcess() throws Throwable {
		if (runAsSystem) {
			AuthenticationUtil.setRunAsUserSystem();
		}

		if(StringUtils.hasText(beforeProcessFunction)){
			
			scriptService.executeScriptString("javascript", beforeProcessFunction+BEFORE_PROCESSING_SCRIPT, createNodesModel(nodeRefs.toArray()));
		}

	}

	@Override
	public void afterProcess() throws Throwable {
		if(StringUtils.hasLength(afterProcessFuntion)){
			scriptService.executeScriptString("javascript", afterProcessFuntion+AFTER_PROCESSING_SCRIPT, createNodesModel(nodeRefs.toArray()));
		}
	}

	@Override
	public String getIdentifier(Collection<ScriptNode> entries) {
		return entries.toString();
	}

	@Override
	public void process(Collection<ScriptNode> entries) throws Throwable {

		String javascriptCode = processorFunction + PROCESSING_SCRIPT;
		scriptService.executeScriptString("javascript", javascriptCode, createNodesModel(entries.toArray()));

	}
//
//	/**
//	 * @param entries
//	 * @return
//	 */
//	private Object[] createScriptNodes(Collection<NodeRef> entries) {
//		Object[] scriptNodes = new Object[entries.size()];
//		int counter = 0;
//		for (NodeRef nodeRef : entries) {
//			scriptNodes[counter] = new ScriptNode(nodeRef, serviceRegistry);
//			counter++;
//		}
//		return scriptNodes;
//	}

	/**
	 * creates
	 *
	 * @param scriptNodes
	 * @return
	 */
	private HashMap<String, Object> createNodesModel(Object[] scriptNodes) {
		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("processingNodes", context.newArray(batchScope, scriptNodes));
		return model;
	}
}