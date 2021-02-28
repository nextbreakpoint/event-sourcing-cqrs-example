package com.nextbreakpoint.blueprint.accounts;

import com.nextbreakpoint.blueprint.accounts.model.DeleteAccountRequest;
import com.nextbreakpoint.blueprint.accounts.model.DeleteAccountResponse;
import com.nextbreakpoint.blueprint.accounts.model.InsertAccountRequest;
import com.nextbreakpoint.blueprint.accounts.model.InsertAccountResponse;
import com.nextbreakpoint.blueprint.accounts.model.ListAccountsRequest;
import com.nextbreakpoint.blueprint.accounts.model.ListAccountsResponse;
import com.nextbreakpoint.blueprint.accounts.model.LoadAccountRequest;
import com.nextbreakpoint.blueprint.accounts.model.LoadAccountResponse;
import rx.Single;

public interface Store {
    Single<InsertAccountResponse> insertAccount(InsertAccountRequest request);

    Single<LoadAccountResponse> loadAccount(LoadAccountRequest request);

    Single<DeleteAccountResponse> deleteAccount(DeleteAccountRequest request);

    Single<ListAccountsResponse> listAccounts(ListAccountsRequest request);
}
