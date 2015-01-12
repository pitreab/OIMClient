package  com.oracle.poc.impl;

import java.util.HashMap;

import oracle.iam.platform.kernel.spi.PostProcessHandler;
import oracle.iam.platform.kernel.vo.AbstractGenericOrchestration;
import oracle.iam.platform.kernel.vo.BulkEventResult;
import oracle.iam.platform.kernel.vo.BulkOrchestration;
import oracle.iam.platform.kernel.vo.EventResult;
import oracle.iam.platform.kernel.vo.Orchestration;

public class ModifyUserHander implements PostProcessHandler {

	@Override
	public void initialize(HashMap<String, String> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean cancel(long arg0, long arg1,
			AbstractGenericOrchestration arg2) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void compensate(long arg0, long arg1,
			AbstractGenericOrchestration arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public EventResult execute(long processId, long eventId, Orchestration orchestration) {
		// TODO Auto-generated method stub
		String userKey = null;
		try {
			userKey = getUserKey(processId, orchestration);
			System.out.println("ModifyUserHander.execute():userKey: "+userKey);
		}catch(Exception exp){
			
		}
		return null;
	}

	@Override
	public BulkEventResult execute(long arg0, long arg1, BulkOrchestration arg2) {
		// TODO Auto-generated method stub
		
		return null;
	}
	
	private String getUserKey(long processId, Orchestration orchestration) {
		String userKey = null;
		userKey = orchestration.getTarget().getEntityId();
		return userKey;
	}

}
