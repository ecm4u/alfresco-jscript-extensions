/**
 *
 */
package de.jgoldhammer.alfresco.jscript.authentication;

import org.alfresco.repo.processor.BaseProcessorExtension;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.security.AuthorityService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.extensions.webscripts.annotation.ScriptClass;
import org.springframework.extensions.webscripts.annotation.ScriptClassType;
import org.springframework.extensions.webscripts.annotation.ScriptMethod;
import org.springframework.extensions.webscripts.annotation.ScriptMethodType;

/**
 * script object for handling the behaviourFilter
 *
 * @author Jens Goldhammer (fme AG)
 */

 /**
  * example usage:
  *
  * auth.runAs("user-a");
  * auth.runAsFullyAuthenticatedUser("user-b");
  * 
  * auth.runAsSystem();
  * print("runAs: "+auth.getRunAsUser());
  * print("FullyAuthenticatedUser: "+auth.getFullyAuthenticatedUser());
  *
  */

@ScriptClass(types=ScriptClassType.JavaScriptRootObject, code="auth", help="the root object for the de.jgoldhammer.alfresco.jscript.authentication util to switch the authenticated user ")
public class ScriptAuthentication extends BaseProcessorExtension implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@ScriptMethod(
    		help="Set the system user as the currently running user for authentication purposes.",
    		output="void",
    		code="auth.runAsSystem()",
    		type=ScriptMethodType.READ)
	public void runAsSystem() {
		AuthorityService authorityService = (AuthorityService) applicationContext.getBean("authorityService");
		if (!authorityService.isAdminAuthority(AuthenticationUtil.getFullyAuthenticatedUser())) {
			throw new RuntimeException("you are not allowed to runAs method");
		}
		AuthenticationUtil.setRunAsUserSystem();
	}

    @ScriptMethod(
    		help="Switch to the given user for all authenticated operations.  The original, authenticated user can still be found using auth.getFullyAuthenticatedUser()",
    		output="void",
    		code="auth.runAs('user1');",
    		type=ScriptMethodType.READ)
    public void runAs(String userName){
		AuthorityService authorityService = (AuthorityService) applicationContext.getBean("authorityService");
		if (!authorityService.isAdminAuthority(AuthenticationUtil.getFullyAuthenticatedUser())) {
			throw new RuntimeException("you are not allowed to runAs method");
		}
    	AuthenticationUtil.setRunAsUser(userName) ;
    }

    @ScriptMethod(
    		help="Get the user that is currently in effect for purposes of de.jgoldhammer.alfresco.jscript.authentication.  This includes any overlays introduced by auth.runAsUser(<username>).",
    		output="void",
    		code="auth.getRunAsUser()",
    		type=ScriptMethodType.READ)

    public String getRunAsUser(){
    	return AuthenticationUtil.getRunAsUser();
    }

    @ScriptMethod(
    		help="Get the fully authenticated user. It returns the name of the user that last authenticated and excludes any overlay de.jgoldhammer.alfresco.jscript.authentication set",
    		output="void",
    		code="auth.getFullyAuthenticatedUser();",
    		type=ScriptMethodType.READ)

    public String getFullyAuthenticatedUser(){
    	return AuthenticationUtil.getFullyAuthenticatedUser();
    }

    @ScriptMethod(
    		help="Authenticate as the given user.  The user will be authenticated and all operations with be run in the context of this user.",
    		output="void",
    		code="auth.runAsFullyAuthenticatedUser('user1');",
    		type=ScriptMethodType.READ)

    public void runAsFullyAuthenticatedUser(String userName){
		AuthorityService authorityService = (AuthorityService) applicationContext.getBean("authorityService");
		if (!authorityService.isAdminAuthority(AuthenticationUtil.getFullyAuthenticatedUser())) {
			throw new RuntimeException("you are not allowed to runAs method");
		}

		AuthenticationUtil.setFullyAuthenticatedUser(userName);
    }


    @ScriptMethod(
    		help="Get the name of the system user",
    		output="String",
    		code="auth.getSystemUserName()",
    		type=ScriptMethodType.READ)

    public String getSystemUserName(){
    	return AuthenticationUtil.getSystemUserName();
    }

    @ScriptMethod(
    		help="get the name of the admin user",
    		output="String",
    		code="auth.getAdminUserName();",
    		type=ScriptMethodType.READ)

    public String getAdminUserName(){
    	return AuthenticationUtil.getAdminUserName();
    }

}
