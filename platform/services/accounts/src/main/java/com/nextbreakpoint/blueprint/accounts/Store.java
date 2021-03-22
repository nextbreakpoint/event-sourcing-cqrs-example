package com.nextbreakpoint.blueprint.accounts;

import com.nextbreakpoint.blueprint.accounts.operations.delete.DeleteAccountRequest;
import com.nextbreakpoint.blueprint.accounts.operations.delete.DeleteAccountResponse;
import com.nextbreakpoint.blueprint.accounts.operations.insert.InsertAccountRequest;
import com.nextbreakpoint.blueprint.accounts.operations.insert.InsertAccountResponse;
import com.nextbreakpoint.blueprint.accounts.operations.list.ListAccountsRequest;
import com.nextbreakpoint.blueprint.accounts.operations.list.ListAccountsResponse;
import com.nextbreakpoint.blueprint.accounts.operations.load.LoadAccountRequest;
import com.nextbreakpoint.blueprint.accounts.operations.load.LoadAccountResponse;
import rx.Single;

public interface Store {
    Single<InsertAccountResponse> insertAccount(InsertAccountRequest request);

    Single<DeleteAccountResponse> deleteAccount(DeleteAccountRequest request);

    Single<LoadAccountResponse> loadAccount(LoadAccountRequest request);

    Single<ListAccountsResponse> listAccounts(ListAccountsRequest request);
}
