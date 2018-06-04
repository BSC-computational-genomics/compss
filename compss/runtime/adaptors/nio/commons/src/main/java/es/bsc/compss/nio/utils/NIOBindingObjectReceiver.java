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
package es.bsc.compss.nio.utils;

import es.bsc.comm.exceptions.CommException;
import es.bsc.comm.exceptions.CommException.ErrorType;
import es.bsc.comm.nio.NIOConnection;
import es.bsc.comm.nio.exceptions.NIOException;
import es.bsc.comm.nio.exceptions.NIOException.SpecificErrorType;
import es.bsc.compss.log.Loggers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NIOBindingObjectReceiver implements Runnable {

    protected static final Logger LOGGER = LogManager.getLogger(Loggers.COMM);
    private static final boolean DEBUG = LOGGER.isDebugEnabled();
    
    NIOBindingObjectStream nbos;
    String id;
    int type;
    NIOConnection c;
    
    public NIOBindingObjectReceiver(NIOConnection c, String id, int type, NIOBindingObjectStream nbos) {
        this.nbos = nbos;
        this.id = id;
        this.type = type;
        this.c = c;
    }

    public void run() {
      int res = NIOBindingDataManager.receiveNativeObject(id, type, nbos);
      if (res != 0){
          LOGGER.error("Error ("+res+") receiving native object "+id);
          c.error(new NIOException(SpecificErrorType.READ, "Error ("+res+") receiving native object."));
      }else{
          NIOBindingDataManager.objectReceived(c);
      }
      c.finishConnection();
      
    }
}