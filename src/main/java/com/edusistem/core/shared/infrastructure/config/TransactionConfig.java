package com.edusistem.core.shared.infrastructure.config;

import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.TransactionAnnotationParser;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Traduce la anotación de application {@link UseCaseTransactional} a transacciones de Spring, para que la capa
 * application no dependa del framework. El source y el interceptor no se exponen como beans para no chocar con los
 * de {@code @Transactional}, que sigue usándose en los adapters de infrastructure.
 */
@Configuration
public class TransactionConfig {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static Advisor useCaseTransactionAdvisor(BeanFactory beanFactory) {
        AnnotationTransactionAttributeSource source =
                new AnnotationTransactionAttributeSource(new UseCaseTransactionalAnnotationParser());
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionAttributeSource(source);
        interceptor.setBeanFactory(beanFactory);

        BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
        advisor.setTransactionAttributeSource(source);
        advisor.setAdvice(interceptor);
        return advisor;
    }

    /** Propagación REQUIRED y rollback ante RuntimeException/Error, salvo las excepciones de noRollbackFor. */
    static final class UseCaseTransactionalAnnotationParser implements TransactionAnnotationParser {

        @Override
        public boolean isCandidateClass(Class<?> targetClass) {
            return AnnotationUtils.isCandidateClass(targetClass, UseCaseTransactional.class);
        }

        @Override
        public TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
            AnnotationAttributes attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(
                    element, UseCaseTransactional.class, false, false);
            if (attributes == null) {
                return null;
            }
            List<RollbackRuleAttribute> rules = new ArrayList<>();
            for (Class<?> type : attributes.getClassArray("noRollbackFor")) {
                rules.add(new NoRollbackRuleAttribute(type));
            }
            RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
            attribute.setRollbackRules(rules);
            return attribute;
        }
    }
}
