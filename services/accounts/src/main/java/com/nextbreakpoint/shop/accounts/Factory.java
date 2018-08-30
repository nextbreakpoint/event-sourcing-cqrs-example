package com.nextbreakpoint.shop.accounts;

import com.nextbreakpoint.shop.accounts.common.ContentConsumer;
import com.nextbreakpoint.shop.accounts.controllers.delete.DeleteAccountController;
import com.nextbreakpoint.shop.accounts.controllers.delete.DeleteAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.controllers.delete.DeleteAccountResponseMapper;
import com.nextbreakpoint.shop.accounts.controllers.insert.InsertAccountController;
import com.nextbreakpoint.shop.accounts.controllers.insert.InsertAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.controllers.insert.InsertAccountResponseMapper;
import com.nextbreakpoint.shop.accounts.controllers.list.ListAccountsController;
import com.nextbreakpoint.shop.accounts.controllers.list.ListAccountsRequestMapper;
import com.nextbreakpoint.shop.accounts.controllers.list.ListAccountsResponseMapper;
import com.nextbreakpoint.shop.accounts.controllers.load.LoadAccountController;
import com.nextbreakpoint.shop.accounts.controllers.load.LoadAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.controllers.load.LoadAccountResponseMapper;
import com.nextbreakpoint.shop.accounts.controllers.load.LoadSelfAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountRequest;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountResponse;
import com.nextbreakpoint.shop.accounts.model.InsertAccountRequest;
import com.nextbreakpoint.shop.accounts.model.InsertAccountResponse;
import com.nextbreakpoint.shop.accounts.model.ListAccountsRequest;
import com.nextbreakpoint.shop.accounts.model.ListAccountsResponse;
import com.nextbreakpoint.shop.accounts.model.LoadAccountRequest;
import com.nextbreakpoint.shop.accounts.model.LoadAccountResponse;
import com.nextbreakpoint.shop.common.model.Content;
import com.nextbreakpoint.shop.common.vertx.handlers.DefaultHandler;
import com.nextbreakpoint.shop.common.vertx.handlers.FailedRequestConsumer;
import io.vertx.rxjava.ext.web.RoutingContext;

public class Factory {
    private Factory() {}

    public static DefaultHandler<RoutingContext, DeleteAccountRequest, DeleteAccountResponse, Content> createDeleteAccountHandler(Store store) {
        return DefaultHandler.<RoutingContext, DeleteAccountRequest, DeleteAccountResponse, Content>builder()
                .withInputMapper(new DeleteAccountRequestMapper())
                .withOutputMapper(new DeleteAccountResponseMapper())
                .withController(new DeleteAccountController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, InsertAccountRequest, InsertAccountResponse, Content> createInsertAccountHandler(Store store) {
        return DefaultHandler.<RoutingContext, InsertAccountRequest, InsertAccountResponse, Content>builder()
                .withInputMapper(new InsertAccountRequestMapper())
                .withOutputMapper(new InsertAccountResponseMapper())
                .withController(new InsertAccountController(store))
                .onSuccess(new ContentConsumer(201))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, ListAccountsRequest, ListAccountsResponse, Content> createListAccountsHandler(Store store) {
        return DefaultHandler.<RoutingContext, ListAccountsRequest, ListAccountsResponse, Content>builder()
                .withInputMapper(new ListAccountsRequestMapper())
                .withOutputMapper(new ListAccountsResponseMapper())
                .withController(new ListAccountsController(store))
                .onSuccess(new ContentConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, LoadAccountRequest, LoadAccountResponse, Content> createLoadAccountHandler(Store store) {
        return DefaultHandler.<RoutingContext, LoadAccountRequest, LoadAccountResponse, Content>builder()
                .withInputMapper(new LoadAccountRequestMapper())
                .withOutputMapper(new LoadAccountResponseMapper())
                .withController(new LoadAccountController(store))
                .onSuccess(new ContentConsumer(200, 404))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static DefaultHandler<RoutingContext, LoadAccountRequest, LoadAccountResponse, Content> createLoadSelfAccountHandler(Store store) {
        return DefaultHandler.<RoutingContext, LoadAccountRequest, LoadAccountResponse, Content>builder()
                .withInputMapper(new LoadSelfAccountRequestMapper())
                .withOutputMapper(new LoadAccountResponseMapper())
                .withController(new LoadAccountController(store))
                .onSuccess(new ContentConsumer(200, 404))
                .onFailure(new FailedRequestConsumer())
                .build();
    }
}
