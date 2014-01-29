/*
 * SpagoBI, the Open Source Business Intelligence suite
 * � 2005-2015 Engineering Group
 *
 * This file is part of SpagoBI. SpagoBI is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 2.1 of the License, or any later version. 
 * SpagoBI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You should have received
 * a copy of the GNU Lesser General Public License along with SpagoBI. If not, see: http://www.gnu.org/licenses/.
 * The complete text of SpagoBI license is included in the COPYING.LESSER file. 
 */
package it.eng.spagobi.engine.cockpit.services.editor;

import it.eng.spago.security.IEngUserProfile;
import it.eng.spagobi.engine.cockpit.CockpitEngineInstance;
import it.eng.spagobi.services.proxy.DataSetServiceProxy;
import it.eng.spagobi.tools.dataset.bo.IDataSet;
import it.eng.spagobi.tools.dataset.common.metadata.IFieldMetaData;
import it.eng.spagobi.tools.dataset.common.metadata.IFieldMetaData.FieldType;
import it.eng.spagobi.tools.dataset.common.metadata.IMetaData;
import it.eng.spagobi.tools.dataset.common.query.AggregationFunctions;
import it.eng.spagobi.utilities.assertion.Assert;
import it.eng.spagobi.utilities.engines.EngineConstants;
import it.eng.spagobi.utilities.engines.SpagoBIEngineServiceExceptionHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Antonella Giachino (antonella.giachino@eng.it)
 * 
 */

@Path("/xdatasets/metafields")
public class GetDatasetMetaFields { 
	
	// PROPERTIES TO LOOK FOR INTO THE FIELDS
	public static final String PROPERTY_VISIBLE = "visible";
	public static final String PROPERTY_CALCULATED_EXPERT = "calculatedExpert";
	public static final String PROPERTY_IS_SEGMENT_ATTRIBUTE = "isSegmentAttribute";
	public static final String PROPERTY_IS_MANDATORY_MEASURE = "isMandatoryMeasure";
	public static final String PROPERTY_AGGREGATION_FUNCTION = "aggregationFunction";

	
	static private Logger logger = Logger.getLogger(GetDatasetMetaFields.class);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getDataSetMetaFields(@Context HttpServletRequest req) {
		
		CockpitEngineInstance engineInstance = (CockpitEngineInstance)req.getSession().getAttribute(EngineConstants.ENGINE_INSTANCE );
		Assert.assertNotNull(engineInstance, "It's not possible to execute GetDataSetMetaFields service before having properly created an instance of EngineInstance class");
		
		IEngUserProfile profile = (IEngUserProfile) req.getSession().getAttribute(IEngUserProfile.ENG_USER_PROFILE);

		String dsLabel = req.getParameter("dataset");
		Assert.assertNotNull(dsLabel, "The dataset label is null!!");
		try {
			DataSetServiceProxy datasetProxy = new DataSetServiceProxy((String)profile.getUserUniqueIdentifier() , req.getSession());
			IDataSet dataset = datasetProxy.getDataSetByLabel(dsLabel);

			Assert.assertNotNull(dataset, "The engine instance is missing the dataset!!");
			IMetaData metadata = dataset.getMetadata();
			Assert.assertNotNull(metadata, "No metadata retrieved by the dataset");
	
			JSONArray fieldsJSON = writeFields(metadata);
			logger.debug("Metadata read:");
			logger.debug(fieldsJSON);
			
			JSONObject resultsJSON = new JSONObject();
			resultsJSON.put("results", fieldsJSON);
		
			return resultsJSON.toString();
			
		} catch(Throwable t) {
			throw SpagoBIEngineServiceExceptionHandler.getInstance().getWrappedException("", engineInstance, t);
		} finally {			
			logger.debug("OUT");
		}	
	}
	
