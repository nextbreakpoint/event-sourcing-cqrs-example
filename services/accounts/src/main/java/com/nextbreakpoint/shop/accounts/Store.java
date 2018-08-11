package com.nextbreakpoint.shop.accounts;

import com.nextbreakpoint.shop.accounts.delete.DeleteAccountRequest;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountResponse;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountsRequest;
import com.nextbreakpoint.shop.accounts.delete.DeleteAccountsResponse;
import com.nextbreakpoint.shop.accounts.insert.InsertAccountResponse;
import com.nextbreakpoint.shop.accounts.list.ListAccountsResponse;
import com.nextbreakpoint.shop.accounts.list.ListAccountsRequest;
import com.nextbreakpoint.shop.accounts.insert.InsertAccountRequest;
import com.nextbreakpoint.shop.accounts.load.LoadAccountRequest;
import com.nextbreakpoint.shop.accounts.load.LoadAccountResponse;
import rx.Single;

public interface Store {
    Single<InsertAccountResponse> insertAccount(InsertAccountRequest request);

    Single<LoadAccountResponse> loadAccount(LoadAccountRequest request);

    Single<DeleteAccountResponse> deleteAccount(DeleteAccountRequest request);

    Single<DeleteAccountsResponse> deleteAccounts(DeleteAccountsRequest request);

    Single<ListAccountsResponse> listAccounts(ListAccountsRequest request);
}
