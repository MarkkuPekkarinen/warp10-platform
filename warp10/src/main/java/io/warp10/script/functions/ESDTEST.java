//
//   Copyright 2018  SenX S.A.S.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package io.warp10.script.functions;

import io.warp10.continuum.gts.GTSOutliersHelper;
import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.script.GTSStackFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Apply an ESD test
 * @see http://www.itl.nist.gov/div898/handbook/eda/section3/eda35h3.htm
 * 
 * If mad is true, then use modified z-score.
 * Alpha is optional. Default value is 0.05.
 */
public class ESDTEST extends GTSStackFunction {

  private static final String UPPERBOUND_PARAM = "k";
  private static final String MODIFIED_PARAM = "mad";
  private static final String SIGNIFICANCE_PARAM = "alpha";
  
  private static final double SIGNIFICANCE_DEFAULT = 0.05D;
    
  public ESDTEST(String name) {
    super(name);
  }
  
  @Override
  protected Map<String, Object> retrieveParameters(WarpScriptStack stack) throws WarpScriptException {
    Map<String,Object> params = new HashMap<String,Object>();
    
    Object top = stack.pop();
    
    boolean alpha_is_default = false;
    
    if (!(top instanceof Double)) {
      if (!(top instanceof Boolean)) {
        throw new WarpScriptException(getName() + " expects a significance level (a DOUBLE)  or a flag (a BOOLEAN) on top of the stack.");
      } else {
        alpha_is_default = true;
      }
    }
    
    if (!alpha_is_default) {
      params.put(SIGNIFICANCE_PARAM, ((Number) top).doubleValue());
      
      top = stack.pop();
    } else {
      params.put(SIGNIFICANCE_PARAM, SIGNIFICANCE_DEFAULT);
    }
    
    if (!(top instanceof Boolean)) {
      throw new WarpScriptException(getName() + " expects a flag (a BOOLEAN) that indicates wether to use modified z-score below the SIGNIFICANCE level.");
    }
    
    params.put(MODIFIED_PARAM, ((Boolean) top).booleanValue());
    
    top = stack.pop();
    
    if (!(top instanceof Long)) {
      throw new WarpScriptException(getName() + " expects an upper bound of the number of outliers (a LONG) below the flag.");
    }
    
    params.put(UPPERBOUND_PARAM, ((Number) top).intValue());
    
    return params;
  }

  @Override
  protected Object gtsOp(Map<String, Object> params, GeoTimeSerie gts) throws WarpScriptException {    
    int k = (int) params.get(UPPERBOUND_PARAM);
    boolean mad = (boolean) params.get(MODIFIED_PARAM);    
    double alpha = (double) params.get(SIGNIFICANCE_PARAM);
    
    return GTSOutliersHelper.ESDTest(gts, k, mad, alpha);
  }
}
