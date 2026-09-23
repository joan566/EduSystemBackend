package com.edusistem.core.shared.infrastructure.adapter;

import jakarta.persistence.EntityManager;

/** Utilidad para consultas de existencia en SQL nativo. */
public final class SqlSupport {

    private SqlSupport() {
    }

    public static boolean exists(EntityManager em, String selectOneSql, Object... namedParamPairs) {
        var query = em.createNativeQuery(selectOneSql + " limit 1");
        for (int i = 0; i < namedParamPairs.length; i += 2) {
            query.setParameter((String) namedParamPairs[i], namedParamPairs[i + 1]);
        }
        return !query.getResultList().isEmpty();
    }
}
