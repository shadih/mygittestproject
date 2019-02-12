package com.att.gfp.data.ipagPreprocess.preprocess;

import java.util.List;

import com.hp.uca.common.exception.UcaException;
import com.hp.uca.expert.lifecycle.DefaultScenarioInitialization;
import com.hp.uca.expert.scenario.Scenario;
import com.hp.uca.expert.vp.internal.ValuePackApplicationContext;
import com.att.gfp.data.preprocess.conf.PreProcessConfiguration;


public class PreprocessInitialization extends DefaultScenarioInitialization {
	
    public PreprocessInitialization(Scenario scenario,
            ValuePackApplicationContext valuePackApplicationContext) {
      super(scenario, valuePackApplicationContext);
      scenario.addSpecificConfiguration(new PreProcessConfiguration() );
      
}
    
@Override
public void initializeScenario() throws UcaException {
      //put your code here
}

@Override
public void disposeScenario() throws UcaException {
      //put your code here
}

@Override
public void resynchPerFlow(String arg0, List<Object> arg1) throws Exception {
	// TODO Auto-generated method stub
	
}

}
