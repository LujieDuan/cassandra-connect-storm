/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package util;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.thrift.TException;

public final class StormRunner {

  private static final int MILLIS_IN_SEC = 1000;

  private StormRunner() {
  }

  public static void runTopologyLocally(StormTopology topology, String topologyName, Config conf, int runtimeInSeconds)
      throws InterruptedException {
    LocalCluster cluster = null;
    try {
      cluster = new LocalCluster();
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      cluster.submitTopology(topologyName, conf, topology);
    } catch (TException e) {
      e.printStackTrace();
    }
    Thread.sleep((long) runtimeInSeconds * MILLIS_IN_SEC);
    try {
      cluster.killTopology(topologyName);
    } catch (TException e) {
      e.printStackTrace();
    }
    cluster.shutdown();
  }

  public static void runTopologyRemotely(StormTopology topology, String topologyName, Config conf)
      throws AlreadyAliveException, InvalidTopologyException, AuthorizationException {
    StormSubmitter.submitTopology(topologyName, conf, topology);
  }
}
