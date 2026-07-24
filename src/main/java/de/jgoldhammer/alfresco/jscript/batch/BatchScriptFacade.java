package de.jgoldhammer.alfresco.jscript.batch;

import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.repo.jscript.Scopeable;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.processor.BaseProcessorExtension;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.ScriptService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.transaction.TransactionService;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.springframework.extensions.webscripts.annotation.ScriptMethod;
import org.springframework.extensions.webscripts.annotation.ScriptMethodType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.mozilla.javascript.NativeJavaObject;

/**
 * fascade to a de.jgoldhammer.alfresco.jscript.batch processor implementation.
 *
 * @author jgoldhammer
 */

public class BatchScriptFacade extends BaseProcessorExtension implements Scopeable {

	private static final int FIXED_BATCH_SIZE = 1;
	private TransactionService transactionService;
	private SearchService searchService;
	private Scriptable scope;
	private ScriptService scriptService;

	public void setTransactionService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setScriptService(ScriptService scriptService) {
		this.scriptService = scriptService;
	}

	/**
	 * process the given processorfunction on a set of nodes which are the
	 * result of the provided luceneQuery. The processing takes place in a de.jgoldhammer.alfresco.jscript.batch
	 * processor with the given batchName, the number of workerthreads and the
	 * number of nodes as batchsize.
	 *
	 * @param batchName
	 *            the name of the de.jgoldhammer.alfresco.jscript.batch
	 * @param workerThreads
	 *            the number of threads which can be used for the de.jgoldhammer.alfresco.jscript.batch
	 *            processing
	 * @param batchSize
	 * 				size of the batch
	 * @param luceneQuery
	 *            the lucene query to execute to make the processing on the
	 *            nodes of the resultset
	 * @param processorFunction
	 *            the javascript function to process- the function must have the
	 *            named "process"
	 *
	 *            Example:
	 *            de.jgoldhammer.alfresco.jscript.batch.runForQuery('MyProcessor',4,10,'TEXT:alfresco',function
	 *            process(node){ logger.error(node); }, true);
	 * @param runAsSystem
	 *            true if the processing should be run as system, false to
	 *            process as the current user
	 * @param beforeProcessFunction
	 * 				the function to run before the processing
	 * @param afterProcessFunction
	 * 				the function to run after the processing
	 *
	 */
	@ScriptMethod(code = "batch.run('MyProcessor',4,10,'TEXT:alfresco',function process(node){logger.error(node);}, true);", help = "", output = "nothing", type = ScriptMethodType.WRITE)
	public void runForQuery(String batchName, int workerThreads, final int batchSize, final String luceneQuery,
			final String processorFunction, final boolean runAsSystem, final String beforeProcessFunction,
			final String afterProcessFunction) {

		final SearchParameters params = new SearchParameters();
		params.setLimitBy(LimitBy.UNLIMITED);
		params.setLanguage(SearchService.LANGUAGE_LUCENE);
		params.setQuery(luceneQuery);
		params.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

		final ResultSet searchResult = searchService.query(params);
		BatchProcessor<Collection<ScriptNode>> processor;
		final Scriptable batchScope = this.scope;

		try {
			if (searchResult.length() != 0) {
				searchResult.setBulkFetch(false);

				processor = new BatchProcessor<>(batchName, transactionService.getRetryingTransactionHelper(),
						new QueryResultBatchProcessWorkProvider(searchResult, batchSize, serviceRegistry), workerThreads, FIXED_BATCH_SIZE, null,
						null, FIXED_BATCH_SIZE);

				processor.process(
						new ScriptedBatchProcessWorker(runAsSystem, batchScope, processorFunction, null, beforeProcessFunction,
								afterProcessFunction, Context.getCurrentContext(), serviceRegistry, scriptService), true);
			}
		} finally {
			searchResult.close();
		}

	}

