package org.springframework.cloud.openfeign.cloud;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.Nullable;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Special ObjectProvider that allows the actual ObjectProvider to be resolved
 * later because of the creation of the named child context.
 * @param <T>
 */
class ClientFactoryObjectProvider<T> implements ObjectProvider<T> {

    private final NamedContextFactory clientFactory;
    private final String name;
    private final Class<T> type;
    private ObjectProvider<T> provider;

    public ClientFactoryObjectProvider(NamedContextFactory clientFactory, String name, Class<T> type) {
        this.clientFactory = clientFactory;
        this.name = name;
        this.type = type;
    }

    @Override
    public T getObject(Object... args) throws BeansException {
        return delegate().getObject(args);
    }

    @Override
    @Nullable
    public T getIfAvailable() throws BeansException {
        return delegate().getIfAvailable();
    }

    @Override
    public T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException {
        return delegate().getIfAvailable(defaultSupplier);
    }

    @Override
    public void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
        delegate().ifAvailable(dependencyConsumer);
    }

    @Override
    @Nullable
    public T getIfUnique() throws BeansException {
        return delegate().getIfUnique();
    }

    @Override
    public T getIfUnique(Supplier<T> defaultSupplier) throws BeansException {
        return delegate().getIfUnique(defaultSupplier);
    }

    @Override
    public void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
        delegate().ifUnique(dependencyConsumer);
    }

    @Override
    public Iterator<T> iterator() {
        return delegate().iterator();
    }

    @Override
    public Stream<T> stream() {
        return delegate().stream();
    }

    @Override
    public T getObject() throws BeansException {
        return delegate().getObject();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        delegate().forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return delegate().spliterator();
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<T> delegate() {
        if (this.provider == null) {
            provider = this.clientFactory.getProvider(name, type);
        }
        return provider;
    }
}

