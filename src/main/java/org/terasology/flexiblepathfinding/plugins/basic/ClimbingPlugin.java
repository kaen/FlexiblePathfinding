// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.flexiblepathfinding.plugins.basic;

import org.terasology.flexiblepathfinding.plugins.StandardPlugin;
import org.terasology.math.Region3i;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.WorldProvider;

public class ClimbingPlugin extends StandardPlugin {
    public ClimbingPlugin(WorldProvider world, float width, float height) {
        super(world, width, height);
    }

    @Override
    public boolean isReachable(Vector3i to, Vector3i from) {
        // can only move one unit in a single direction
        Vector3i delta = new Vector3i(to).sub(from);
        delta.absolute();
        if (delta.lengthSquared() > 1) {
            return false;
        }

        // check that all blocks passed through by this movement are penetrable or climbable
        boolean hasClimbableDestination = false;
        for (Vector3i occupiedBlock : getOccupiedRegionRelative()) {

            // the start/stop for this block in the occupied region
            Vector3i occupiedBlockTo = new Vector3i(to).add(occupiedBlock);
            Vector3i occupiedBlockFrom = new Vector3i(from).add(occupiedBlock);

            // only need to look for a climbable destination block if we don't have one already
            if (!hasClimbableDestination && world.getBlock(occupiedBlockTo).isClimbable()) {
                hasClimbableDestination = true;
            }

            Region3i movementBounds = Region3i.createBounded(occupiedBlockTo, occupiedBlockFrom);
            for (Vector3i block : movementBounds) {
                if (!world.getBlock(block).isPenetrable() && !world.getBlock(block).isClimbable()) {
                    return false;
                }
            }
        }

        return hasClimbableDestination;
    }
}
