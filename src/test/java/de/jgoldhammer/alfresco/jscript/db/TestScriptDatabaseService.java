package de.jgoldhammer.alfresco.jscript.db;


import com.tradeshift.test.remote.Remote;
import com.tradeshift.test.remote.RemoteTestRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * A simple class demonstrating how to run out-of-container tests 
 * loading Alfresco application context.
 * 
 * This class uses the RemoteTestRunner to try and connect to 
 * localhost:4578 and send the test name and method to be executed on 
 * a running Alfresco. One or more hostnames can be configured in the @Remote
 * annotation.
 * 
 * If there is no available remote server to run the test, it falls 
 * back on local running of JUnits.
 * 
 * For proper functioning the test class file must match exactly 
 * the one deployed in the webapp (either via JRebel or static deployment)
 * otherwise "incompatible magic value XXXXX" class error loading issues will arise.  
 * 
 * @author Gabriele Columbro 
 * @author Maurizio Pillitu
 *
 */

@RunWith(RemoteTestRunner.class)
@Remote(runnerClass=SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:alfresco/application-context.xml")
public class TestScriptDatabaseService {
    
    static Logger log = LoggerFactory.getLogger(TestScriptDatabaseService.class);

    @Autowired
    protected ScriptDatabaseService scriptDatabaseService;

	@Test
    public void testQueryWithAlfNode() {
    	Map<String,Object>[] query = scriptDatabaseService.query("dataSource", "Select * from alf_node where id=?", 2);
    	System.out.println(query[0].entrySet());
    	assertNotNull(query);
    	Assert.assertTrue(query.length==1);
    	Assert.assertNotNull(query[0].entrySet());
    	Assert.assertEquals(2L, query[0].get("ID"));
    }

	@Test
	public void testUpdateWithAlfNode() {
		int numberOfRowsAffected = scriptDatabaseService.update("dataSource", "Update alf_node set id=? where id=?", 2,2);
		Assert.assertTrue(numberOfRowsAffected==1);

		numberOfRowsAffected = scriptDatabaseService.update("dataSource", "Update alf_node set id=? where id=?", 3,21232323);
		Assert.assertTrue(numberOfRowsAffected==0);

	}
    
}
