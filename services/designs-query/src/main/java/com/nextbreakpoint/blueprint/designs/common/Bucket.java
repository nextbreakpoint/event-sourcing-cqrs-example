
package com.nextbreakpoint.blueprint.designs.common;

import com.nextbreakpoint.blueprint.designs.operations.get.GetTileRequest;

public class Bucket {
    private Bucket() {}

    public static String createBucketKey(GetTileRequest request, String checksum) {
        return "tiles/%s/%d/%04d%04d.png".formatted(checksum, request.getLevel(), request.getRow(), request.getCol());
    }
}
