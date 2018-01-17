package com.nextbreakpoint.shop.accounts.delete;

import com.nextbreakpoint.shop.common.ResponseMapper;
import com.nextbreakpoint.shop.common.Result;

public class DeleteAccountsResponseMapper implements ResponseMapper<DeleteAccountsResponse> {
    @Override
    public Result apply(DeleteAccountsResponse response) {
        return new Result();
    }
}