	public JSONArray writeFields(IMetaData metadata) throws Exception {

		// field's meta
		JSONArray fieldsMetaDataJSON = new JSONArray();
		
		List<JSONObject> attributesList = new ArrayList<JSONObject>();
		List<JSONObject> measuresList = new ArrayList<JSONObject>();

		int fieldCount = metadata.getFieldCount();
		logger.debug("Number of fields = " + fieldCount);
		Assert.assertTrue(fieldCount > 0, "Dataset has no fields!!!");

		for (int i = 0; i < fieldCount; i++) {
			IFieldMetaData fieldMetaData = metadata.getFieldMeta(i);
			Assert.assertNotNull(fieldMetaData, "Field metadata for position " + i + " not found.");
			logger.debug("Evaluating field with name [" + fieldMetaData.getName() + "], alias [" + fieldMetaData.getAlias() + "] ...");

			Boolean isCalculatedExpert= (Boolean) fieldMetaData.getProperty(PROPERTY_CALCULATED_EXPERT);
			
			if(isCalculatedExpert!=null && isCalculatedExpert){
				logger.debug("The field is a expert calculated field so we skip it");
				//continue;
			}
			
			Object propertyRawValue = fieldMetaData.getProperty(PROPERTY_VISIBLE);
			logger.debug("Read property " + PROPERTY_VISIBLE + ": its value is [" + propertyRawValue + "]");
			
			if (propertyRawValue != null && !propertyRawValue.toString().equals("")
					&& (Boolean.parseBoolean(propertyRawValue.toString()) == false)) {
				logger.debug("The field is not visible");
				continue;
			} else {
				logger.debug("The field is visible");
			}

			String fieldName = getFieldName(fieldMetaData);
			String fieldHeader = getFieldAlias(fieldMetaData);

			JSONObject fieldMetaDataJSON = new JSONObject();
			fieldMetaDataJSON.put("id", fieldName);						
			fieldMetaDataJSON.put("alias", fieldHeader);

			FieldType type = fieldMetaData.getFieldType();
			logger.debug("The field type is " + type.name());
			switch (type) {
			case ATTRIBUTE:
				Object isSegmentAttributeObj = fieldMetaData.getProperty(PROPERTY_IS_SEGMENT_ATTRIBUTE);
				logger.debug("Read property " + PROPERTY_IS_SEGMENT_ATTRIBUTE + ": its value is [" + propertyRawValue + "]");
				String attributeNature = (isSegmentAttributeObj != null
						&& (Boolean.parseBoolean(isSegmentAttributeObj.toString())==true)) ? "segment_attribute" : "attribute";
				
				logger.debug("The nature of the attribute is recognized as " + attributeNature);
				fieldMetaDataJSON.put("nature", attributeNature);
				fieldMetaDataJSON.put("funct", AggregationFunctions.NONE);
				fieldMetaDataJSON.put("iconCls", attributeNature);
				break;
			case MEASURE:
				Object isMandatoryMeasureObj = fieldMetaData.getProperty(PROPERTY_IS_MANDATORY_MEASURE);
				logger.debug("Read property " + PROPERTY_IS_MANDATORY_MEASURE + ": its value is [" + isMandatoryMeasureObj + "]");
				String measureNature = (isMandatoryMeasureObj != null
						&& (Boolean.parseBoolean(isMandatoryMeasureObj.toString())==true)) ? "mandatory_measure" : "measure";
				logger.debug("The nature of the measure is recognized as " + measureNature);
				fieldMetaDataJSON.put("nature", measureNature);
				String aggregationFunction = (String) fieldMetaData.getProperty(PROPERTY_AGGREGATION_FUNCTION);
				logger.debug("Read property " + PROPERTY_AGGREGATION_FUNCTION + ": its value is [" + aggregationFunction + "]");
				fieldMetaDataJSON.put("funct", AggregationFunctions.get(aggregationFunction).getName());
				fieldMetaDataJSON.put("iconCls", measureNature);
				String decimalPrecision= (String) fieldMetaData.getProperty(IFieldMetaData.DECIMALPRECISION);
				if(decimalPrecision!=null){
					fieldMetaDataJSON.put("precision", decimalPrecision);
				}else{
					fieldMetaDataJSON.put("precision", "2");
				}
				break;
			}
			
			if(type.equals(it.eng.spagobi.tools.dataset.common.metadata.IFieldMetaData.FieldType.MEASURE)){
				measuresList.add(fieldMetaDataJSON);
			}
			else{
				attributesList.add(fieldMetaDataJSON);
			}
		}

		
		//  put first measures and only after attributes
		
		for (Iterator iterator = measuresList.iterator(); iterator.hasNext();) {
			JSONObject jsonObject = (JSONObject) iterator.next();
			fieldsMetaDataJSON.put(jsonObject);
		}	

		for (Iterator iterator = attributesList.iterator(); iterator.hasNext();) {
			JSONObject jsonObject = (JSONObject) iterator.next();
			fieldsMetaDataJSON.put(jsonObject);
		}	

		return fieldsMetaDataJSON;

	}

	protected String getFieldAlias(IFieldMetaData fieldMetaData) {
		String fieldAlias = fieldMetaData.getAlias() != null ? fieldMetaData.getAlias() : fieldMetaData.getName();
		return fieldAlias;
	}

	protected String getFieldName(IFieldMetaData fieldMetaData) {
		String fieldName = fieldMetaData.getName();
		return fieldName;
	}


}