package com.bluenimble.platform.scheduler;

import java.io.Serializable;

import com.bluenimble.platform.Feature;
import com.bluenimble.platform.json.JsonObject;

@Feature (name = "scheduler")
public interface Scheduler extends Serializable {
	
	interface JobStatus {
		int All 	= 2;
		int Running = 1;
		int Paused 	= 0;
	}
	
	enum SchedulingResult {
		Unknown,
		Scheduled,
		Invalid,
		Expired
	}

	void 		start		() throws SchedulerException;
	void 		stop		() throws SchedulerException;
	
	SchedulingResult 	
				schedule 	(JsonObject job, boolean save) throws SchedulerException;
	void 		unschedule 	(String id, boolean save) throws SchedulerException;
	
	void 		pause		(String id, boolean save) throws SchedulerException;
	void 		resume		(String id, boolean save) throws SchedulerException;
	
	JsonObject 	get			(String id) throws SchedulerException;
	JsonObject 	list		(int offset, int count, int status, JsonObject metaQuery) throws SchedulerException;
	
	
}
