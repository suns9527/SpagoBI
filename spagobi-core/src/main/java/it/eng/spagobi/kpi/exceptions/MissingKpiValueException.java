/* SpagoBI, the Open Source Business Intelligence suite

 * Copyright (C) 2012 Engineering Ingegneria Informatica S.p.A. - SpagoBI Competency Center
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0, without the "Incompatible With Secondary Licenses" notice. 
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package it.eng.spagobi.kpi.exceptions;

import it.eng.spagobi.tools.dataset.exceptions.DatasetException;

public class MissingKpiValueException extends DatasetException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String USER_MESSAGE = "Missing 'value' column in query: ";

	/**
	 * Builds a <code>SpagoBIRuntimeException</code>.
	 * 
	 * @param message Text of the exception
	 */
    public MissingKpiValueException(String message) {
    	super(message);  	
    }
	
    /**
     * Builds a <code>SpagoBIRuntimeException</code>.
     * 
     * @param message Text of the exception
     * @param ex previous Throwable object
     */
    public MissingKpiValueException(String message, Throwable ex) {
    	super(message, ex);
    }
}
