/*         
 *  Copyright 2002-2018 Barcelona Supercomputing Center (www.bsc.es)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package es.bsc.compss.types.execution;

import es.bsc.compss.types.annotations.parameter.DataType;
import es.bsc.compss.types.annotations.parameter.Stream;
import java.util.List;


/**
 *
 * @author flordan
 */
public interface InvocationParam {

    public void setType(DataType type);

    public DataType getType();

    public boolean isPreserveSourceData();

    public boolean isWriteFinalValue();

    public String getPrefix();

    public String getName();

    public Stream getStream();

    public String getOriginalName();

    public void setOriginalName(String originalName);

    public Object getValue();

    public void setValue(Object val);

    public void setValueClass(Class<?> aClass);

    public Class<?> getValueClass();

    public String getDataMgmtId();

    public String getSourceDataId();

    public List<? extends InvocationParamURI> getSources();

}