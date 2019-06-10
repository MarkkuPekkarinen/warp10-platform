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

package io.warp10.script.filler;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptFillerFunction;

public class FillerPrevious extends NamedWarpScriptFunction implements WarpScriptFillerFunction {
  
  public FillerPrevious(String name) {
    super(name);
  }
  
  @Override
  public Object[] apply(Object[] args) throws WarpScriptException {
    
    Object[] results = new Object[4];
    
    Object[] prev = (Object[]) args[1];
    Object[] other = (Object[]) args[2];

    // We cannot interpolate on the edges
    if (null == prev[0]) {
      return results;
    }

    long tick = ((Number) other[0]).longValue();
    
    long prevloc = ((Number) prev[1]).longValue();
    long prevelev = ((Number) prev[2]).longValue();
    Object prevvalue = prev[3];
    
    results[0] = tick;
    results[1] = prevloc;
    results[2] = prevelev;
    results[3] = prevvalue;
    
    return results;
  }
    
  @Override
  public int getPostWindow() {
    return 0;
  }
  
  @Override
  public int getPreWindow() {
    return 1;
  }

}
