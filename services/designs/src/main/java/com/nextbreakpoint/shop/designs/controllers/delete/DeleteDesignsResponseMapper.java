package com.nextbreakpoint.shop.designs.controllers.delete;

import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.designs.model.DeleteDesignsResponse;

public class DeleteDesignsResponseMapper implements Mapper<DeleteDesignsResponse, Content> {
    @Override
    public Content transform(DeleteDesignsResponse response) {
        return new Content();
    }
}
