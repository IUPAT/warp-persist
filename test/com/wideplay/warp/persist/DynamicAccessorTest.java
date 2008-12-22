package com.wideplay.warp.persist;

import com.google.inject.*;
import com.wideplay.codemonkey.web.startup.Initializer;
import com.wideplay.warp.hibernate.HibernatePersistenceStrategy;
import com.wideplay.warp.hibernate.HibernateTestEntity;
import com.wideplay.warp.persist.dao.Finder;
import org.hibernate.HibernateException;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.testng.annotations.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * @author Robbie Vanbrabant
 */
public class DynamicAccessorTest {
    @Test(expectedExceptions = CreationException.class)
    public void testDynamicAccessorWithoutFinderAnnotation() {
        Guice.createInjector(PersistenceService.usingHibernate().across(UnitOfWork.REQUEST)
                .addAccessor(InvalidHibernateTestAccessor.class)
                .buildModule());
    }

    @Test(expectedExceptions = CreationException.class)
    public void testDynamicAccessorWithoutFinderUnit() {
        HibernatePersistenceStrategy hibernate = HibernatePersistenceStrategy.builder().annotatedWith(MyUnit.class).build();
        Guice.createInjector(PersistenceService.using(hibernate).across(UnitOfWork.REQUEST)
                .addAccessor(InvalidHibernateTestAccessor.class)
                .buildModule());
    }

    @Test(expectedExceptions = HibernateException.class) // finder not valid without tx
    public void testDynamicAccessorWithoutTransaction() {
        HibernatePersistenceStrategy hibernate = HibernatePersistenceStrategy.builder().annotatedWith(MyUnit.class).build();
        Guice.createInjector(PersistenceService.using(hibernate).across(UnitOfWork.REQUEST)
                .addAccessor(ValidNonTransactionalAccessor.class)
                .buildModule(),
                new AbstractModule() {
                    protected void configure() {
                        bind(Configuration.class).annotatedWith(MyUnit.class).toInstance(new AnnotationConfiguration()
                            .addAnnotatedClass(HibernateTestEntity.class)
                            .setProperties(Initializer.loadProperties("spt-persistence.properties")));
                    }
                }).getInstance(Key.get(ValidNonTransactionalAccessor.class, MyUnit.class)).listAll();
    }

    @Test
    public void testDynamicAccessorWithTransaction() {
        HibernatePersistenceStrategy hibernate = HibernatePersistenceStrategy.builder().annotatedWith(MyUnit.class).build();
        Guice.createInjector(PersistenceService.using(hibernate).across(UnitOfWork.REQUEST)
                .addAccessor(ValidTransactionalAccessor.class)
                .buildModule(),
                new AbstractModule() {
                    protected void configure() {
                        bind(Configuration.class).annotatedWith(MyUnit.class).toInstance(new AnnotationConfiguration()
                            .addAnnotatedClass(HibernateTestEntity.class)
                            .setProperties(Initializer.loadProperties("spt-persistence.properties")));
                    }
                }).getInstance(Key.get(ValidTransactionalAccessor.class, MyUnit.class)).listAll();
    }

    @Test(expectedExceptions = CreationException.class)
    public void testDynamicAccessorWithTransactionInvalidInterface() {
        HibernatePersistenceStrategy hibernate = HibernatePersistenceStrategy.builder().annotatedWith(MyUnit.class).build();
        Guice.createInjector(PersistenceService.using(hibernate).across(UnitOfWork.REQUEST)
                .addAccessor(InvalidTransactionalAccessor.class)
                .buildModule(),
                new AbstractModule() {
                    protected void configure() {
                        bind(Configuration.class).annotatedWith(MyUnit.class).toInstance(new AnnotationConfiguration()
                            .addAnnotatedClass(HibernateTestEntity.class)
                            .setProperties(Initializer.loadProperties("spt-persistence.properties")));
                    }
                }).getInstance(Key.get(InvalidTransactionalAccessor.class, MyUnit.class)).listAll();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @BindingAnnotation
    @interface MyUnit {}

    public interface ValidTransactionalAccessor {
        @Finder(unit=MyUnit.class, query = "from HibernateTestEntity")
        @Transactional(unit=MyUnit.class)
        List<HibernateTestEntity> listAll();
    }

    public interface InvalidTransactionalAccessor {
        // Invalid when in multi-modules mode, needs unit=...
        @Finder(unit=MyUnit.class, query = "from HibernateTestEntity")
        @Transactional
        List<HibernateTestEntity> listAll();
    }

    public interface ValidNonTransactionalAccessor {
        // Invalid when in multi-modules mode, needs unit=...
        @Finder(unit=MyUnit.class, query = "from HibernateTestEntity")
        List<HibernateTestEntity> listAll();
    }

    public interface InvalidHibernateTestAccessor {
        // Invalid when in multi-modules mode, needs unit=...
        @Finder(query = "from HibernateTestEntity")
        List<HibernateTestEntity> listAll();

        // Invalid, has to have a @Finder annotation.
        List<HibernateTestEntity> listAll2();
    }

}