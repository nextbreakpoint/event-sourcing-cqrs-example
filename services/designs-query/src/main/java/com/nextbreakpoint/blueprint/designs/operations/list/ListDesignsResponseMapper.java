package com.nextbreakpoint.blueprint.designs.operations.list;

import com.nextbreakpoint.blueprint.common.core.Json;
import com.nextbreakpoint.blueprint.common.core.Mapper;
import com.nextbreakpoint.blueprint.designs.model.DesignDocument;
import com.nextbreakpoint.blueprint.designs.model.DesignDocuments;
import com.nextbreakpoint.blueprint.designs.persistence.dto.ListDesignsResponse;

import java.util.stream.Collectors;

public class ListDesignsResponseMapper implements Mapper<ListDesignsResponse, String> {
    @Override
    public String transform(ListDesignsResponse response) {
        final DesignDocuments documents = DesignDocuments.builder()
                .withDesigns(response.getDesigns().stream().map(DesignDocument::from).collect(Collectors.toList()))
                .withTotal(response.getTotal())
                .build();

        return Json.encodeValue(documents);
    }
}
