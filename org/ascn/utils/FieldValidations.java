package org.ascn.utils;

import sailpoint.api.SailPointContext;
import sailpoint.api.SailPointFactory;
import sailpoint.tools.GeneralException;

public class FieldValidations {
	
	private SailPointContext context;
	
	public FieldValidations() throws GeneralException {
		this.context = SailPointFactory.getCurrentContext();
	}

}
