package com.nextbreakpoint.shop.accounts.controllers.delete;

import com.nextbreakpoint.shop.accounts.model.DeleteAccountsResponse;
import com.nextbreakpoint.shop.common.model.Mapper;
import com.nextbreakpoint.shop.common.model.Content;

public class DeleteAccountsResponseMapper implements Mapper<DeleteAccountsResponse, Content> {
    @Override
    public Content transform(DeleteAccountsResponse response) {
        return new Content();
    }
}
