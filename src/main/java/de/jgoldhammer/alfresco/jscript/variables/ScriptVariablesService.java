package de.jgoldhammer.alfresco.jscript.variables;

import java.util.Properties;
import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.security.AuthorityService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Expose global properties to js scripts
 *
 */
public class ScriptVariablesService extends BaseScopableProcessorExtension implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

    private Properties properties;
    
    public void setProperties(final Properties globalProperties) {
		AuthorityService authorityService = (AuthorityService) applicationContext.getBean("authorityService");
		if (!authorityService.isAdminAuthority(AuthenticationUtil.getFullyAuthenticatedUser())) {
			throw new RuntimeException("you are not allowed to access properties!");
		}
        properties = globalProperties;
    }

    public Properties getProperties() {
		AuthorityService authorityService = (AuthorityService) applicationContext.getBean("authorityService");
		if (!authorityService.isAdminAuthority(AuthenticationUtil.getFullyAuthenticatedUser())) {
			throw new RuntimeException("you are not allowed to access properties!");
		}
        return properties;
    }

    public String get(String key) {
		AuthorityService authorityService = (AuthorityService) applicationContext.getBean("authorityService");
		if (!authorityService.isAdminAuthority(AuthenticationUtil.getFullyAuthenticatedUser())) {
			throw new RuntimeException("you are not allowed to access properties!");
		}
        return properties.getProperty(key);
    }
    
    public String get(String key,String otherwise) {
		AuthorityService authorityService = (AuthorityService) applicationContext.getBean("authorityService");
		if (!authorityService.isAdminAuthority(AuthenticationUtil.getFullyAuthenticatedUser())) {
			throw new RuntimeException("you are not allowed to access properties!");
		}
        return properties.getProperty(key,otherwise);
    }
}