	/**
	 * process the given processorfunction on a set of nodes which are given as
	 * native array. The processing takes place in a de.jgoldhammer.alfresco.jscript.batch processor with the
	 * given batchName, the number of workerthreads and the number of nodes as
	 * batchsize.
	 *
	 * @param batchName
	 *            the name of the de.jgoldhammer.alfresco.jscript.batch
	 * @param workerThreads
	 *            the number of threads which can be used for the de.jgoldhammer.alfresco.jscript.batch
	 *            processing
	 * @param batchSize
	 * 			size of the batch
	 * @param scriptNodes
	 *            the array of scriptnodes to process (if you have your own
	 *            logic to determine the nodes)
	 * @param processorFunction
	 *            the javascript function to process- the function must have the
	 *            named "process"
	 *
	 *            Example: de.jgoldhammer.alfresco.jscript.batch.runForNodes('MyProcessor', 4, 10, nodes,
	 *            function process(node){ logger.error(node); }, true);
	 * @param runAsSystem
	 *            true if the processing should be run as system, false to
	 *            process as the current user
	 * @param beforeProcessFunction
	 * 				the function to run before the processing
	 * @param afterProcessFunction
	 * 				the function to run after the processing
	 *
	 */
	@SuppressWarnings("unchecked")
	@ScriptMethod(code = "de.jgoldhammer.alfresco.jscript.batch.run('MyProcessor',4,10,'TEXT:alfresco',function process(node){logger.error(node);}, true);", help = "", output = "nothing", type = ScriptMethodType.WRITE)
	public void runForNodes(String batchName, int workerThreads, final int batchSize, final NativeArray scriptNodes,
			final String processorFunction, final boolean runAsSystem, final String beforeProcessFunction,
			final String afterProcessFunction) {

		
		List<ScriptNode> nodes = new ArrayList<>();
		scriptNodes.forEach(sn -> { 
			nodes.add((ScriptNode)sn);
		});
		//scriptNodes.stream().map(sn -> (ScriptNode)sn).collect(Collectors.toList());		
		
		BatchProcessor<Collection<ScriptNode>> processor;
		final Scriptable batchScope = this.scope;

		processor = new BatchProcessor<>(batchName, transactionService.getRetryingTransactionHelper(),
				new SimpleListWorkProvider(nodes, batchSize), workerThreads, FIXED_BATCH_SIZE, null, null, FIXED_BATCH_SIZE);

        	processor.process(new ScriptedBatchProcessWorker(runAsSystem, batchScope, processorFunction, nodes,
                                beforeProcessFunction, afterProcessFunction, Context.getCurrentContext(), serviceRegistry, scriptService), true);

	}
        
	/**
	 * process the given processorfunction on a set of nodes which are given as
	 * native array. The processing takes place in a de.jgoldhammer.alfresco.jscript.batch processor with the
	 * given batchName, the number of workerthreads and the number of nodes as
	 * batchsize.
	 *
	 * @param batchName
	 *            the name of the de.jgoldhammer.alfresco.jscript.batch
	 * @param workerThreads
	 *            the number of threads which can be used for the de.jgoldhammer.alfresco.jscript.batch
	 *            processing
	 * @param batchSize
	 * 			size of the batch
	 * @param nodeRefsAsStrings
	 *            the array of NodeRef strings to process (if you have your own
	 *            logic to determine the nodes)
	 * @param processorFunction
	 *            the javascript function to process- the function must have the
	 *            named "process"
	 *
	 *            Example: de.jgoldhammer.alfresco.jscript.batch.runForNodes('MyProcessor', 4, 10, nodes,
	 *            function process(node){ logger.error(node); }, true);
	 * @param runAsSystem
	 *            true if the processing should be run as system, false to
	 *            process as the current user
	 * @param beforeProcessFunction
	 * 				the function to run before the processing
	 * @param afterProcessFunction
	 * 				the function to run after the processing
	 *
	 */
	@ScriptMethod(code = "de.jgoldhammer.alfresco.jscript.batch.run('MyProcessor',4,10,'TEXT:alfresco',function process(node){logger.error(node);}, true);", help = "", output = "nothing", type = ScriptMethodType.WRITE)
	public void runForNodeRefs(String batchName, int workerThreads, final int batchSize, final NativeArray nodeRefsAsStrings,
			final String processorFunction, final boolean runAsSystem, final String beforeProcessFunction,
			final String afterProcessFunction) {

		BatchProcessor<Collection<ScriptNode>> processor;
		final Scriptable batchScope = this.scope;
		List<NodeRef> nodeRefs = convertNodeRefsAsStringsArray(nodeRefsAsStrings);
		
		List<ScriptNode> nodes = new ArrayList<>();
		nodeRefs.forEach(nodeRef -> { 
			nodes.add(new ScriptNode(nodeRef, serviceRegistry));
		});

		processor = new BatchProcessor<>(batchName, transactionService.getRetryingTransactionHelper(),
				new SimpleListWorkProvider(nodes, batchSize), workerThreads, FIXED_BATCH_SIZE, null, null, FIXED_BATCH_SIZE);

        	processor.process(new ScriptedBatchProcessWorker(runAsSystem, batchScope, processorFunction, nodes,
                                beforeProcessFunction, afterProcessFunction, Context.getCurrentContext(), serviceRegistry, scriptService), true);

	}
        
