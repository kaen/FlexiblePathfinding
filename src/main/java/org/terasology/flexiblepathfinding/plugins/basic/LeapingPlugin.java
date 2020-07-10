/*
 * Copyright 2018 MovingBlocks
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
package org.terasology.flexiblepathfinding.plugins.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.flexiblepathfinding.plugins.StandardPlugin;
import org.terasology.math.Region3i;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.WorldProvider;

public class LeapingPlugin extends WalkingPlugin {
    private static final Logger logger = LoggerFactory.getLogger(LeapingPlugin.class);

    public LeapingPlugin(WorldProvider world, float width, float height) {
        super(world, width, height);
    }

    @Override
    public boolean isReachable(Vector3i to, Vector3i from) {
        int dx = to.x - from.x;
        int dy = to.y - from.y;
        int dz = to.z - from.z;
        if (Math.abs(dx) > 1 || Math.abs(dz) > 1) {
            return false;
        }

        // you can jump up or fall down, but you can't road-runner off the edge
        if (dy == 0) {
            return false;
        }

        // only go up if we're currently on the ground
        if (dy >= 0 && !isWalkable(from)) {
            return false;
        }

        // check that all blocks passed through by this movement are penetrable
        for (Vector3i occupiedBlock : getOccupiedRegionRelative()) {

            // the start/stop for this block in the occupied region
            Vector3i occupiedBlockTo = new Vector3i(to).add(occupiedBlock);
            Vector3i occupiedBlockFrom = new Vector3i(from).add(occupiedBlock);

            Region3i movementBounds = Region3i.createBounded(occupiedBlockTo, occupiedBlockFrom);
            for (Vector3i block : movementBounds) {
                if (!world.getBlock(block).isPenetrable()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public double getCost(Vector3i to, Vector3i from) {
        // prefer walking over jumping, value is roughly sqrt(2)
        // setting this any higher will make path finding MUCH slower when a jump is needed
        return 1.0f * super.getCost(to, from);
    }
}
