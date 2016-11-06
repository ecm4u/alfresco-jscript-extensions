package de.jgoldhammer.alfresco.jscript;

import com.tradeshift.test.remote.Remote;
import com.tradeshift.test.remote.RemoteTestRunner;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.ScriptService;
import org.alfresco.util.test.junitrules.ApplicationContextInit;
import org.alfresco.util.test.junitrules.TemporaryNodes;
import org.alfresco.util.test.junitrules.TemporarySites;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * base alfresco test class which can be reused in unit tests.
 *
 * Created by jgoldhammer on 16.09.16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:alfresco/application-context.xml")
@Ignore("No test")
public class BaseAlfrescoTest {

	@ClassRule
	public static ApplicationContextInit APP_CONTEXT_INIT = new ApplicationContextInit();

	@Rule
	public TemporaryNodes testNodes = new TemporaryNodes(APP_CONTEXT_INIT);

	@Rule
	public TemporarySites temporarySites = new TemporarySites(APP_CONTEXT_INIT);

	protected RetryingTransactionHelper transactionHelper =
			APP_CONTEXT_INIT.getApplicationContext().getBean("retryingTransactionHelper", RetryingTransactionHelper.class);


	@Autowired
	Repository repo;

	@Autowired
	ServiceRegistry serviceRegistry;

	@Autowired
	protected FileFolderService fileFolderService;

	@Autowired
	protected DictionaryService dictionaryService;

	@Autowired
	protected NodeService nodeService;

	@Autowired
	protected ScriptService scriptService;

	@Autowired
	@Qualifier("policyBehaviourFilter")
	protected BehaviourFilter behaviourFilter;

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	protected NodeRef generateTempDocument(){
		return testNodes.createNode(repo.getCompanyHome(),"Junit-"+System.nanoTime(),
				ContentModel.TYPE_CONTENT, AuthenticationUtil.getSystemUserName());
	}


	protected  NodeRef generateTempFolder(){
		return testNodes.createFolder(getCompanyHome(), this.getClass().getSimpleName() + System.nanoTime(), AuthenticationUtil.getFullyAuthenticatedUser());

	}

	protected  NodeRef getCompanyHome(){
		return repo.getCompanyHome();
	}


}
