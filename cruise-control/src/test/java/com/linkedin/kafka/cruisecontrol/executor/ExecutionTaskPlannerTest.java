/*
 * Copyright 2017 LinkedIn Corp. Licensed under the BSD 2-Clause License (the "License"). See License in the project root for license information.
 */

package com.linkedin.kafka.cruisecontrol.executor;

import com.linkedin.kafka.cruisecontrol.analyzer.BalancingAction;
import com.linkedin.kafka.cruisecontrol.common.ActionType;
import java.util.Collections;
import org.apache.kafka.common.TopicPartition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit test class for execution task planner
 */
public class ExecutionTaskPlannerTest {
  private static final String TOPIC1 = "topic1";
  private static final String TOPIC2 = "topic2";

  private final BalancingAction leaderMovement1 = new BalancingAction(new TopicPartition(TOPIC1, 0), 0, 1,
                                                                      ActionType.LEADERSHIP_MOVEMENT);
  private final BalancingAction leaderMovement2 = new BalancingAction(new TopicPartition(TOPIC1, 1), 0, 1,
                                                                      ActionType.LEADERSHIP_MOVEMENT);
  private final BalancingAction leaderMovement3 = new BalancingAction(new TopicPartition(TOPIC1, 2), 2, 1,
                                                                      ActionType.LEADERSHIP_MOVEMENT);
  private final BalancingAction leaderMovement4 = new BalancingAction(new TopicPartition(TOPIC1, 3), 3, 2,
                                                                      ActionType.LEADERSHIP_MOVEMENT);

  private final BalancingAction partitionMovement1 = new BalancingAction(new TopicPartition(TOPIC2, 0), 0, 1,
                                                                         ActionType.REPLICA_MOVEMENT, 1);
  private final BalancingAction partitionMovement2 = new BalancingAction(new TopicPartition(TOPIC2, 1), 0, 1,
                                                                         ActionType.REPLICA_MOVEMENT, 2);
  private final BalancingAction partitionMovement3 = new BalancingAction(new TopicPartition(TOPIC2, 2), 2, 1,
                                                                         ActionType.REPLICA_MOVEMENT, 3);
  private final BalancingAction partitionMovement4 = new BalancingAction(new TopicPartition(TOPIC2, 3), 3, 2,
                                                                         ActionType.REPLICA_MOVEMENT, 4);

  private final AtomicLong _executionId = new AtomicLong(0L);

  @Test
  public void testGetLeaderMovementTasks() {
    List<BalancingAction> proposals = new ArrayList<>();
    proposals.add(leaderMovement1);
    proposals.add(leaderMovement2);
    proposals.add(leaderMovement3);
    proposals.add(leaderMovement4);
    ExecutionTaskPlanner planner = new ExecutionTaskPlanner();
    planner.addBalancingProposals(proposals);
    List<ExecutionTask> leaderMovementTasks = planner.getLeaderMovementTasks(2);
    assertEquals("2 of the leader movements should return in one batch", 2, leaderMovementTasks.size());
    assertEquals(leaderMovementTasks.get(0).executionId, 0L);
    assertEquals(leaderMovementTasks.get(0).proposal, leaderMovement1);
    assertEquals(leaderMovementTasks.get(1).executionId, 1L);
    assertEquals(leaderMovementTasks.get(1).proposal, leaderMovement2);
    leaderMovementTasks = planner.getLeaderMovementTasks(2);
    assertEquals("2 of the leader movements should return in one batch", 2, leaderMovementTasks.size());
    assertEquals(leaderMovementTasks.get(0).executionId, 2L);
    assertEquals(leaderMovementTasks.get(0).proposal, leaderMovement3);
    assertEquals(leaderMovementTasks.get(1).executionId, 3L);
    assertEquals(leaderMovementTasks.get(1).proposal, leaderMovement4);
  }

  @Test
  public void testGetPartitionMovementTasks() {
    assertEquals(partitionMovement1.balancingAction(), ActionType.REPLICA_MOVEMENT);
    List<BalancingAction> proposals = new ArrayList<>();
    proposals.add(partitionMovement1);
    proposals.add(partitionMovement2);
    proposals.add(partitionMovement3);
    proposals.add(partitionMovement4);
    ExecutionTaskPlanner planner = new ExecutionTaskPlanner();
    planner.addBalancingProposals(proposals);
    Map<Integer, Integer> readyBrokers = new HashMap<>();
    readyBrokers.put(0, 2);
    readyBrokers.put(1, 2);
    readyBrokers.put(2, 1);
    readyBrokers.put(3, 1);
    List<ExecutionTask> partitionMovementTasks = planner.getReplicaMovementTasks(readyBrokers, Collections.emptySet());
    assertEquals("First task should be partitionMovement1", partitionMovement1, partitionMovementTasks.get(0).proposal);
    assertEquals("First task should be partitionMovement4", partitionMovement4, partitionMovementTasks.get(1).proposal);
    assertEquals("First task should be partitionMovement2", partitionMovement2, partitionMovementTasks.get(2).proposal);
  }
  
  @Test
  public void testClear() {
    List<BalancingAction> proposals = new ArrayList<>();
    proposals.add(leaderMovement1);
    proposals.add(partitionMovement1);
    ExecutionTaskPlanner planner = new ExecutionTaskPlanner();
    planner.addBalancingProposals(proposals);
    assertEquals(1, planner.remainingDataToMoveInMB());
    assertEquals(1, planner.remainingLeaderMovements().size());
    assertEquals(1, planner.remainingReplicaMovements().size());
    planner.clear();
    assertEquals(0, planner.remainingDataToMoveInMB());
    assertEquals(0, planner.remainingLeaderMovements().size());
    assertEquals(0, planner.remainingReplicaMovements().size());
  }

  private List<ExecutionTask> generateExecutionTasks(BalancingAction... proposals) {
    List<ExecutionTask> tasks = new ArrayList<>();
    for (BalancingAction proposal : proposals) {
      tasks.add(new ExecutionTask(_executionId.getAndIncrement(), proposal));
    }
    return tasks;
  }
}
