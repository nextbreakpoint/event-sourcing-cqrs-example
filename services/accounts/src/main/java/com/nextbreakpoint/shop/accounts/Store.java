package com.nextbreakpoint.shop.accounts;

import com.nextbreakpoint.shop.accounts.model.DeleteAccountRequest;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountResponse;
import com.nextbreakpoint.shop.accounts.model.InsertAccountResponse;
import com.nextbreakpoint.shop.accounts.model.ListAccountsResponse;
import com.nextbreakpoint.shop.accounts.model.ListAccountsRequest;
import com.nextbreakpoint.shop.accounts.model.InsertAccountRequest;
import com.nextbreakpoint.shop.accounts.model.LoadAccountRequest;
import com.nextbreakpoint.shop.accounts.model.LoadAccountResponse;
import rx.Single;

public interface Store {
    Single<InsertAccountResponse> insertAccount(InsertAccountRequest request);

    Single<LoadAccountResponse> loadAccount(LoadAccountRequest request);

    Single<DeleteAccountResponse> deleteAccount(DeleteAccountRequest request);

    Single<ListAccountsResponse> listAccounts(ListAccountsRequest request);
}
