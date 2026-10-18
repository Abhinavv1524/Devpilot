package com.devpilot.orchestrator.history;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskHistoryRepository extends JpaRepository<TaskHistoryEntity, String> {

    List<TaskHistoryEntity> findTop20ByOwnerUsernameOrderByCreatedAtDesc(String ownerUsername);

    java.util.Optional<TaskHistoryEntity> findByTaskIdAndOwnerUsername(
            String taskId, String ownerUsername);

    @Query("""
            select t from TaskHistoryEntity t
            where t.ownerUsername = :ownerUsername
              and (lower(t.userPrompt) like lower(concat('%', :term, '%'))
               or lower(coalesce(t.researchSummary, '')) like lower(concat('%', :term, '%'))
               or lower(coalesce(t.codingSummary, '')) like lower(concat('%', :term, '%'))
               or lower(coalesce(t.testingSummary, '')) like lower(concat('%', :term, '%')))
            order by t.createdAt desc
            """)
    List<TaskHistoryEntity> searchByTerm(
            @Param("ownerUsername") String ownerUsername,
            @Param("term") String term);
}
