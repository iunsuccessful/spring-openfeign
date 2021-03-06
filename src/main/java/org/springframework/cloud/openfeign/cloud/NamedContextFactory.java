package org.springframework.cloud.openfeign.cloud;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.MapPropertySource;

/**
 * Creates a set of child contexts that allows a set of Specifications to define the beans
 * in each child context.
 *
 * Ported from spring-cloud-netflix FeignClientFactory and SpringClientFactory
 *
 * @author Spencer Gibb
 * @author Dave Syer
 */
//TODO: add javadoc
public abstract class NamedContextFactory<C extends NamedContextFactory.Specification>
        implements DisposableBean, ApplicationContextAware {
    
    private static final Logger logger = LoggerFactory.getLogger(NamedContextFactory.class);

    public interface Specification {
        String getName();

        Class<?>[] getConfiguration();
    }

    private Map<String, AnnotationConfigApplicationContext> contexts = new ConcurrentHashMap<>();

    private Map<String, C> configurations = new ConcurrentHashMap<>();

    private ApplicationContext parent;

    private Class<?> defaultConfigType;
    private final String propertySourceName;
    private final String propertyName;

    public NamedContextFactory(Class<?> defaultConfigType, String propertySourceName,
                               String propertyName) {
        this.defaultConfigType = defaultConfigType;
        this.propertySourceName = propertySourceName;
        this.propertyName = propertyName;
    }

    @Override
    public void setApplicationContext(ApplicationContext parent) throws BeansException {
        this.parent = parent;
    }

    public void setConfigurations(List<C> configurations) {
        for (C client : configurations) {
            this.configurations.put(client.getName(), client);
        }
    }

    public Set<String> getContextNames() {
        return new HashSet<>(contexts.keySet());
    }

    @Override
    public void destroy() {
        Collection<AnnotationConfigApplicationContext> values = this.contexts.values();
        for (AnnotationConfigApplicationContext context : values) {
            // This can fail, but it never throws an exception (you see stack traces
            // logged as WARN).
            context.close();
        }
        this.contexts.clear();
    }

    protected AnnotationConfigApplicationContext getContext(String name) {
        if (!this.contexts.containsKey(name)) {
            synchronized (this.contexts) {
                if (!this.contexts.containsKey(name)) {
                    this.contexts.put(name, createContext(name));
                }
            }
        }
        return this.contexts.get(name);
    }

    protected AnnotationConfigApplicationContext createContext(String name) {
        logger.warn("first: {}", name);
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        if (this.configurations.containsKey(name)) {
            for (Class<?> configuration : this.configurations.get(name)
                    .getConfiguration()) {
                context.register(configuration);
            }
        }
        for (Map.Entry<String, C> entry : this.configurations.entrySet()) {
            if (entry.getKey().startsWith("default.")) {
                for (Class<?> configuration : entry.getValue().getConfiguration()) {
                    context.register(configuration);
                }
            }
        }
        context.register(PropertyPlaceholderAutoConfiguration.class,
                this.defaultConfigType);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                this.propertySourceName,
                Collections.<String, Object> singletonMap(this.propertyName, name)));
        if (this.parent != null) {
            // Uses Environment from parent as well as beans
            context.setParent(this.parent);
        }
        context.setDisplayName(generateDisplayName(name));
        context.refresh();
        return context;
    }

    protected String generateDisplayName(String name) {
        return this.getClass().getSimpleName() + "-" + name;
    }

    public <T> T getInstance(String name, Class<T> type) {
        AnnotationConfigApplicationContext context = getContext(name);
        if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context,
                type).length > 0) {
            return context.getBean(type);
        }
        return null;
    }

    public <T> ObjectProvider<T> getLazyProvider(String name, Class<T> type) {
        return new ClientFactoryObjectProvider<>(this, name, type);
    }

    public <T> ObjectProvider<T> getProvider(String name, Class<T> type) {
        AnnotationConfigApplicationContext context = getContext(name);
        return context.getBeanProvider(type);
    }

    public <T> T getInstance(String name, Class<?> clazz, Class<?>... generics) {
        ResolvableType type = ResolvableType.forClassWithGenerics(clazz, generics);
        return getInstance(name, type);
    }

    @SuppressWarnings("unchecked")
    public <T> T getInstance(String name, ResolvableType type) {
        AnnotationConfigApplicationContext context = getContext(name);
        String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context,
                type);
        if (beanNames.length > 0) {
            for (String beanName : beanNames) {
                if (context.isTypeMatch(beanName, type)) {
                    return (T) context.getBean(beanName);
                }
            }
        }
        return null;
    }

    public <T> Map<String, T> getInstances(String name, Class<T> type) {
        AnnotationConfigApplicationContext context = getContext(name);
        if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context,
                type).length > 0) {
            return BeanFactoryUtils.beansOfTypeIncludingAncestors(context, type);
        }
        return null;
    }

}