	/**
	 * Process the given processorfunction on a set of nodes in which are given as
	 * native array in a background Thread. The processing takes place in a de.jgoldhammer.alfresco.jscript.batch processor with the
	 * given batchName, the number of workerthreads and the number of nodes as
	 * batchsize.
	 *
	 * @param batchName
	 *            the name of the de.jgoldhammer.alfresco.jscript.batch
	 * @param workerThreads
	 *            the number of threads which can be used for the de.jgoldhammer.alfresco.jscript.batch
	 *            processing
	 * @param batchSize
	 * 			size of the batch
	 * @param scriptNodes
	 *            the array of scriptnodes to process (if you have your own
	 *            logic to determine the nodes)
	 * @param processorFunction
	 *            the javascript function to process- the function must have the
	 *            named "process"
	 *
	 *            Example: de.jgoldhammer.alfresco.jscript.batch.runForNodes('MyProcessor', 4, 10, nodes,
	 *            function process(node){ logger.error(node); }, true);
	 * @param runAsSystem
	 *            true if the processing should be run as system, false to
	 *            process as the current user
	 * @param beforeProcessFunction
	 * 				the function to run before the processing
	 * @param afterProcessFunction
	 * 				the function to run after the processing
	 *
	 */
	@ScriptMethod(code = "de.jgoldhammer.alfresco.jscript.batch.run('MyProcessor',4,10,'TEXT:alfresco',function process(node){logger.error(node);}, true);", help = "", output = "nothing", type = ScriptMethodType.WRITE)
	public void runForNodesInThread(String batchName, int workerThreads, final int batchSize, final NativeArray scriptNodes,
			final String processorFunction, final boolean runAsSystem, final String beforeProcessFunction,
			final String afterProcessFunction) {

		BatchProcessor<Collection<ScriptNode>> processor;
		final Scriptable batchScope = this.scope;
		//List<NodeRef> nodeRefs = convertScriptNodesArray(scriptNodes);
		
		List<ScriptNode> nodes = new ArrayList<>();
		scriptNodes.forEach(sn -> { 
			nodes.add((ScriptNode)sn);
		});

		processor = new BatchProcessor<>(batchName, transactionService.getRetryingTransactionHelper(),
				new SimpleListWorkProvider(nodes, batchSize), workerThreads, FIXED_BATCH_SIZE, null, null, FIXED_BATCH_SIZE);

                final ScriptedBatchProcessWorker scriptedBatchProcessWorker = new ScriptedBatchProcessWorker(
                    runAsSystem,
                    batchScope,
                    processorFunction,
                    nodes,
                    beforeProcessFunction,
                    afterProcessFunction,
                    Context.getCurrentContext(),
                    serviceRegistry,
                    scriptService);
                
                Runner p = new Runner(processor, scriptedBatchProcessWorker);
                new Thread(p).start();
	}
        
        private static class Runner implements Runnable {
            
            private final BatchProcessor<Collection<ScriptNode>> processor;
            private final ScriptedBatchProcessWorker scriptedBatchProcessWorker;

            private Runner(BatchProcessor<Collection<ScriptNode>> processor, ScriptedBatchProcessWorker scriptedBatchProcessWorker) {
                this.processor = processor;
                this.scriptedBatchProcessWorker = scriptedBatchProcessWorker;
            }

            @Override
            public void run() {
                processor.process(scriptedBatchProcessWorker, true);
            }
        }

	/**
	 * converts the native array of scriptnodes to a list of noderefs
	 *
	 * @param scriptNodes
	 * @return list of noderefs
	 */
	private List<NodeRef> convertScriptNodesArray(NativeArray scriptNodes) {
		List<NodeRef> nodes = new ArrayList<>();
		for (Object id : scriptNodes.getIds()) {
			int index = (Integer) id;
                        Object obj = scriptNodes.get(index);
                        if (obj instanceof NativeJavaObject) {
                            obj = ((NativeJavaObject) obj).unwrap();
                        }
			ScriptNode scriptNode = (ScriptNode) obj;
			nodes.add(scriptNode.getNodeRef());
		}
		return nodes;
	}
        
	/**
	 * converts the native array of NodeRef strings to a list of noderefs
	 *
	 * @param nodeRefsAsStrings
	 * @return list of noderefs
	 */
	private List<NodeRef> convertNodeRefsAsStringsArray(NativeArray nodeRefsAsStrings) {
		List<NodeRef> nodes = new ArrayList<>();
		for (Object id : nodeRefsAsStrings.getIds()) {
			int index = (Integer) id;
                        Object obj = nodeRefsAsStrings.get(index);
                        if (obj instanceof NativeJavaObject) {
                            obj = ((NativeJavaObject) obj).unwrap();
                        }
			String nodeRefAsString = (String) obj;
			nodes.add(new NodeRef(nodeRefAsString));
		}
		return nodes;
	}

	@Override
	public void setScope(Scriptable scope) {
		this.scope = scope;

	}

	private ServiceRegistry serviceRegistry;

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

}
