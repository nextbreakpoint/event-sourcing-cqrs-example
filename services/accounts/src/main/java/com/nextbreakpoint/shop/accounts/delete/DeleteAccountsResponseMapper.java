package com.nextbreakpoint.shop.accounts.delete;

import com.nextbreakpoint.shop.common.Mapper;
import com.nextbreakpoint.shop.common.Content;

public class DeleteAccountsResponseMapper implements Mapper<DeleteAccountsResponse, Content> {
    @Override
    public Content transform(DeleteAccountsResponse response) {
        return new Content();
    }
}
