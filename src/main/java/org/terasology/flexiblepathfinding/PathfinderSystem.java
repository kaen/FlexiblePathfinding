/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.flexiblepathfinding;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.console.commandSystem.annotations.Command;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.utilities.concurrency.TaskMaster;
import org.terasology.world.WorldProvider;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This systems helps finding a paths through the game world.
 * <p/>
 * Since paths finding takes some time, it completely runs in a background thread. So, a requested paths is not
 * available in the moment it is requested. Instead you need to listen for a PathReadyEvent.
 * <p/>
 * Here we also listen for world changes (OnChunkReady and OnBlockChanged). Currently, both events reset the
 * pathfinder (clear path cache) and rebuild the modified chunk.
 * </p>
 * Chunk updates are processed before any pathfinding request. However, this system does not inform about
 * paths getting invalid.
 *
 * @author synopia
 */
@RegisterSystem(RegisterMode.AUTHORITY)
@Share(value = PathfinderSystem.class)
public class PathfinderSystem extends BaseComponentSystem {

    private static final Logger logger = LoggerFactory.getLogger(PathfinderSystem.class);

    private int nextId;
    private int pathsSearched;

    public static ExecutorService getExecutor() {
        return executor;
    }


    private Set<EntityRef> entitiesWithPendingTasks = Sets.newHashSet();

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    // These threads simply create TimeLimiters and wait in a loop, the real work is done on the executor above
    private TaskMaster<PathfinderTask> taskMaster = TaskMaster.createPriorityTaskMaster("Pathfinder", 4, 1024);

    @In
    private WorldProvider world;

    @Override
    public void initialise() {
        logger.info("PathfinderSystem started");
    }

    @Override
    public void shutdown() {
        taskMaster.shutdown(new ShutdownTask(null, null, null), false);
    }

    public int requestPath(EntityRef requestor, Vector3i target, List<Vector3i> start) {
        return requestPath(requestor, target, start, null);
    }

    public int requestPath(EntityRef requestor, Vector3i target, List<Vector3i> start, PathfinderCallback callback) {
        JPSConfig config = new JPSConfig(start.get(0), target);
        config.requestor = requestor;
        return requestPath(config, callback);
    }

    public int requestPath(JPSConfig config, PathfinderCallback callback) {
        if(config.requestor != null && config.requestor.exists()) {
            if(entitiesWithPendingTasks.contains(config.requestor)) {
                return -1;
            }
            entitiesWithPendingTasks.add(config.requestor);
        }

        if(config.executor == null) {
            config.executor = executor;
        }

        PathfinderTask task = new PathfinderTask(world, config, callback);
        taskMaster.offer(task);
        return nextId++;    }

    public int requestPath(Vector3i start, Vector3i target, PathfinderCallback callback) {
        return requestPath(null, target, Lists.newArrayList(start), callback);
    }

    public int getPathsSearched() {
        return pathsSearched;
    }

    public void completePathFor(EntityRef requestor) {
        if(requestor == null) {
            return;
        }
        entitiesWithPendingTasks.remove(requestor);
    }

    @Command
    public void recordPathStats() {
        JPSImpl.setStatsEnabled(true);
    }

    @Command
    public void printPathStats() {
        logger.info(JPSImpl.getStats());
    }
}