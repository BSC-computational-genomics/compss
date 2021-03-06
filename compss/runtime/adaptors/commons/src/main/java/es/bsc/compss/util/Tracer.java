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

package es.bsc.compss.util;

import es.bsc.cepbatools.extrae.Wrapper;
import es.bsc.compss.COMPSsConstants;
import es.bsc.compss.comm.Comm;
import es.bsc.compss.log.Loggers;
import es.bsc.compss.types.data.LogicalData;
import es.bsc.compss.types.data.listener.TracingCopyListener;
import es.bsc.compss.types.data.location.DataLocation;
import es.bsc.compss.types.data.transferable.TracingCopyTransferable;
import es.bsc.compss.types.uri.SimpleURI;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public abstract class Tracer {

    private static final String taskDesc = "Task";
    private static final String apiDesc = "Runtime";
    private static final String taskIdDesc = "Task IDs";
    private static final String dataTransfersDesc = "Data Transfers";
    private static final String tasksTransfersDesc = "Task Transfers Request";
    private static final String storageDesc = "Storage API";
    private static final String insideTaskDesc = "Events inside tasks";

    protected static final String TRACE_SCRIPT_PATH = File.separator + "Runtime" + File.separator + "scripts"
            + File.separator + "system" + File.separator + "trace" + File.separator + "trace.sh";
    protected static final String traceOutRelativePath = File.separator + "trace" + File.separator + "tracer.out";
    protected static final String traceErrRelativePath = File.separator + "trace" + File.separator + "tracer.err";

    protected static final Logger LOGGER = LogManager.getLogger(Loggers.TRACING);
    protected static final boolean DEBUG = LOGGER.isDebugEnabled();
    protected static final String ERROR_TRACE_DIR = "ERROR: Cannot create trace directory";

    private static final int TASKS_FUNC_TYPE = 8_000_000;
    private static final int RUNTIME_EVENTS = 8_000_001;
    private static final int TASKS_ID_TYPE = 8_000_002;
    private static final int TASK_TRANSFERS = 8_000_003;
    private static final int DATA_TRANSFERS = 8_000_004;
    private static final int STORAGE_TYPE = 8_000_005;
    private static final int READY_COUNTS = 8_000_006;
    private static final int SYNC_TYPE = 8_000_666;
    private static final int INSIDE_TASKS_TYPE = 60_000_100;

    public static final int EVENT_END = 0;

    public static final int BASIC_MODE = 1;

    public static final int SCOREP_MODE = -1;
    public static final int MAP_MODE = -2;

    public static final String LD_PRELOAD = "LD_PRELOAD";
    public static final String EXTRAE_CONFIG_FILE = "EXTRAE_CONFIG_FILE";

    protected static int tracingLevel = 0;

    private static final boolean isCustomExtraeFile = (System.getProperty(COMPSsConstants.EXTRAE_CONFIG_FILE) != null)
            && !System.getProperty(COMPSsConstants.EXTRAE_CONFIG_FILE).isEmpty()
            && !System.getProperty(COMPSsConstants.EXTRAE_CONFIG_FILE).equals("null");
    private static final String extraeFile = isCustomExtraeFile ? System.getProperty(COMPSsConstants.EXTRAE_CONFIG_FILE)
            : "null";


    public enum Event {
        STATIC_IT(1, RUNTIME_EVENTS, "Loading Runtime"), // Static COMPSs
        START(2, RUNTIME_EVENTS, "Start"), // Start
        STOP(3, RUNTIME_EVENTS, "Stop"), // Stop
        TASK(4, RUNTIME_EVENTS, "Execute Task"), // Execute task
        NO_MORE_TASKS(5, RUNTIME_EVENTS, "Waiting for tasks end"), // No more tasks
        WAIT_FOR_ALL_TASKS(6, RUNTIME_EVENTS, "Barrier"), // Waiting for tasks
        OPEN_FILE(7, RUNTIME_EVENTS, "Waiting for open file"), // Open file
        GET_FILE(8, RUNTIME_EVENTS, "Waiting for get file"), // Get file
        GET_OBJECT(9, RUNTIME_EVENTS, "Waiting for get object"), // Get Object
        TASK_RUNNING(11, RUNTIME_EVENTS, "Task Running"), // Task running
        DELETE(12, RUNTIME_EVENTS, "Delete File"), // Delete file
        WORKER_RECEIVED_NEW_TASK(13, RUNTIME_EVENTS, "Received new task"), // New task at worker

        // Access Processor Events
        DEBUG(17, RUNTIME_EVENTS, "Access Processor: Debug"), // Debug
        ANALYSE_TASK(18, RUNTIME_EVENTS, "Access Processor: Analyse task"), // Analyse task
        UPDATE_GRAPH(19, RUNTIME_EVENTS, "Access Processor: Update graph"), // Update graph
        WAIT_FOR_TASK(20, RUNTIME_EVENTS, "Access Processor: Wait for task"), // wait for task
        END_OF_APP(21, RUNTIME_EVENTS, "Access Processor: End of app"), // End of application
        ALREADY_ACCESSED(22, RUNTIME_EVENTS, "Access Processor: Already accessed"), // Already accessed
        REGISTER_DATA_ACCESS(23, RUNTIME_EVENTS, "Access Processor: Register data access"), // Register data access
        TRANSFER_OPEN_FILE(24, RUNTIME_EVENTS, "Access Processor: Transfer open file"), // Transfer open file
        TRANSFER_RAW_FILE(25, RUNTIME_EVENTS, "Access Processor: Transfer raw file"), // Transfer raw file
        TRANSFER_OBJECT(26, RUNTIME_EVENTS, "Access Processor: Transfer object"), // Transfer object
        NEW_VERSION_SAME_VALUE(27, RUNTIME_EVENTS, "Access Processor: New version same value"), // New version
        IS_OBJECT_HERE(28, RUNTIME_EVENTS, "Access Processor: Is object here"), // Is object here
        SET_OBJECT_VERSION_VALUE(29, RUNTIME_EVENTS, "Access Processor: Set object version value"), // Set version
        GET_LAST_RENAMING(30, RUNTIME_EVENTS, "Access Processor: Get last renaming"), // Get last renaming
        BLOCK_AND_GET_RESULT_FILES(31, RUNTIME_EVENTS, "Access Processor: Block and get result files"), // Get files
        UNBLOCK_RESULT_FILES(32, RUNTIME_EVENTS, "Access Processor: Unblock result files"), // Unblock result files
        SHUTDOWN(33, RUNTIME_EVENTS, "Access Processor: Shutdown"), // Shutdown
        GRAPHSTATE(34, RUNTIME_EVENTS, "Access Processor: Graphstate"), // Graph state
        TASKSTATE(35, RUNTIME_EVENTS, "Access Processor: Taskstate"), // Task state
        DELETE_FILE(36, RUNTIME_EVENTS, "Access Processor: Delete file"), // Delete file
        FINISH_ACCESS_FILE(37, RUNTIME_EVENTS, "Access Processor: Finish acess to file"), // Finish access to file

        // Storage Events
        STORAGE_GETBYID(38, STORAGE_TYPE, "getByID"), // Get By Id
        STORAGE_NEWREPLICA(39, STORAGE_TYPE, "newReplica"), // New replica
        STORAGE_NEWVERSION(40, STORAGE_TYPE, "newVersion"), // New version
        STORAGE_INVOKE(41, STORAGE_TYPE, "invoke"), // Invoke
        STORAGE_EXECUTETASK(42, STORAGE_TYPE, "executeTask"), // Execute task
        STORAGE_GETLOCATIONS(43, STORAGE_TYPE, "getLocations"), // Get locations
        STORAGE_CONSOLIDATE(44, STORAGE_TYPE, "consolidateVersion"), // Consolidate version

        // Task Dispatcher Events
        ACTION_UPDATE(45, RUNTIME_EVENTS, "Task Dispatcher: Action update"), // Action update
        CE_REGISTRATION(46, RUNTIME_EVENTS, "Task Dispatcher: CE registration"), // CE registration
        EXECUTE_TASKS(47, RUNTIME_EVENTS, "Task Dispatcher: Execute tasks"), // Execute task
        GET_CURRENT_SCHEDULE(48, RUNTIME_EVENTS, "Task Dispatcher: Get current schedule"), // Get schedule
        PRINT_CURRENT_GRAPH(49, RUNTIME_EVENTS, "Task Dispatcher: Print current graph"), // Print graph
        MONITORING_DATA(50, RUNTIME_EVENTS, "Task Dispatcher: Monitoring data"), // Get monitor data
        TD_SHUTDOWN(51, RUNTIME_EVENTS, "Task Dispatcher: Shutdown"), // Shutdown
        UPDATE_CEI_LOCAL(52, RUNTIME_EVENTS, "Task Dispatcher: Update CEI local"), // Update CEI
        WORKER_UPDATE_REQUEST(53, RUNTIME_EVENTS, "Task Dispatcher: Worker update request"), // Update worker

        // Task Events
        CREATING_TASK_SANDBOX(54, RUNTIME_EVENTS, "Worker: Creating task sandbox"), // Create task sandbox
        REMOVING_TASK_SANDBOX(55, RUNTIME_EVENTS, "Worker: Removing task sandbox"), // Erase task sandbox
        TASK_EXECUTION_PYTHON(1, INSIDE_TASKS_TYPE, "Task execution"), // Execute python task
        USER_CODE_PYTHON1(2, INSIDE_TASKS_TYPE, "User code execution 1"), // User code 1
        USER_CODE_PYTHON2(3, INSIDE_TASKS_TYPE, "User code execution 2"), // User code 2
        USER_CODE_PYTHON3(4, INSIDE_TASKS_TYPE, "User code execution 3"), // User code 3
        IMPORTING_MODULES_PYTHON(5, INSIDE_TASKS_TYPE, "Importing modules"), // Import python
        THREAD_BINDING_PYTHON(6, INSIDE_TASKS_TYPE, "Thread binding"), // Thread binding
        DESERIALIZE_OBJECT_PYTHON1(7, INSIDE_TASKS_TYPE, "Deserializing object"), // Deserialize
        DESERIALIZE_OBJECT_PYTHON2(8, INSIDE_TASKS_TYPE, "Deserializing object"), // Deserialize
        SERIALIZE_OBJECT_PYTHON(9, INSIDE_TASKS_TYPE, "Serializing object"), // Serialize
        CREATE_THREADS_PYTHON(10, INSIDE_TASKS_TYPE, "Create persistent threads"), // Create threads python
        GET_BY_ID(11, INSIDE_TASKS_TYPE, "Get by ID persistent object"), // Get by id
        MAKE_PERSISTENT(12, INSIDE_TASKS_TYPE, "Make persistent object"), // Make persistent
        DELETE_PERSISTENT(13, INSIDE_TASKS_TYPE, "Delete persistent object"), // Delete persistent
        WORKER_RUNNING(102, INSIDE_TASKS_TYPE, "Worker running"), // Worker running

        READY_COUNT(1, READY_COUNTS, "Ready queue count");// Ready count

        private final int id;
        private final int type;
        private final String signature;


        private Event(int id, int type, String signature) {
            this.id = id;
            this.type = type;
            this.signature = signature;
        }

        public int getId() {
            return this.id;
        }

        public int getType() {
            return this.type;
        }

        public String getSignature() {
            return this.signature;
        }
    }


    private static String traceDirPath;
    private static Map<String, TraceHost> hostToSlots;
    private static AtomicInteger hostId;


    /**
     * Initializes tracer creating the trace folder. If extrae's tracing is used (level > 0) then
     * the current node (master) sets its nodeID (taskID in extrae) to 0, and its number of tasks
     * to 1 (a single program).
     *
     * @param level type of tracing: -3: arm-ddt, -2: arm-map, -1: scorep, 0: off, 1: extrae-basic,
     *              2: extrae-advanced
     */
    public static void init(int level) {
        if (DEBUG) {
            LOGGER.debug("Initializing tracing with level " + level);
        }

        hostId = new AtomicInteger(1);
        hostToSlots = new HashMap<>();

        traceDirPath = Comm.getAppHost().getAppLogDirPath() + "trace" + File.separator;
        if (!new File(traceDirPath).mkdir()) {
            ErrorManager.error(ERROR_TRACE_DIR);
        }

        tracingLevel = level;

        if (Tracer.extraeEnabled()) {
            if (DEBUG) {
                LOGGER.debug("Initializing extrae Wrapper.");
            }
            Wrapper.SetTaskID(0);
            Wrapper.SetNumTasks(1);
        }else if (Tracer.scorepEnabled()) {
            if (DEBUG) {
                LOGGER.debug("Initializing scorep.");
            }
        }else if (Tracer.mapEnabled()) {
            if (DEBUG) {
                LOGGER.debug("Initializing arm-map.");
            }
        }
    }

    /**
     * Returns if the current execution is being instrumented by extrae.
     *
     * @return true if currently instrumented by extrae
     */
    public static boolean extraeEnabled() {
        return tracingLevel > 0;
    }

    /**
     * Returns if the current execution is being instrumented by scorep.
     *
     * @return true if currently instrumented by scorep
     */
    public static boolean scorepEnabled() {
        return tracingLevel == Tracer.SCOREP_MODE;
    }

    /**
     * Returns if the current execution is being instrumented by arm-map.
     *
     * @return true if currently instrumented by arm-map
     */
    public static boolean mapEnabled() {
        return tracingLevel == Tracer.MAP_MODE;
    }

    /**
     * Returns if any kind of tracing is activated including ddt, map, scorep, or extrae).
     *
     * @return true if any kind of tracing is activated
     */
    public static boolean isActivated() {
        return tracingLevel != 0;
    }

    /**
     * Returns whether extrae is working and is activated in basic mode.
     *
     * @return true if extrae is enabled in basic mode
     */
    public static boolean basicModeEnabled() {
        return tracingLevel == Tracer.BASIC_MODE;
    }

    /**
     * Returns with which tracing level the Tracer has been initialized (0 if it's not active).
     *
     * @return int with tracing level (in [-3, -2, -1, 0, 1, 2])
     */
    public static int getLevel() {
        return tracingLevel;
    }

    /**
     * Returns the config file used for extrae.
     *
     * @return path of extrae config file
     */
    public static String getExtraeFile() {
        return extraeFile;
    }

    /**
     * When using extrae's tracing, this call enables the instrumentation of ALL created threads
     * from here onwards. To deactivate it use disablePThreads().
     */
    public static void enablePThreads() {
        synchronized (Tracer.class) {
            Wrapper.SetOptions(Wrapper.EXTRAE_ENABLE_ALL_OPTIONS);
        }
    }

    /**
     * When using extrae's tracing, this call disables the instrumentation of any created threads
     * from here onwards. To reactivate it use enablePThreads()
     */
    public static void disablePThreads() {
        synchronized (Tracer.class) {
            Wrapper.SetOptions(Wrapper.EXTRAE_ENABLE_ALL_OPTIONS & ~Wrapper.EXTRAE_PTHREAD_OPTION);
        }
    }

    /**
     * Adds a host name and its number of slots to a hashmap required to later merge the traces from
     * each host into a single one.
     *
     * @param name  of the host
     * @param slots number of threads the host is expected to have (used in GAT, in NIO is 0,
     *              because they will be computed automatically
     * @return the next ID to be used during the initialization of the tracing in the given host.
     */
    public static int registerHost(String name, int slots) {
        if (DEBUG) {
            LOGGER.debug("Tracing: Registering host " + name + " in the tracing system");
        }
        int id;
        synchronized (hostToSlots) {
            if (hostToSlots.containsKey(name)) {
                if (DEBUG) {
                    LOGGER.debug("Host " + name + " already in tracing system, skipping");
                }
                return -1;
            }
            id = hostId.getAndIncrement();
            hostToSlots.put(name, new TraceHost(slots));
        }
        return id;
    }

    /**
     * Returns the next slot ID (thread) that will run a task (GAT only).
     *
     * @param host that is going to execute a task
     * @return the next thread ID available to execute task (don't care about real order)
     */
    public static int getNextSlot(String host) {
        int slot = hostToSlots.get(host).getNextSlot();
        if (DEBUG) {
            LOGGER.debug("Tracing: Getting slot " + slot + " of host " + host);
        }
        return slot;
    }

    /**
     * Signals that a slot ID (thread) of a host is free again.
     *
     * @param host that is going to have a slot freed
     * @param slot to be freed
     */
    public static void freeSlot(String host, int slot) {
        if (DEBUG) {
            LOGGER.debug("Tracing: Freeing slot " + slot + " of host " + host);
        }
        hostToSlots.get(host).freeSlot(slot);
    }

    public static int getRuntimeEventsType() {
        return RUNTIME_EVENTS;
    }

    public static int getSyncType() {
        return SYNC_TYPE;
    }

    public static int getTaskTransfersType() {
        return TASK_TRANSFERS;
    }

    public static int getDataTransfersType() {
        return DATA_TRANSFERS;
    }

    public static int getTaskEventsType() {
        return TASKS_FUNC_TYPE;
    }

    public static int getTaskSchedulingType() {
        return TASKS_ID_TYPE;
    }

    public static int getInsideTasksEventsType() {
        return INSIDE_TASKS_TYPE;
    }

    public static Event getAcessProcessorRequestEvent(String eventType) {
        return Event.valueOf(eventType);
    }

    /**
     * Returns the corresponding event ID for a TD request event type.
     *
     * @param eventType of the TD
     * @return the tracing event ID associated with eventType
     */
    public static Event getTaskDispatcherRequestEvent(String eventType) {
        Event event = null;
        try {
            event = Event.valueOf(eventType);
        } catch (Exception e) {
            LOGGER.error("Task Dispatcher event " + eventType + " is not present in Tracer's list ");
        }
        return event;
    }

    /**
     * Emits an event using extrae's Wrapper. Requires that Tracer has been initialized with lvl >0
     *
     * @param eventID   ID of the event
     * @param eventType type of the event.
     */
    public static void emitEvent(long eventID, int eventType) {
        synchronized (Tracer.class) {
            Wrapper.Event(eventType, eventID);
        }

        if (DEBUG) {
            LOGGER.debug("Emitting synchronized event [type, id] = [" + eventType + " , " + eventID + "]");
        }
    }

    /**
     * Emits an event and the current PAPI counters activated using extrae's Wrapper. Requires
     * that Tracer has been initialized with lvl >0.
     *
     * @param taskId    ID of the event
     * @param eventType type of the event.
     */
    public static void emitEventAndCounters(int taskId, int eventType) {
        synchronized (Tracer.class) {
            Wrapper.Eventandcounters(eventType, taskId);
        }

        if (DEBUG) {
            LOGGER.debug("Emitting synchronized event with HW counters [type, taskId] = [" + eventType + " , " + taskId
                    + "]");
        }
    }

    /**
     * End the extrae tracing system. Finishes master's tracing, generates both master and worker's
     * packages, merges the packages, and clean the intermediate traces.
     */
    public static void fini() {
        if (DEBUG) {
            LOGGER.debug("Tracing: finalizing");
        }

        synchronized (Tracer.class) {
            if (extraeEnabled()){
                defineEvents();

                Wrapper.SetOptions(Wrapper.EXTRAE_ENABLE_ALL_OPTIONS & ~Wrapper.EXTRAE_PTHREAD_OPTION);
                Wrapper.Fini();
                Wrapper.SetOptions(Wrapper.EXTRAE_DISABLE_ALL_OPTIONS);

                generateMasterPackage("package");
                transferMasterPackage();
                generateTrace();
                cleanMasterPackage();
            }
        }
    }

    /**
     * Returns how many events of a given type exist.
     * @param type of the events
     * @return how many events does that type of event contains.
     */
    private static int getSizeByEventType(int type) {
        int size = 0;
        for (Event task : Event.values()) {
            if (task.getType() == type) {
                ++size;
            }
        }
        return size;
    }

    /**
     * Iterates over all the tracing events and sets them in the Wrapper to generate the config.
     * for the tracefile.
     */
    private static void defineEvents() {
        Map<String, Integer> signatureToId = CoreManager.getSignaturesToId();
        if (DEBUG) {
            LOGGER.debug("SignatureToId size: " + signatureToId.size());
        }

        int size = getSizeByEventType(RUNTIME_EVENTS) + 1;
        long[] values = new long[size];
        // int offset = Event.values().length; // We offset the values of the
        // defined API events (plus the 0 which is the end task always).

        String[] descriptionValues = new String[size];

        values[0] = 0;
        descriptionValues[0] = "End";
        int i = 1;
        for (Event task : Event.values()) {
            if (task.getType() == RUNTIME_EVENTS) {
                values[i] = task.getId();
                descriptionValues[i] = task.getSignature();
                if (DEBUG) {
                    LOGGER.debug("Tracing[API]: Api Event " + i + "=> value: " + values[i] + ", Desc: "
                            + descriptionValues[i]);
                }
                ++i;
            }
        }

        Wrapper.defineEventType(RUNTIME_EVENTS, apiDesc, values, descriptionValues);

        size = signatureToId.entrySet().size() + 1;

        values = new long[size];
        descriptionValues = new String[size];
        values[0] = 0;
        descriptionValues[0] = "End";

        i = 1;
        for (Entry<String, Integer> entry : signatureToId.entrySet()) {
            String signature = entry.getKey();
            Integer methodId = entry.getValue();
            values[i] = methodId + 1;
            LOGGER.debug("Tracing debug: " + signature);
            String methodName = signature.substring(signature.indexOf('.') + 1, signature.length());
            descriptionValues[i] = methodName;
            if (DEBUG) {
                LOGGER.debug("Tracing[TASKS_FUNC_TYPE] Event [i,methodId]: [" + i + "," + methodId + "] => value: "
                        + values[i] + ", Desc: " + descriptionValues[i]);
            }
            i++;
        }

        Wrapper.defineEventType(TASKS_FUNC_TYPE, taskDesc, values, descriptionValues);

        // Definition of TRANSFER_TYPE events
        size = getSizeByEventType(TASK_TRANSFERS) + 1;
        values = new long[size];
        descriptionValues = new String[size];

        values[0] = 0;
        descriptionValues[0] = "End";
        i = 1;
        for (Event task : Event.values()) {
            if (task.getType() == TASK_TRANSFERS) {
                values[i] = task.getId();
                descriptionValues[i] = task.getSignature();
                if (DEBUG) {
                    LOGGER.debug("Tracing[TASK_TRANSFERS]: Event " + i + "=> value: " + values[i] + ", Desc: "
                            + descriptionValues[i]);
                }
                ++i;
            }
        }

        Wrapper.defineEventType(TASK_TRANSFERS, tasksTransfersDesc, values, descriptionValues);

        // Definition of STORAGE_TYPE events
        size = getSizeByEventType(STORAGE_TYPE) + 1;
        values = new long[size];
        descriptionValues = new String[size];

        values[0] = 0;
        descriptionValues[0] = "End";
        i = 1;
        for (Event task : Event.values()) {
            if (task.getType() == STORAGE_TYPE) {
                values[i] = task.getId();
                descriptionValues[i] = task.getSignature();
                if (DEBUG) {
                    LOGGER.debug("Tracing[STORAGE_TYPE]: Event " + i + "=> value: " + values[i] + ", Desc: "
                            + descriptionValues[i]);
                }
                ++i;
            }
        }

        Wrapper.defineEventType(STORAGE_TYPE, storageDesc, values, descriptionValues);

        // Definition of Events inside task
        size = getSizeByEventType(INSIDE_TASKS_TYPE) + 1;
        values = new long[size];
        descriptionValues = new String[size];

        values[0] = 0;
        descriptionValues[0] = "End";
        i = 1;
        for (Event task : Event.values()) {
            if (task.getType() == INSIDE_TASKS_TYPE) {
                values[i] = task.getId();
                descriptionValues[i] = task.getSignature();
                if (DEBUG) {
                    LOGGER.debug("Tracing[INSIDE_TASKS_EVENTS]: Event " + i + "=> value: " + values[i] + ", Desc: "
                            + descriptionValues[i]);
                }
                ++i;
            }
        }

        Wrapper.defineEventType(INSIDE_TASKS_TYPE, insideTaskDesc, values, descriptionValues);

        // Definition of Scheduling and Transfer time events
        size = 0;
        values = new long[size];

        descriptionValues = new String[size];

        Wrapper.defineEventType(TASKS_ID_TYPE, taskIdDesc, values, descriptionValues);

        // Definition of Data transfers
        size = 0;
        values = new long[size];

        descriptionValues = new String[size];

        Wrapper.defineEventType(DATA_TRANSFERS, dataTransfersDesc, values, descriptionValues);
    }

    /**
     * Generate the tracing package for the master.
     * The mode parameter enables to use different packaging methods.
     * The currently supported modes are:
     *     "package" --------> for Extrae
     *     "package-scorep" -> for ScoreP
     *     "package-map" ----> for Map
     *
     * @param mode of the packaging (see trace.sh)
     */
    private static void generateMasterPackage(String mode) {
        if (DEBUG) {
            LOGGER.debug("Tracing: generating master package: " + mode);
        }

        String script = System.getenv(COMPSsConstants.COMPSS_HOME) + TRACE_SCRIPT_PATH;
        ProcessBuilder pb = new ProcessBuilder(script, mode, ".", "master");
        pb.environment().remove(LD_PRELOAD);
        Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            ErrorManager.warn("Error generating master package", e);
            return;
        }

        if (DEBUG) {
            StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), System.out, LOGGER);
            StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), System.err, LOGGER);
            outputGobbler.start();
            errorGobbler.start();
        }

        try {
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                ErrorManager.warn("Error generating master package, exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            ErrorManager.warn("Error generating master package (interruptedException) : " + e.getMessage());
        }
    }

    /**
     * Copy the tracing master package from the working directory. Node packages are transferred
     * on NIOTracer of GATTracer.
     */
    private static void transferMasterPackage() {
        if (DEBUG) {
            LOGGER.debug("Tracing: Transferring master package");
        }

        // Create source and target locations for tar.gz file
        String filename = "master_compss_trace.tar.gz";
        DataLocation source = null;
        String sourcePath = DataLocation.Protocol.FILE_URI.getSchema() + filename;
        try {
            SimpleURI uri = new SimpleURI(sourcePath);
            source = DataLocation.createLocation(Comm.getAppHost(), uri);
        } catch (Exception e) {
            ErrorManager.error(DataLocation.ERROR_INVALID_LOCATION + " " + sourcePath, e);
        }
        DataLocation target = null;
        String targetPath = DataLocation.Protocol.FILE_URI.getSchema() + traceDirPath + filename;
        try {
            SimpleURI uri = new SimpleURI(targetPath);
            target = DataLocation.createLocation(Comm.getAppHost(), uri);
        } catch (Exception e) {
            ErrorManager.error(DataLocation.ERROR_INVALID_LOCATION + " " + targetPath, e);
        }

        // Ask for data
        Semaphore sem = new Semaphore(0);
        TracingCopyListener tracingListener = new TracingCopyListener(sem);
        tracingListener.addOperation();

        Comm.getAppHost().getNode().obtainData(new LogicalData("tracing master package"), source, target,
                new LogicalData("tracing master package"), new TracingCopyTransferable(), tracingListener);

        // Wait for data
        tracingListener.enable();
        try {
            sem.acquire();
        } catch (InterruptedException ex) {
            ErrorManager.warn("Error waiting for tracing files in master to get saved");
        }
    }

    /**
     * Generate the final extrae tracefile with all transferred packages.
     */
    private static void generateTrace() {
        if (DEBUG) {
            LOGGER.debug("Tracing: Generating trace");
        }
        String script = System.getenv(COMPSsConstants.COMPSS_HOME) + TRACE_SCRIPT_PATH;
        String appName = System.getProperty(COMPSsConstants.APP_NAME);
        ProcessBuilder pb = new ProcessBuilder(script, "gentrace", System.getProperty(COMPSsConstants.APP_LOG_DIR),
                appName, String.valueOf(hostToSlots.size() + 1));
        Process p;
        pb.environment().remove(LD_PRELOAD);
        try {
            p = pb.start();
        } catch (IOException e) {
            ErrorManager.warn("Error generating trace", e);
            return;
        }

        StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), System.out, LOGGER);
        StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), System.err, LOGGER);
        outputGobbler.start();
        errorGobbler.start();

        int exitCode = 0;
        try {
            exitCode = p.waitFor();
            if (exitCode != 0) {
                ErrorManager.warn("Error generating trace, exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            ErrorManager.warn("Error generating trace (interruptedException) : " + e.getMessage());
        }

        String lang = System.getProperty(COMPSsConstants.LANG);
        if (exitCode == 0 && lang.equalsIgnoreCase(COMPSsConstants.Lang.PYTHON.name())) {
            try {
                new TraceMerger(System.getProperty(COMPSsConstants.APP_LOG_DIR), appName).merge();
            } catch (IOException e) {
                ErrorManager.warn("Error while trying to merge files: " + e.toString());
            }
        }
    }

    /**
     * Removing the tracing temporal packages.
     */
    private static void cleanMasterPackage() {
        String filename = DataLocation.Protocol.FILE_URI.getSchema() + "master_compss_trace.tar.gz";

        String filePath;
        try {
            SimpleURI uri = new SimpleURI(filename);
            filePath = new File(uri.getPath()).getCanonicalPath();
        } catch (Exception e) {
            ErrorManager.error(DataLocation.ERROR_INVALID_LOCATION + " " + filename, e);
            return;
        }

        if (DEBUG) {
            LOGGER.debug("Tracing: Removing tracing master package: " + filePath);
        }

        File f;
        try {
            f = new File(filePath);
            boolean deleted = f.delete();
            if (!deleted) {
                ErrorManager.warn("Unable to remove tracing temporary files of master node.");
            } else if (DEBUG) {
                LOGGER.debug("Deleted master tracing package.");
            }
        } catch (Exception e) {
            ErrorManager.warn("Exception while trying to remove tracing temporary " + "files of master node.", e);
        }
    }


    private static class TraceHost {

        private boolean[] slots;
        private int numFreeSlots;
        private int nextSlot;


        private TraceHost(int nslots) {
            this.slots = new boolean[nslots];
            this.numFreeSlots = nslots;
            this.nextSlot = 0;
        }

        private int getNextSlot() {
            if (numFreeSlots-- > 0) {
                while (slots[nextSlot]) {
                    nextSlot = (nextSlot + 1) % slots.length;
                }
                slots[nextSlot] = true;
                return nextSlot;
            } else {
                return -1;
            }
        }

        private void freeSlot(int slot) {
            slots[slot] = false;
            nextSlot = slot;
            numFreeSlots++;
        }
    }

}
