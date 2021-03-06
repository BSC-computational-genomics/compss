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
package es.bsc.compss.scheduler.types;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Class used to register the characteristics of the execution of one execution or generate a statistic summary of a set
 * of executions.
 * <p>
 * To obtain the characteristics of a single execution, a new Profile is created. At the beginning of the execution, the
 * start method is invoked to obtain the necessary measurements to characterize the execution. Upon the end of the
 * execution, the end() method is called and the Profile class collects the necessary values, and analyzes their initial
 * value to prepare the execution summary.
 * <p>
 * To obtain the statistic report of several executions, their profiles need to be merged using the accumulate method.
 * (Accumulate overrides the internal values of the Profile, it is recommended to accumulate all the values on an empty
 * Profile). To generate a Profile instance with customized values, Profile.Builder enables the creation of a new
 * profile instance and set its initial values.
 * <p>
 * Data currently provided:
 * <ul>
 * <li>Execution time
 * </ul>
 */
public class Profile {

    private static final long DEFAULT_EXECUTION_TIME = 100l;

    private long executions;
    private long startTime;
    private long minTime;
    private long averageTime;
    private long maxTime;


    public Profile() {
        this.executions = 0;
        this.minTime = Long.MAX_VALUE;
        this.averageTime = DEFAULT_EXECUTION_TIME;
        this.maxTime = Long.MIN_VALUE;
    }

    public Profile(Profile p) {
        this.executions = p.executions;
        this.minTime = p.minTime;
        this.averageTime = p.averageTime;
        this.maxTime = p.maxTime;
    }

    public Profile(JSONObject json) {
        if (json != null) {
            try {
                executions = json.getLong("executions");
            } catch (JSONException je) {
                executions = 0;
            }
            try {
                minTime = json.getLong("minTime");
            } catch (JSONException je) {
                minTime = Long.MAX_VALUE;
            }
            try {
                averageTime = json.getLong("avgTime");
            } catch (JSONException je) {
                averageTime = DEFAULT_EXECUTION_TIME;
            }
            try {
                maxTime = json.getLong("maxTime");
            } catch (JSONException je) {
                maxTime = Long.MIN_VALUE;
            }
        } else {
            this.executions = 0;
            this.minTime = Long.MAX_VALUE;
            this.averageTime = DEFAULT_EXECUTION_TIME;
            this.maxTime = Long.MIN_VALUE;
        }
    }

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void end() {
        executions = 1;
        averageTime = System.currentTimeMillis() - startTime;
        minTime = averageTime;
        maxTime = averageTime;
    }

    public long getExecutionCount() {
        return executions;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getMinExecutionTime() {
        return minTime;
    }

    public long getAverageExecutionTime() {
        return averageTime;
    }

    public long getMaxExecutionTime() {
        return maxTime;
    }

    public <P extends Profile> void accumulate(P p) {
        Profile profile = (Profile) p;
        long totalExecutions = executions + profile.executions;
        if (totalExecutions > 0) {
            if (executions == 0) {
                minTime = profile.minTime;
                maxTime = profile.maxTime;
            } else {
                minTime = Math.min(minTime, profile.minTime);
                maxTime = Math.max(maxTime, profile.maxTime);
            }
            averageTime = (profile.averageTime * profile.executions + executions * averageTime) / totalExecutions;
            executions = totalExecutions;
        }
    }

    public JSONObject toJSONObject() {
        JSONObject jo = new JSONObject();
        jo.put("executions", executions);
        jo.put("minTime", minTime);
        jo.put("avgTime", averageTime);
        jo.put("maxTime", maxTime);
        return jo;
    }

    public JSONObject updateJSON(JSONObject jo) {
        JSONObject difference = new JSONObject();

        long oldExecutions = 0;
        long oldAvg = 0;

        if (jo.has("executions")) {
            oldExecutions = jo.getLong("executions");
        }

        if (jo.has("avgTime")) {
            oldAvg = jo.getLong("avgTime");
        }

        long newExecutions = this.executions - oldExecutions;
        jo.put("executions", this.executions);
        jo.put("minTime", this.minTime);
        jo.put("avgTime", this.averageTime);
        jo.put("maxTime", this.maxTime);

        difference.put("executions", executions - oldExecutions);
        difference.put("minTime", this.minTime);
        difference.put("avgTime", this.averageTime);
        long oldTime = oldAvg * oldExecutions;
        long newTime = this.averageTime * this.executions;
        if (newExecutions > 0) {
            difference.put("avgTime", (newTime - oldTime) / newExecutions);
        } else {
            difference.put("avgTime", 0);
        }

        return difference;
    }

    public void accumulateJSON(JSONObject jo) {
        long oldExecutions = 0;
        long oldMin = Long.MAX_VALUE;
        long oldMax = Long.MIN_VALUE;
        long oldAvg = 0;

        if (jo.has("executions")) {
            oldExecutions = jo.getLong("executions");
        }
        if (jo.has("minTime")) {
            oldMin = jo.getLong("minTime");
        }
        if (jo.has("maxTime")) {
            oldMax = jo.getLong("maxTime");
        }
        if (jo.has("avgTime")) {
            oldAvg = jo.getLong("avgTime");
        }

        jo.put("executions", this.executions + oldExecutions);

        if (this.executions > 0) {
            jo.put("minTime", Math.min(minTime, oldMin));
            jo.put("maxTime", Math.max(maxTime, oldMax));
            jo.put("avgTime", (oldAvg * oldExecutions + averageTime * executions) / (this.executions + oldExecutions));
        }

    }

    public Profile copy() {
        return new Profile(this);
    }

    @Override
    public String toString() {
        return "[Profile " + getContent() + "]";
    }

    protected String getContent() {
        return "executions=" + executions + " minTime=" + minTime + " avgTime=" + averageTime + " maxTime=" + maxTime;
    }

    public void clearExecutionCount() {
        this.executions = 0;
    }


    public static class Builder {

        private long minExecutionTime = Long.MAX_VALUE;
        private long maxExecutionTime = Long.MIN_VALUE;
        private long avgExecutionTime = DEFAULT_EXECUTION_TIME;
        private long executions = 0;


        public Builder() {
        }

        public void setExecutions(long executions) {
            this.executions = executions;
        }

        public void setMinExecutionTime(long minExecutionTime) {
            this.minExecutionTime = minExecutionTime;
        }

        public void setAvgExecutionTime(long avgExecutionTime) {
            this.avgExecutionTime = avgExecutionTime;
        }

        public void setMaxExecutionTime(long maxExecutionTime) {
            this.maxExecutionTime = maxExecutionTime;
        }

        public Profile build() {
            Profile p = new Profile();
            update(p);
            return p;
        }

        protected <P extends Profile> void update(P p) {
            Profile profile = (Profile) p;
            profile.executions = executions;
            profile.minTime = minExecutionTime;
            profile.averageTime = avgExecutionTime;
            profile.maxTime = maxExecutionTime;
        }
    }
}
