package data.controller;

import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties({})
public record BatchJobsWrapper(
      @Qualifier("defaultBatchJob") Job defaultBatchJob,
      @Qualifier("oaiHarvestBatchJob") Job oaiHarvestBatchJob,
      @Qualifier("validationBatchJob") Job validationBatchJob,
      @Qualifier("transformationBatchJob") Job tranformationBatchJob,
      @Qualifier("normalizationBatchJob") Job normalizationBatchJob,
      @Qualifier("enrichmentBatchJob") Job enrichmentBatchJob,
      @Qualifier("mediaBatchJob") Job mediaBatchJob
) {}