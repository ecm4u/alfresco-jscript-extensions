package de.jgoldhammer.alfresco.jscript.batchexecuter;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Scriptable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
 import org.springframework.extensions.webscripts.annotation.ScriptClass;
 import org.springframework.extensions.webscripts.annotation.ScriptClassType;

import de.jgoldhammer.alfresco.jscript.batchexecuter.WorkProviders.CancellableWorkProvider;
import de.jgoldhammer.alfresco.jscript.batchexecuter.WorkProviders.CollectionWorkProviderFactory;
import de.jgoldhammer.alfresco.jscript.batchexecuter.WorkProviders.FolderBrowsingWorkProviderFactory;
import de.jgoldhammer.alfresco.jscript.batchexecuter.WorkProviders.NodeOrBatchWorkProviderFactory;
import de.jgoldhammer.alfresco.jscript.batchexecuter.Workers.CancellableWorker;
import de.jgoldhammer.alfresco.jscript.batchexecuter.Workers.ProcessBatchWorker;
import de.jgoldhammer.alfresco.jscript.batchexecuter.Workers.ProcessNodeWorker;

/**
 * JavaScript object which helps execute big data changes in Alfresco.
 *
 * The object allows providing a set of nodes to process and a function to run,
 * then splits nodes in batches and executes each batch in a separate transaction
 * and multiple threads.
 *
 * @author Bulat Yaminov
 * @author Corentin Roux
 */
@ScriptClass(types= ScriptClassType.JavaScriptRootObject, code="batchExecuter",
        help="the root object for the batch interface")
public class ScriptBatchExecuter extends BaseScopableProcessorExtension implements ApplicationContextAware {

    private static final Log logger = LogFactory.getLog(ScriptBatchExecuter.class);

    private ServiceRegistry sr;
    private ApplicationContext applicationContext;

    private static ConcurrentHashMap<String, BatchJobParameters> runningJobs = new ConcurrentHashMap(10);
    private static ConcurrentHashMap<String, Pair<CancellableWorkProvider, CancellableWorker>>
            runningWorkProviders = new ConcurrentHashMap(10);

    /**
     * Starts processing an array of objects, applying a function to each object or batch of objects
     * within the array.
     *
     * This is a blocking call.
     *
     * @param params processing params, with array stored as 'items' property. See
     * {@link src.main.java.de.jgoldhammer.alfresco.repo.jscript.batchexecuter.batchexecuter.BatchJobParameters} for all parameters.
     * @return job ID.
     */
    public String processArray(Object params) {
        BatchJobParameters.ProcessArrayJobParameters job = BatchJobParameters.parseArrayParameters(params);
        return doProcess(job, CollectionWorkProviderFactory.getInstance(), job.getItems());
    }

    /**
     * Starts processing a folder and its children recursively, applying a function to each
     * node or batch of nodes. Both folders and documents are included.
     *
     * This is a blocking call.
     *
     * @param params processing params, with the folder ScriptNode stored as 'root' property. See
     * {@link src.main.java.de.jgoldhammer.alfresco.repo.jscript.batchexecuter.batchexecuter.BatchJobParameters} for all parameters.
     * @return job ID.
     */
    public String processFolderRecursively(Object params) {
        BatchJobParameters.ProcessFolderJobParameters job = BatchJobParameters.parseFolderParameters(params);
        return doProcess(job,
                new FolderBrowsingWorkProviderFactory(sr, getScope(), logger),
                job.getRoot().getNodeRef());
    }

    /**
     * Get the list of currently executing jobs.
     *
     * @return collection of jobs being executed.
     */
    public Collection<BatchJobParameters> getCurrentJobs() {
        return runningJobs.values();
    }

    /**
     * Cancels a job by given job ID. Any batches being already fed to the processor
     * will be finished, but no new batches will be started.
     *
     * @param jobId job ID
     * @return true if job existed by given ID and was cancelled.
     * False if job was already finished or never existed.
     */
    public synchronized boolean cancelJob(String jobId) {
        if (jobId == null) {
            return false;
        }

        // We don't have access to BatchProcessor's executer service,
        // so the only way to cancel is stop giving new work packages
        BatchJobParameters job = runningJobs.get(jobId);
        Pair<CancellableWorkProvider, CancellableWorker> pair = runningWorkProviders.get(jobId);
        if (pair != null) {
            boolean workProviderCanceled = pair.getFirst().cancel();
            boolean workerCanceled = pair.getSecond().cancel();
            boolean canceled = workProviderCanceled || workerCanceled; // either cancellation is a change
            if (canceled && job != null) {
                job.setStatus(BatchJobParameters.Status.CANCELED);
            }
            return canceled;
        }
        return false;
    }

    private <T> String doProcess(BatchJobParameters job,
                                 NodeOrBatchWorkProviderFactory<T> workFactory,
                                 T data) {
        try {
            /* Process items */
            runningJobs.put(job.getId(), job);

            final Scriptable cachedScope = getScope();
            final String user = AuthenticationUtil.getRunAsUser();

            RetryingTransactionHelper rth = sr.getTransactionService().getRetryingTransactionHelper();

            job.setStatus(BatchJobParameters.Status.RUNNING);

            if (job.getOnNode() != null) {

                // Let the BatchProcessor do the batching
                CancellableWorkProvider<Object> workProvider =
                        workFactory.newNodesWorkProvider(data, job.getBatchSize(), job.isBrowseSystemNodes());
                ProcessNodeWorker worker = new ProcessNodeWorker(job.getOnNode(), cachedScope,
                        user, job.getDisableRules(), sr.getRuleService(), logger, this);

                runningWorkProviders.put(job.getId(), new Pair<CancellableWorkProvider,
                        CancellableWorker>(workProvider, worker));

                BatchProcessor<Object> processor = new BatchProcessor(job.getName(), rth,
                        workProvider,
                        job.getThreads(), job.getBatchSize(), applicationContext, logger, 1000);
                logger.info(String.format("Starting batch processor '%s' to process %s",
                        job.getName(), workFactory.describe(data)));
                processor.process(worker, true);

            } else {

                // Split into batches here so that onBatch function can process them
                CancellableWorkProvider<List<Object>> workProvider =
                        workFactory.newBatchesWorkProvider(data, job.getBatchSize(), job.isBrowseSystemNodes());
                ProcessBatchWorker worker = new ProcessBatchWorker(job.getOnBatch(), cachedScope,
                        user, job.getDisableRules(), sr.getRuleService(), logger, this);

                runningWorkProviders.put(job.getId(), new Pair<CancellableWorkProvider,
                        CancellableWorker>(workProvider, worker));

                BatchProcessor<List<Object>> processor = new BatchProcessor(job.getName(), rth,
                        workProvider,
                        job.getThreads(), 1, applicationContext, logger, 1);
                logger.info(String.format("Starting batch processor '%s' to process %s with batch function",
                        job.getName(), workFactory.describe(data)));
                processor.process(worker, true);
            }

            if (job.getStatus() != BatchJobParameters.Status.CANCELED) {
                job.setStatus(BatchJobParameters.Status.FINISHED);
            }

            return job.getName();

        } finally {
            runningJobs.remove(job.getId());
            runningWorkProviders.remove(job.getId());
        }
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.sr = serviceRegistry;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
