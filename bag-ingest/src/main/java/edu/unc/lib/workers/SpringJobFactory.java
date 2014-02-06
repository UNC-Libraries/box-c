package edu.unc.lib.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.worker.JobFactory;

public class SpringJobFactory implements JobFactory, ApplicationContextAware {
	private Logger log = LoggerFactory.getLogger(SpringWorker.class);

	private ApplicationContext applicationContext;

	public SpringJobFactory() {
	}

	@Override
	public Object materializeJob(Job job) throws ClassNotFoundException {
		Runnable runnableJob = null;
		if (applicationContext.containsBeanDefinition(job.getClassName())) {
			runnableJob = (Runnable) applicationContext.getBean(
					job.getClassName(), job.getArgs());
		} else {
			Class clazz = Class.forName(job.getClassName());// Lookup by Class
															// type
			String[] beanNames = applicationContext.getBeanNamesForType(clazz,
					true, false);
			if (applicationContext.containsBeanDefinition(job.getClassName())) {
				runnableJob = (Runnable) applicationContext.getBean(
						beanNames[0], job.getArgs());
			} else {
				if (beanNames != null && beanNames.length == 1) {
					runnableJob = (Runnable) applicationContext.getBean(
							beanNames[0], job.getArgs());
				}
			}
		}
		if(runnableJob == null) {
			throw new ClassNotFoundException("Neither bean ID nor class definition found for " + job.getClassName());
		} else {
			return runnableJob;
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

}
