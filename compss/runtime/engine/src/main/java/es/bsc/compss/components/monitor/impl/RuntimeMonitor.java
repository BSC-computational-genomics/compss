/*
 *  Copyright 2002-2019 Barcelona Supercomputing Center (www.bsc.es)
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
package es.bsc.compss.components.monitor.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.bsc.compss.COMPSsConstants;
import es.bsc.compss.components.impl.AccessProcessor;
import es.bsc.compss.components.impl.TaskDispatcher;
import es.bsc.compss.log.Loggers;
import es.bsc.compss.util.ResourceManager;


/**
 * Represents the thread to handle all the information needed by the COMPSs Monitor
 */
public class RuntimeMonitor implements Runnable {

    private static final boolean monitorEnabled = System.getProperty(COMPSsConstants.MONITOR) != null
            && !System.getProperty(COMPSsConstants.MONITOR).equals("0") ? true : false;

    private static final String monitorDirPath;

    private static final Logger logger = LogManager.getLogger(Loggers.ALL_COMP);
    private static final String ERROR_GENERATING_DATA = "Error generating monitoring data";

    /**
     * Task Dispatcher associated to the monitor
     */
    private TaskDispatcher TD;
    /**
     * Access Processor associated to the monitor
     */
    private AccessProcessor AP;
    /**
     * Graph Generator associated to the monitor
     */
    private GraphGenerator GM;
    /**
     * Time inbetween two state queries
     */
    private long sleepTime;
    /**
     * Monitor keeps making queries
     */
    private boolean keepRunning;
    /**
     * The monitor thread is still alive
     */
    private boolean running;
    /**
     * Monitor Thread
     */
    private Thread monitor;

    /**
     * COMPSs installation directory
     */
    String installDir;

    static {
        // Get the monitorDirPath from the graph because it is always initialized before the RuntimeMonitor
        monitorDirPath = GraphGenerator.getMonitorDirPath();
    }


    /**
     * Constructs a new Runtime monitor. If the monitor parameter has been used, it starts a new thread which
     * periodically checks the current state of the execution and gives the outputs to the user. If only the graph
     * parameter (or none) has been used, the monitor starts but NOT as a thread.
     *
     * @param AP Task Processor associated to the monitor
     * @param TD Task Dispatcher associated to the monitor
     * @param GM Graph Generator to be used by the monitor
     * @param sleepTime interval of time between state queries
     */
    public RuntimeMonitor(AccessProcessor AP, TaskDispatcher TD, GraphGenerator GM, long sleepTime) {
        this.TD = TD;
        this.AP = AP;
        this.GM = GM;

        // Configure and start internal monitor thread
        this.keepRunning = true;
        this.sleepTime = sleepTime;
        installDir = System.getenv().get(COMPSsConstants.COMPSS_HOME);
        monitor = new Thread(this);
        monitor.setName("Monitor Thread");
        monitor.start();
    }

    /**
     * Checks periodically the status of the execution and returns the results to the user
     */
    public void run() {
        running = true;
        while (keepRunning) {
            try {
                // Print XML state for Monitor
                getXMLTaskState();

                // Print current task graph
                printCurrentGraph();

                // Print load and resources information on log
                TD.printCurrentState();
                ResourceManager.printResourcesState();
                Thread.sleep(sleepTime);
            } catch (IOException ioe) {
                logger.error(ERROR_GENERATING_DATA, ioe);
            } catch (InterruptedException ie) {
                logger.error(ERROR_GENERATING_DATA, ie);
                Thread.currentThread().interrupt();
            }
        }
        running = false;
    }

    /**
     * Stops the monitoring
     */
    public void shutdown() {
        this.keepRunning = false;

        try {
            while (running) {
                Thread.sleep(sleepTime);
            }
            // Print XML state for Monitor
            getXMLTaskState();

            // Print current task graph
            printCurrentGraph();
        } catch (IOException | InterruptedException ioe) {
            logger.error(ERROR_GENERATING_DATA, ioe);
        }

        // Clears the execution files
        if (!new File(monitorDirPath + "monitor.xml").delete()) {
            logger.error("Error clearing monitor.xml execution files");
        }
    }

    /**
     * Prints in a file the current state of the Task load
     */
    private void getXMLTaskState() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append("\n");
        sb.append("<?xml-stylesheet type=\"text/xsl\" href=\"").append(installDir)
                .append("/xml/monitor/monitor.xsl\"?>").append("\n");
        sb.append("<COMPSsState>").append("\n");
        sb.append(AP.getCurrentTaskState());
        sb.append(TD.getCurrentMonitoringData());
        sb.append("</COMPSsState>");

        BufferedWriter fw = new BufferedWriter(new FileWriter(monitorDirPath + "COMPSs_state.xml"));
        fw.write(sb.toString());
        fw.close();
        fw = null;
    }

    /**
     * Prints the current graph to the specified GM file
     */
    private void printCurrentGraph() {
        BufferedWriter graph = this.GM.getAndOpenCurrentGraph();
        this.TD.printCurrentGraph(graph);
        this.GM.closeCurrentGraph();
    }

    /**
     * Returns the monitor dir path
     *
     * @return
     */
    public static String getMonitorDirPath() {
        return monitorDirPath;
    }

    /**
     * Returns if the monitor is enabled or not
     *
     * @return
     */
    public static boolean isEnabled() {
        return monitorEnabled;
    }

}
