package br.com.ambev.order_service.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BatchScheduler {

	 private final JobLauncher jobLauncher;
	    private final Job processOrdersJob;

	    public BatchScheduler(JobLauncher jobLauncher, Job processOrdersJob) {
	        this.jobLauncher = jobLauncher;
	        this.processOrdersJob = processOrdersJob;
	    }


	    
	    @Scheduled(fixedRate = 60000) 
	    public void scheduleJob() {
	        try {
	            log.info("Iniciando execução do job agendado.");
	            jobLauncher.run(processOrdersJob, new JobParametersBuilder()
	                    .addLong("time", System.currentTimeMillis())
	                    .toJobParameters());
	        } catch (Exception e) {
	            log.error("Erro ao executar o job: " + e.getMessage(), e);
	        }
	    }
}
