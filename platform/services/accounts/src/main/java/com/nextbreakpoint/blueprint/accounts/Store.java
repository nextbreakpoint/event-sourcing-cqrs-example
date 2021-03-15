package com.nextbreakpoint.blueprint.accounts;

import com.nextbreakpoint.blueprint.accounts.model.*;
import rx.Single;

public interface Store {
    Single<InsertAccountResponse> insertAccount(InsertAccountRequest request);

    Single<LoadAccountResponse> loadAccount(LoadAccountRequest request);

    Single<DeleteAccountResponse> deleteAccount(DeleteAccountRequest request);

    Single<ListAccountsResponse> listAccounts(ListAccountsRequest request);
}
