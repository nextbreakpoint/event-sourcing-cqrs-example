package com.nextbreakpoint.shop.designs.handlers.delete;

import com.nextbreakpoint.shop.common.Content;
import com.nextbreakpoint.shop.common.Mapper;

public class DeleteDesignsResponseMapper implements Mapper<DeleteDesignsResponse, Content> {
    @Override
    public Content transform(DeleteDesignsResponse response) {
        return new Content();
    }
}
