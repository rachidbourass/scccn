package org.ascn.utils;

import java.util.Date;

import sailpoint.api.RequestManager;
import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.object.Attributes;
import sailpoint.object.Identity;
import sailpoint.object.Request;
import sailpoint.object.RequestDefinition;
import sailpoint.object.Workflow;
import sailpoint.tools.GeneralException;
import sailpoint.workflow.StandardWorkflowHandler;

public class ScheduledWorkflowRule {

    private static SailPointContext context;

    public static void scheduleWorkflow(String wfName, String caseName, long startTime, Attributes < String, String > wfArgs, String requesterId) throws GeneralException {
    	context = SailPointFactory.getCurrentContext();
    	if (null == caseName || "".equals(caseName.trim())) {
            caseName = "Run " + wfName;
        }
        try {
            Workflow eventWorkflow = context.getObject(Workflow.class, wfName);
            if (null == eventWorkflow) {
                throw new GeneralException("Invalid worklfow: " + wfName);
            }

            Identity id = context.getObjectByName(Identity.class, requesterId);
            if (null == id) {
                throw new GeneralException("Invalid identity: " + requesterId);
            }

            // Ask the Request Processor to start the workflow 5 seconds from now.
            // Append the time stamp to the workflow case name to ensure it's unique. 
            long launchTime = startTime;
            // Build out a map of arguments to pass to the Request Scheduler.
            Attributes<String,Object> reqArgs = new Attributes<>();
            reqArgs.put(StandardWorkflowHandler.ARG_REQUEST_DEFINITION, sailpoint.request.WorkflowRequestExecutor.DEFINITION_NAME);
            reqArgs.put(sailpoint.workflow.StandardWorkflowHandler.ARG_WORKFLOW, wfName);
            reqArgs.put(sailpoint.workflow.StandardWorkflowHandler.ARG_REQUEST_NAME, caseName);
            reqArgs.put("requestName", caseName);

            wfArgs.put("workflow", eventWorkflow.getName());

            reqArgs.putAll(wfArgs);

            // Use the Request Launcher to schedule the workflow reqeust.  This requires
            // a Request object to store the properties of the request item.
            Request req = new Request();
            RequestDefinition reqdef = null;

            reqdef = context.getObject(RequestDefinition.class, "Workflow Request");

            req.setDefinition(reqdef);
            req.setEventDate(new Date(launchTime));
            req.setOwner(id);
            req.setName(caseName);
            req.setAttributes(reqdef, reqArgs);

            // Schedule the work flow via the request manager.

            RequestManager.addRequest(context, req);
        } catch (GeneralException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}